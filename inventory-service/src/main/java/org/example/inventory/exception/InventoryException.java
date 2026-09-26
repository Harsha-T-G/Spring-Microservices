package org.example.inventory.exception;

import lombok.Getter;

@Getter
public class InventoryException extends RuntimeException {
    private final String code;

    public InventoryException(String code, String message) {
        super(message);
        this.code = code;
    }
}
