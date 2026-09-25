package org.example.inventory.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.example.inventory.dto.ReservationRequest;
import org.example.inventory.dto.ReservationResponse;
import org.example.inventory.dto.StockResponse;
import org.example.inventory.service.InventoryService;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventory;

    @GetMapping("/{sku}")
    public StockResponse stock(@PathVariable String sku) {
        var stock = inventory.stock(sku);
        return new StockResponse(stock.getSku(), stock.getAvailableQuantity());
    }

    @PostMapping("/{sku}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@PathVariable String sku,
            @RequestHeader("Idempotency-Key") @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String key,
            @Valid @RequestBody ReservationRequest request) {
        var reservation = inventory.reserve(sku, request.orderId(), request.quantity(), key);
        return new ReservationResponse(reservation.getReservationId(), reservation.getOrderId(),
                reservation.getSku(), reservation.getQuantity(), "RESERVED");
    }
}
