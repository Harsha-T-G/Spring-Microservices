package org.example.order.client;

import java.util.UUID;

import org.example.order.dto.ReservationResponse;

public interface InventoryClient {
    ReservationResponse reserve(UUID orderId, String sku, int quantity, String key, String correlationId);
}
