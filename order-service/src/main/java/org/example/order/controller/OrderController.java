package org.example.order.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.example.order.dto.ApiError;
import org.example.order.dto.CreateOrderRequest;
import org.example.order.dto.OrderResponse;
import org.example.order.model.Order;
import org.example.order.service.OrderService;

@RestController
@Tag(name = "Order")
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orders;

    @Operation(summary = "Create an order or replay its original outcome")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Successful response",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Idempotency key conflict",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "422", description = "Order rejected by Inventory",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "502", description = "Unexpected Inventory contract",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "Inventory unavailable or order still processing",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Parameter(description = "Reuse for an identical retry; use a new key for a new operation.", example = "swagger-order-1")
            @RequestHeader("Idempotency-Key") @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String key,
            @Valid @RequestBody CreateOrderRequest request) {
        Order order = orders.create(request, key, MDC.get("correlationId"));
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.getId())).body(response(order));
    }

    @Operation(summary = "Get a confirmed or rejected order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful response",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid order ID",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Order not found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public OrderResponse find(@PathVariable UUID id) {
        return response(orders.find(id));
    }

    @Operation(summary = "List orders in creation order")
    @GetMapping
    public List<OrderResponse> list() {
        return orders.list().stream().map(this::response).toList();
    }

    private OrderResponse response(Order order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getSku(), order.getQuantity(),
                order.getStatus(), order.getReservationId(), order.getRejectionReason(), order.getCreatedAt());
    }
}
