package org.example.inventory.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationRequest(@NotNull UUID orderId, @NotNull @Positive Integer quantity) {
}
