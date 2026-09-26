package org.example.order.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.example.order.gateway.InventoryClient;
import org.example.order.gateway.InventoryFailure;
import org.example.order.config.OrderProperties;
import org.example.order.dto.CreateOrderRequest;
import org.example.order.exception.OrderException;
import org.example.order.model.Order;
import org.example.order.model.OrderStatus;
import org.example.order.store.OrderAttempt;
import org.example.order.store.OrderStore;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final InventoryClient inventory;
    private final OrderStore store;

    private final OrderProperties properties;

    public Order create(CreateOrderRequest request, String key, String correlationId) {
        OrderAttempt attempt = store.begin(key, request);
        boolean acquired = false;
        try {
            acquired = attempt.acquire(properties.inProgressWaitMs());
            if (!acquired) {
                throw new OrderException("ORDER_IN_PROGRESS", "The order is still processing. Retry with the same key.");
            }
            Order completed = store.find(attempt.getId()).orElse(null);
            if (completed != null) {
                return outcome(completed);
            }
            return reserve(attempt, request, key, correlationId);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new OrderException("ORDER_IN_PROGRESS", "The request was interrupted. Retry with the same key.");
        } finally {
            if (acquired) {
                attempt.release();
            }
        }
    }

    private Order reserve(OrderAttempt attempt, CreateOrderRequest request, String key, String correlationId) {
        log.info("orderId={} status=RESERVING", attempt.getId());
        try {
            UUID reservationId = inventory.reserve(attempt.getId(), request.sku(), request.quantity(), key, correlationId);
            Order order = store.save(new Order(attempt.getId(), request.customerId(), request.sku(), request.quantity(),
                    OrderStatus.CONFIRMED, reservationId, null, attempt.getCreatedAt()));
            log.info("orderId={} reservationId={} status={}", order.getId(), order.getReservationId(), order.getStatus());
            return order;
        } catch (InventoryFailure failure) {
            if (failure.getKind() == InventoryFailure.Kind.BUSINESS) {
                Order rejected = store.save(new Order(attempt.getId(), request.customerId(), request.sku(), request.quantity(),
                        OrderStatus.REJECTED, null, failure.getCode(), attempt.getCreatedAt()));
                log.info("orderId={} status=REJECTED reason={}", attempt.getId(), failure.getCode());
                return outcome(rejected);
            }
            throw new OrderException(failure.getCode(), failure.getKind() == InventoryFailure.Kind.UNAVAILABLE
                    ? "Inventory could not confirm the reservation. Retry with the same idempotency key."
                    : "Inventory could not process the reservation contract");
        }
    }

    private Order outcome(Order order) {
        if (order.getStatus() == OrderStatus.REJECTED) {
            throw new OrderException(order.getRejectionReason(), "Stock reservation was rejected", order.getId());
        }
        return order;
    }

    public Order find(UUID id) {
        return store.find(id).orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", "The order does not exist"));
    }

    public List<Order> list() {
        return store.list();
    }
}
