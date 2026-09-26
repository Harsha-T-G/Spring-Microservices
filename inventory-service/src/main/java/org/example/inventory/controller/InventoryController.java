package org.example.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import org.example.inventory.dto.ApiError;
import org.example.inventory.dto.ReservationRequest;
import org.example.inventory.dto.ReservationResponse;
import org.example.inventory.dto.StockResponse;
import org.example.inventory.service.InventoryService;

@RestController
@Tag(name = "Inventory")
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventory;

    @Operation(summary = "Get available stock for a SKU")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = StockResponse.class))),
        @ApiResponse(responseCode = "404", description = "SKU not found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{sku}")
    public StockResponse stock(@Parameter(example = "JAVA-BOOK") @PathVariable String sku) {
        var stock = inventory.stock(sku);
        return new StockResponse(stock.getSku(), stock.getAvailableQuantity());
    }

    @Operation(summary = "Reserve stock or replay a successful reservation")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Successful response",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "SKU not found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Insufficient stock or conflicting idempotency key",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{sku}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Parameter(example = "JAVA-BOOK") @PathVariable String sku,
            @Parameter(description = "Reuse for an identical retry; use a new key for a new operation.", example = "swagger-order-1")
            @RequestHeader("Idempotency-Key") @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String key,
            @Valid @RequestBody ReservationRequest request) {
        var reservation = inventory.reserve(sku, request.orderId(), request.quantity(), key);
        return new ReservationResponse(reservation.getReservationId(), reservation.getOrderId(),
                reservation.getSku(), reservation.getQuantity(), "RESERVED");
    }
}
