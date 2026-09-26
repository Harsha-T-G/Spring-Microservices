package org.example.order.store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import org.example.order.dto.CreateOrderRequest;
import org.example.order.exception.OrderException;
import org.example.order.model.Order;
import org.example.order.model.OrderStatus;

@Component
@RequiredArgsConstructor
public class OrderStore {
    private static final RowMapper<Order> ORDER_MAPPER = OrderStore::order;

    private final JdbcTemplate jdbc;
    private final ReentrantLock[] locks = java.util.stream.IntStream.range(0, 1024)
            .mapToObj(ignored -> new ReentrantLock()).toArray(ReentrantLock[]::new);

    public OrderAttempt begin(String key, CreateOrderRequest request) {
        jdbc.update("INSERT INTO orders (id, idempotency_key, customer_id, sku, quantity, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (idempotency_key) DO NOTHING",
                UUID.randomUUID(), key, request.customerId(), request.sku(), request.quantity(), Timestamp.from(Instant.now()));
        return jdbc.queryForObject("SELECT id, customer_id, sku, quantity, created_at FROM orders "
                + "WHERE idempotency_key = ?", (result, row) -> {
            CreateOrderRequest previous = new CreateOrderRequest(result.getString("customer_id"),
                    result.getString("sku"), result.getInt("quantity"));
            if (!previous.equals(request)) {
                throw new OrderException("IDEMPOTENCY_CONFLICT", "The key belongs to a different order request");
            }
            return new OrderAttempt(result.getObject("id", UUID.class), result.getTimestamp("created_at").toInstant(),
                    previous, locks[Math.floorMod(key.hashCode(), locks.length)]);
        }, key);
    }

    public Order save(Order order) {
        jdbc.update("UPDATE orders SET status = ?, reservation_id = ?, rejection_reason = ? WHERE id = ?",
                order.getStatus().name(), order.getReservationId(), order.getRejectionReason(), order.getId());
        return order;
    }

    public Optional<Order> find(UUID id) {
        return jdbc.query("SELECT id, customer_id, sku, quantity, status, reservation_id, rejection_reason, "
                + "created_at FROM orders WHERE id = ? AND status IS NOT NULL", ORDER_MAPPER, id).stream().findFirst();
    }

    public List<Order> list() {
        return jdbc.query("SELECT id, customer_id, sku, quantity, status, reservation_id, rejection_reason, "
                + "created_at FROM orders WHERE status IS NOT NULL ORDER BY created_at, id", ORDER_MAPPER);
    }

    private static Order order(ResultSet result, int row) throws SQLException {
        return new Order(result.getObject("id", UUID.class), result.getString("customer_id"),
                result.getString("sku"), result.getInt("quantity"), OrderStatus.valueOf(result.getString("status")),
                result.getObject("reservation_id", UUID.class), result.getString("rejection_reason"),
                result.getTimestamp("created_at").toInstant());
    }
}
