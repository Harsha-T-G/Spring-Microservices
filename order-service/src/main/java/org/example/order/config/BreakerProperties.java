package org.example.order.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("inventory.breaker")
public record BreakerProperties(@Min(1) long openWaitMs) {
}
