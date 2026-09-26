package org.example.order.gateway.http;

import java.util.UUID;

public record ReservationResponse(UUID reservationId, UUID orderId, String sku, int quantity, String status) {
}
