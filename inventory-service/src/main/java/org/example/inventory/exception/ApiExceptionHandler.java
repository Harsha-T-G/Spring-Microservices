package org.example.inventory.exception;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.example.inventory.dto.ApiError;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<Object> inventory(InventoryException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.getCode()) {
            case "SKU_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INSUFFICIENT_STOCK", "IDEMPOTENCY_CONFLICT" -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(error(status, exception.getCode(), exception.getMessage(), request));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String code = status == HttpStatus.BAD_REQUEST ? "VALIDATION_ERROR" : "HTTP_ERROR";
        return ResponseEntity.status(status).headers(headers).body(error(status, code,
                status.getReasonPhrase(), ((ServletWebRequest) request).getRequest()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected request failure", exception);
        return ResponseEntity.internalServerError().body(error(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "The request could not be completed", request));
    }

    private ApiError error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), code, message,
                request.getRequestURI(), MDC.get("correlationId"));
    }
}
