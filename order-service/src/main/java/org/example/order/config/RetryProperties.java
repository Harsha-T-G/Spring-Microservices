package org.example.order.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("inventory.retry")
public record RetryProperties(@Min(1) @Max(2) int maxAttempts, @Min(1) @Max(1000) long waitMs) {
}
