package org.example.order.dto;

import java.util.Locale;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(example = "{\"customerId\":\"CUST-1001\",\"sku\":\"JAVA-BOOK\",\"quantity\":2}")
public record CreateOrderRequest(@NotBlank @Size(max = 128) String customerId,
                                 @NotBlank @Size(max = 128) String sku,
                                 @NotNull @Positive Integer quantity) {
    public CreateOrderRequest {
        customerId = customerId == null ? null : customerId.trim();
        sku = sku == null ? null : sku.trim().toUpperCase(Locale.ROOT);
    }
}
