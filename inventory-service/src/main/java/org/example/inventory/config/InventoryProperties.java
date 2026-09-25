package org.example.inventory.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("inventory")
public record InventoryProperties(Map<String, Integer> initialStock) {
    public InventoryProperties {
        initialStock = initialStock == null ? Map.of() : Map.copyOf(initialStock);
    }
}
