package org.example.order.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("orders")
public record OrderProperties(@Min(1) @Max(4000) long inProgressWaitMs) {
}
