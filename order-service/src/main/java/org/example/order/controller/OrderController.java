package org.example.order.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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

import org.example.order.dto.CreateOrderRequest;
import org.example.order.dto.OrderResponse;
import org.example.order.model.Order;
import org.example.order.service.OrderService;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orders;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @RequestHeader("Idempotency-Key") @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}") String key,
            @Valid @RequestBody CreateOrderRequest request) {
        Order order = orders.create(request, key, MDC.get("correlationId"));
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.getId())).body(response(order));
    }

    @GetMapping("/{id}")
    public OrderResponse find(@PathVariable UUID id) {
        return response(orders.find(id));
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orders.list().stream().map(this::response).toList();
    }

    private OrderResponse response(Order order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getSku(), order.getQuantity(),
                order.getStatus(), order.getReservationId(), order.getRejectionReason(), order.getCreatedAt());
    }
}
