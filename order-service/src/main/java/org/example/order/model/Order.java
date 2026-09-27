package org.example.order.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Value;

@Value
public class Order {
    UUID id;
    String customerId;
    String sku;
    int quantity;
    OrderStatus status;
    UUID reservationId;
    String rejectionReason;
    Instant createdAt;

    public Order(UUID id, String customerId, String sku, int quantity, OrderStatus status,
                 UUID reservationId, String rejectionReason, Instant createdAt) {
        if (id == null || customerId == null || customerId.isBlank() || sku == null || sku.isBlank()
                || quantity <= 0 || status == null || createdAt == null) {
            throw new IllegalArgumentException("An order requires identity, customer, SKU, positive quantity, status and time");
        }
        if ((status == OrderStatus.CONFIRMED && (reservationId == null || rejectionReason != null))
                || (status == OrderStatus.REJECTED && (reservationId != null || rejectionReason == null
                || rejectionReason.isBlank()))) {
            throw new IllegalArgumentException("Order outcome must match its reservation or rejection reason");
        }
        this.id = id;
        this.customerId = customerId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = status;
        this.reservationId = reservationId;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }
}
