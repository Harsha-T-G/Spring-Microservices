package org.example.order.store;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import org.example.order.dto.CreateOrderRequest;
import org.example.order.exception.OrderException;
import org.example.order.model.Order;

@Component
public class OrderStore {
    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();

    private final Map<String, OrderAttempt> attempts = new ConcurrentHashMap<>();

    public OrderAttempt begin(String key, CreateOrderRequest request) {
        OrderAttempt attempt = attempts.computeIfAbsent(key, ignored -> new OrderAttempt(request));
        if (!attempt.matches(request)) {
            throw new OrderException("IDEMPOTENCY_CONFLICT", "The key belongs to a different order request");
        }
        return attempt;
    }

    public Order save(Order order) {
        orders.put(order.getId(), order);
        return order;
    }

    public Optional<Order> find(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    public List<Order> list() {
        return orders.values().stream().sorted(Comparator.comparing(Order::getCreatedAt).thenComparing(Order::getId))
                .toList();
    }
}
