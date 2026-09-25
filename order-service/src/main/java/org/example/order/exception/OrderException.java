package org.example.order.exception;

import java.util.UUID;

import lombok.Getter;

@Getter
public class OrderException extends RuntimeException {
    private final String code;
    private final UUID orderId;

    public OrderException(String code, String message) {
        this(code, message, null);
    }

    public OrderException(String code, String message, UUID orderId) {
        super(message);
        this.code = code;
        this.orderId = orderId;
    }
}
