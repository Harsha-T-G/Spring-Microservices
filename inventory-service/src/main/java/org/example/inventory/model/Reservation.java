package org.example.inventory.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Value;

@Value
public class Reservation {
    UUID reservationId;
    UUID orderId;
    String sku;
    int quantity;
    String idempotencyKey;
    Instant createdAt;

    public Reservation(UUID reservationId, UUID orderId, String sku, int quantity,
                       String idempotencyKey, Instant createdAt) {
        if (reservationId == null || orderId == null || sku == null || sku.isBlank() || quantity <= 0
                || idempotencyKey == null || idempotencyKey.isBlank() || createdAt == null) {
            throw new IllegalArgumentException("A reservation requires identity, SKU, positive quantity and creation time");
        }
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }
}
