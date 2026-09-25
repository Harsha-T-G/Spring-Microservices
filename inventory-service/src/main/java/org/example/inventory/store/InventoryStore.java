package org.example.inventory.store;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.example.inventory.config.InventoryProperties;
import org.example.inventory.exception.InventoryException;
import org.example.inventory.model.ProductStock;
import org.example.inventory.model.Reservation;

@Component
public class InventoryStore {
    private final Map<String, ProductStock> stocks = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();

    public InventoryStore(InventoryProperties properties) {
        properties.initialStock().forEach((sku, quantity) -> {
            String normalized = sku.trim().toUpperCase(Locale.ROOT);
            if (quantity < 0) {
                throw new IllegalArgumentException("Initial stock cannot be negative");
            }
            stocks.put(normalized, new ProductStock(normalized, quantity));
        });
    }

    public synchronized Optional<ProductStock> find(String sku) {
        return Optional.ofNullable(stocks.get(sku));
    }

    public synchronized Reservation reserve(String sku, UUID orderId, int quantity, String key) {
        Reservation previous = reservations.get(key);
        if (previous != null) {
            if (!previous.getOrderId().equals(orderId) || !previous.getSku().equals(sku)
                    || previous.getQuantity() != quantity) {
                throw new InventoryException("IDEMPOTENCY_CONFLICT", "The key belongs to a different request");
            }
            return previous;
        }
        ProductStock stock = find(sku)
                .orElseThrow(() -> new InventoryException("SKU_NOT_FOUND", "The SKU does not exist"));
        if (stock.getAvailableQuantity() < quantity) {
            throw new InventoryException("INSUFFICIENT_STOCK", "There is not enough stock");
        }
        Reservation reservation = new Reservation(UUID.randomUUID(), orderId, sku, quantity, key, Instant.now());
        stocks.put(sku, new ProductStock(sku, stock.getAvailableQuantity() - quantity));
        reservations.put(key, reservation);
        return reservation;
    }
}
