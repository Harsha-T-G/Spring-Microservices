package org.example.order.dto;

import java.util.UUID;

public record ReservationRequest(UUID orderId, int quantity) {
}
