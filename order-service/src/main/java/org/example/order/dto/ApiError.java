package org.example.order.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiError(Instant timestamp, int status, String error, String code,
                       String message, String path, String correlationId,
                       @JsonInclude(JsonInclude.Include.NON_NULL) UUID orderId) {
}
