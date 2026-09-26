package org.example.inventory.controller;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryApiTest {
    private final MockMvc mvc;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;

    @Autowired
    InventoryApiTest(MockMvc mvc, ObjectMapper json, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.json = json;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void resetStock() {
        jdbc.update("DELETE FROM reservations");
        jdbc.update("DELETE FROM product_stock");
        jdbc.update("INSERT INTO product_stock (sku, available_quantity) VALUES ('JAVA-BOOK', 20), ('KEYBOARD-01', 10)");
    }

    @Test
    void givenExistingSku_whenReadingCaseInsensitively_thenStockIsReturned() throws Exception {
        mvc.perform(get("/api/v1/inventory/java-book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("JAVA-BOOK"))
                .andExpect(jsonPath("$.availableQuantity").value(20));
    }

    @Test
    void givenUnknownSku_whenReadingStock_thenSafeCorrelatedNotFoundIsReturned() throws Exception {
        mvc.perform(get("/api/v1/inventory/UNKNOWN").header("X-Correlation-Id", "stock-missing"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", "stock-missing"))
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/inventory/UNKNOWN"))
                .andExpect(jsonPath("$.correlationId").value("stock-missing"));
    }

    @Test
    void givenAvailableStock_whenReserving_thenReservationIsCreatedAndStockDecreases() throws Exception {
        mvc.perform(post("/api/v1/inventory/JAVA-BOOK/reservations")
                        .header("Idempotency-Key", "reservation-1")
                        .contentType("application/json")
                        .content("{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("123e4567-e89b-42d3-a456-426614174000"))
                .andExpect(jsonPath("$.reservationId").isNotEmpty())
                .andExpect(jsonPath("$.sku").value("JAVA-BOOK"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("RESERVED"));
        mvc.perform(get("/api/v1/inventory/JAVA-BOOK"))
                .andExpect(jsonPath("$.availableQuantity").value(18));
    }

    @Test
    void givenInsufficientStock_whenReserving_thenConflictLeavesStockUnchanged() throws Exception {
        mvc.perform(post("/api/v1/inventory/JAVA-BOOK/reservations")
                        .header("Idempotency-Key", "too-many")
                        .contentType("application/json")
                        .content("{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":21}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        mvc.perform(get("/api/v1/inventory/JAVA-BOOK"))
                .andExpect(jsonPath("$.availableQuantity").value(20));
    }

    @Test
    void givenCompletedReservation_whenReplayingAfterStockExhausted_thenOriginalResultIsReturned() throws Exception {
        String body = "{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":20}";
        String first = mvc.perform(post("/api/v1/inventory/JAVA-BOOK/reservations")
                        .header("Idempotency-Key", "replay").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/inventory/java-book/reservations")
                        .header("Idempotency-Key", "replay").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(content().json(first));
        mvc.perform(get("/api/v1/inventory/JAVA-BOOK"))
                .andExpect(jsonPath("$.availableQuantity").value(0));
    }

    @ParameterizedTest
    @CsvSource({
            "JAVA-BOOK,123e4567-e89b-42d3-a456-426614174000,3",
            "JAVA-BOOK,223e4567-e89b-42d3-a456-426614174000,2",
            "UNKNOWN,123e4567-e89b-42d3-a456-426614174000,2"
    })
    void givenUsedKey_whenChangingFingerprint_thenConflictPreservesStock(String sku, String orderId, int quantity)
            throws Exception {
        mvc.perform(post("/api/v1/inventory/JAVA-BOOK/reservations")
                        .header("Idempotency-Key", "bound-key").contentType("application/json")
                        .content("{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":2}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/inventory/{sku}/reservations", sku)
                        .header("Idempotency-Key", "bound-key").contentType("application/json")
                        .content("{\"orderId\":\"%s\",\"quantity\":%d}".formatted(orderId, quantity)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        mvc.perform(get("/api/v1/inventory/JAVA-BOOK"))
                .andExpect(jsonPath("$.availableQuantity").value(18));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "null", "2.5", "2147483648", "\"2\""})
    void givenInvalidQuantity_whenReserving_thenSafeValidationErrorLeavesStockUnchanged(String quantity)
            throws Exception {
        mvc.perform(post("/api/v1/inventory/JAVA-BOOK/reservations")
                        .header("Idempotency-Key", "invalid").header("X-Correlation-Id", "invalid-input")
                        .contentType("application/json")
                        .content("{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":" + quantity + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.correlationId").value("invalid-input"));
        mvc.perform(get("/api/v1/inventory/JAVA-BOOK"))
                .andExpect(jsonPath("$.availableQuantity").value(20));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "bad key", "bad/key"})
    void givenInvalidKey_whenReserving_thenBadRequestHasNoStockEffect(String key) throws Exception {
        var request = post("/api/v1/inventory/JAVA-BOOK/reservations").contentType("application/json")
                .content("{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":2}");
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        mvc.perform(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/inventory/JAVA-BOOK"))
                .andExpect(jsonPath("$.availableQuantity").value(20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"quantity\":2}", "{\"orderId\":\"wrong\",\"quantity\":2}", "{"})
    void givenMalformedReservation_whenPosting_thenBadRequestIsSafe(String body) throws Exception {
        mvc.perform(post("/api/v1/inventory/JAVA-BOOK/reservations")
                        .header("Idempotency-Key", "malformed").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void givenMissingSku_whenReserving_thenNotFoundIsReturned() throws Exception {
        mvc.perform(post("/api/v1/inventory/MISSING/reservations")
                        .header("Idempotency-Key", "missing").contentType("application/json")
                        .content("{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":2}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenConcurrentReservations_whenCompeting_thenAtomicityPreventsDuplicateOrNegativeStock(boolean sameKey)
            throws Exception {
        int count = 24;
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        var futures = new ArrayList<Future<MvcResult>>();
        try (var executor = Executors.newFixedThreadPool(count)) {
            for (int index = 0; index < count; index++) {
                String key = sameKey ? "same-key" : "key-" + index;
                String order = sameKey ? "123e4567-e89b-42d3-a456-426614174000" : UUID.randomUUID().toString();
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent start timed out");
                    }
                    return mvc.perform(post("/api/v1/inventory/JAVA-BOOK/reservations")
                                    .header("Idempotency-Key", key).contentType("application/json")
                                    .content("{\"orderId\":\"%s\",\"quantity\":2}".formatted(order)))
                            .andReturn();
                }));
            }
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            var results = new ArrayList<MvcResult>();
            for (var future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }
            assertThat(results.stream().filter(result -> result.getResponse().getStatus() == 201).count())
                    .isEqualTo(sameKey ? count : 10);
            assertThat(results.stream().filter(result -> result.getResponse().getStatus() == 409).count())
                    .isEqualTo(sameKey ? 0 : count - 10);
            if (sameKey) {
                String originalId = json.readTree(results.getFirst().getResponse().getContentAsString())
                        .get("reservationId").asText();
                for (var result : results) {
                    assertThat(json.readTree(result.getResponse().getContentAsString()).get("reservationId").asText())
                            .isEqualTo(originalId);
                }
            }
        }
        mvc.perform(get("/api/v1/inventory/JAVA-BOOK"))
                .andExpect(jsonPath("$.availableQuantity").value(sameKey ? 18 : 0));
    }
    @Test
    void givenConcurrentCrossSkuReuse_whenReserving_thenExactlyOneFingerprintOwnsTheKey() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var futures = new ArrayList<Future<MvcResult>>();
        try (var executor = Executors.newFixedThreadPool(2)) {
            for (String sku : java.util.List.of("JAVA-BOOK", "KEYBOARD-01")) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent start timed out");
                    }
                    return mvc.perform(post("/api/v1/inventory/" + sku + "/reservations")
                                    .header("Idempotency-Key", "cross-sku").contentType("application/json")
                                    .content("{\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"quantity\":2}"))
                            .andReturn();
                }));
            }
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            var first = futures.get(0).get(5, TimeUnit.SECONDS).getResponse();
            var second = futures.get(1).get(5, TimeUnit.SECONDS).getResponse();
            assertThat(java.util.List.of(first.getStatus(), second.getStatus())).containsExactlyInAnyOrder(201, 409);
            mvc.perform(get("/api/v1/inventory/JAVA-BOOK"))
                    .andExpect(jsonPath("$.availableQuantity").value(first.getStatus() == 201 ? 18 : 20));
            mvc.perform(get("/api/v1/inventory/KEYBOARD-01"))
                    .andExpect(jsonPath("$.availableQuantity").value(second.getStatus() == 201 ? 8 : 10));
        }
    }

}
