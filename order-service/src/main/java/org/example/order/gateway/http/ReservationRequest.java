package org.example.order.gateway.http;

import java.util.UUID;

public record ReservationRequest(UUID orderId, int quantity) {
}
