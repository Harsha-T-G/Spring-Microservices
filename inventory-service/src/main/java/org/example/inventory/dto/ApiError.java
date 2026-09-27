package org.example.inventory.dto;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String code,
                       String message, String path, String correlationId) {
}
