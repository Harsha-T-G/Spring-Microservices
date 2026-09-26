package org.example.order.gateway;

import lombok.Getter;

@Getter
public class InventoryFailure extends RuntimeException {
    public enum Kind {
        BUSINESS, CONFLICT, UNAVAILABLE, CONTRACT
    }

    private final Kind kind;
    private final String code;

    public InventoryFailure(Kind kind, String code) {
        super("Inventory reservation could not be confirmed");
        this.kind = kind;
        this.code = code;
    }
}
