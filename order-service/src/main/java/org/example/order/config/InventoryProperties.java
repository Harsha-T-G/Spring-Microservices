package org.example.order.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("inventory")
@Validated
public record InventoryProperties(@NotBlank String baseUrl,
                                  @Min(1) @Max(500) int connectTimeoutMs,
                                  @Min(1) @Max(1000) int responseTimeoutMs) {
}
