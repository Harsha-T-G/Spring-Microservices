package org.example.order.gateway;

import java.util.UUID;

public interface InventoryClient {
    UUID reserve(UUID orderId, String sku, int quantity, String key, String correlationId);
}
