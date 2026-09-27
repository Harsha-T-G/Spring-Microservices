package org.example.order.gateway.http;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import org.example.order.gateway.InventoryClient;
import org.example.order.gateway.InventoryFailure;

@Component
@RequiredArgsConstructor
public class RestClientInventoryClient implements InventoryClient {
    private final RestClient inventoryRestClient;
    private final ObjectMapper json;
    private final Retry inventoryRetry;
    private final CircuitBreaker inventoryCircuitBreaker;

    @Override
    public UUID reserve(UUID orderId, String sku, int quantity, String key, String correlationId) {
        try {
            return inventoryCircuitBreaker.executeSupplier(() ->
                    inventoryRetry.executeSupplier(() -> attempt(orderId, sku, quantity, key, correlationId)));
        } catch (CallNotPermittedException failure) {
            throw new InventoryFailure(InventoryFailure.Kind.UNAVAILABLE, "INVENTORY_UNAVAILABLE");
        }
    }

    private UUID attempt(UUID orderId, String sku, int quantity, String key, String correlationId) {
        try {
            var response = inventoryRestClient.post().uri("/api/v1/inventory/{sku}/reservations", sku)
                    .header("Idempotency-Key", key).header("X-Correlation-Id", correlationId)
                    .contentType(MediaType.APPLICATION_JSON).body(new ReservationRequest(orderId, quantity))
                    .retrieve().onStatus(HttpStatusCode::isError, this::translate)
                    .toEntity(ReservationResponse.class);
            var reservation = response.getBody();
            if (response.getStatusCode().value() != 201 || reservation == null || reservation.reservationId() == null
                    || !orderId.equals(reservation.orderId()) || !sku.equals(reservation.sku())
                    || quantity != reservation.quantity() || !"RESERVED".equals(reservation.status())) {
                throw new InventoryFailure(InventoryFailure.Kind.CONTRACT, "INVENTORY_CONTRACT_ERROR");
            }
            return reservation.reservationId();
        } catch (ResourceAccessException exception) {
            throw new InventoryFailure(InventoryFailure.Kind.UNAVAILABLE, "INVENTORY_UNAVAILABLE");
        } catch (RestClientException exception) {
            throw new InventoryFailure(InventoryFailure.Kind.CONTRACT, "INVENTORY_CONTRACT_ERROR");
        }
    }

    private void translate(HttpRequest request, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        if (status >= 500) {
            throw new InventoryFailure(InventoryFailure.Kind.UNAVAILABLE, "INVENTORY_UNAVAILABLE");
        }
        String code;
        try {
            var error = json.readTree(response.getBody());
            code = error == null ? "" : error.path("code").asText();
        } catch (JsonProcessingException exception) {
            throw new InventoryFailure(InventoryFailure.Kind.CONTRACT, "INVENTORY_CONTRACT_ERROR");
        }
        if ((status == 404 && "SKU_NOT_FOUND".equals(code))
                || (status == 409 && "INSUFFICIENT_STOCK".equals(code))) {
            throw new InventoryFailure(InventoryFailure.Kind.BUSINESS, code);
        }
        if (status == 409 && "IDEMPOTENCY_CONFLICT".equals(code)) {
            throw new InventoryFailure(InventoryFailure.Kind.CONFLICT, code);
        }
        throw new InventoryFailure(InventoryFailure.Kind.CONTRACT, "INVENTORY_CONTRACT_ERROR");
    }
}
