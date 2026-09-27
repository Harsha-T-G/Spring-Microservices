package org.example.inventory.model;

import lombok.Value;

@Value
public class ProductStock {
    String sku;
    int availableQuantity;

    public ProductStock(String sku, int availableQuantity) {
        if (sku == null || sku.isBlank() || availableQuantity < 0) {
            throw new IllegalArgumentException("Stock requires a SKU and nonnegative quantity");
        }
        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }
}
