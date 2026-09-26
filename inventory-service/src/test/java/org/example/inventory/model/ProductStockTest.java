package org.example.inventory.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductStockTest {
    @Test
    void givenNegativeQuantity_whenCreatingStock_thenInvalidStateIsRejected() {
        assertThatThrownBy(() -> new ProductStock("JAVA-BOOK", -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
