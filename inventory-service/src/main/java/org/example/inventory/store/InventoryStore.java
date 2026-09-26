package org.example.inventory.store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.example.inventory.exception.InventoryException;
import org.example.inventory.model.ProductStock;
import org.example.inventory.model.Reservation;

@Component
@RequiredArgsConstructor
public class InventoryStore {
    private static final RowMapper<ProductStock> STOCK_MAPPER =
            (result, row) -> new ProductStock(result.getString("sku"), result.getInt("available_quantity"));
    private static final RowMapper<Reservation> RESERVATION_MAPPER = InventoryStore::reservation;

    private final JdbcTemplate jdbc;

    public Optional<ProductStock> find(String sku) {
        List<ProductStock> stocks = jdbc.query(
                "SELECT sku, available_quantity FROM product_stock WHERE sku = ?", STOCK_MAPPER, sku);
        return stocks.stream().findFirst();
    }

    @Transactional
    public Reservation reserve(String sku, UUID orderId, int quantity, String key) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", result -> {
            result.next();
            return null;
        }, key);
        List<Reservation> previous = jdbc.query("SELECT reservation_id, order_id, sku, quantity, "
                + "idempotency_key, created_at FROM reservations WHERE idempotency_key = ?", RESERVATION_MAPPER, key);
        if (!previous.isEmpty()) {
            Reservation reservation = previous.getFirst();
            if (!reservation.getOrderId().equals(orderId) || !reservation.getSku().equals(sku)
                    || reservation.getQuantity() != quantity) {
                throw new InventoryException("IDEMPOTENCY_CONFLICT", "The key belongs to a different request");
            }
            return reservation;
        }
        int changed = jdbc.update("UPDATE product_stock SET available_quantity = available_quantity - ? "
                + "WHERE sku = ? AND available_quantity >= ?", quantity, sku, quantity);
        if (changed == 0) {
            if (find(sku).isEmpty()) {
                throw new InventoryException("SKU_NOT_FOUND", "The SKU does not exist");
            }
            throw new InventoryException("INSUFFICIENT_STOCK", "There is not enough stock");
        }
        Reservation reservation = new Reservation(UUID.randomUUID(), orderId, sku, quantity, key, Instant.now());
        jdbc.update("INSERT INTO reservations (reservation_id, order_id, sku, quantity, idempotency_key, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)", reservation.getReservationId(), reservation.getOrderId(),
                reservation.getSku(), reservation.getQuantity(), reservation.getIdempotencyKey(),
                java.sql.Timestamp.from(reservation.getCreatedAt()));
        return reservation;
    }

    private static Reservation reservation(ResultSet result, int row) throws SQLException {
        return new Reservation(result.getObject("reservation_id", UUID.class),
                result.getObject("order_id", UUID.class), result.getString("sku"), result.getInt("quantity"),
                result.getString("idempotency_key"), result.getTimestamp("created_at").toInstant());
    }
}
