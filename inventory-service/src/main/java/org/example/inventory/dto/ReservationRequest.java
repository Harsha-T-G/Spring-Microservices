package org.example.inventory.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(example = "{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":2}")
public record ReservationRequest(@NotNull UUID orderId, @NotNull @Positive Integer quantity) {
}
