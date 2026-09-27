package org.example.order.dto;

import java.time.Instant;
import java.util.UUID;

import org.example.order.model.OrderStatus;

public record OrderResponse(UUID id, String customerId, String sku, int quantity, OrderStatus status,
                            UUID reservationId, String rejectionReason, Instant createdAt) {
}
