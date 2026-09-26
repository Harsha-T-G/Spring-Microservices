package org.example.order.controller;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "orders.in-progress-wait-ms=50")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderConcurrencyTest {
    private static final WireMockServer INVENTORY = new WireMockServer(options().dynamicPort());
    private static final String REQUEST = "{\"customerId\":\"CUST-1\",\"sku\":\"JAVA-BOOK\",\"quantity\":2}";

    static {
        INVENTORY.start();
    }

    private final MockMvc mvc;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;

    @Autowired
    OrderConcurrencyTest(MockMvc mvc, ObjectMapper json, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.json = json;
        this.jdbc = jdbc;
    }

    @DynamicPropertySource
    static void inventoryProperties(DynamicPropertyRegistry registry) {
        registry.add("inventory.base-url", INVENTORY::baseUrl);
    }

    @AfterAll
    static void stopInventory() {
        INVENTORY.stop();
    }

    @BeforeEach
    void resetOrders() {
        jdbc.update("DELETE FROM orders");
    }

    @Test
    void givenInProgressOrder_whenDuplicateArrives_thenBoundedWaitWithoutParallelReservation() throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(201)
                .withFixedDelay(700).withHeader("Content-Type", "application/json")
                .withBody("{\"reservationId\":\"223e4567-e89b-42d3-a456-426614174000\","
                        + "\"orderId\":\"{{jsonPath request.body '$.orderId'}}\","
                        + "\"sku\":\"JAVA-BOOK\",\"quantity\":2,\"status\":\"RESERVED\"}")
                .withTransformers("response-template")));
        try (var executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(() -> mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "concurrent")
                    .contentType("application/json").content(REQUEST)).andReturn().getResponse());
            await().atMost(Duration.ofSeconds(2)).pollInterval(Duration.ofMillis(10))
                    .untilAsserted(() -> INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.anyUrl())));
            long start = System.nanoTime();
            mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "concurrent")
                            .contentType("application/json").content(REQUEST))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ORDER_IN_PROGRESS"));
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(500));
            var original = first.get(3, TimeUnit.SECONDS);
            assertThat(original.getStatus()).isEqualTo(201);
            mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "concurrent")
                            .contentType("application/json").content(REQUEST))
                    .andExpect(status().isCreated()).andExpect(content().json(original.getContentAsString()));
            INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.anyUrl())
                    .withRequestBody(WireMock.matchingJsonPath("$.orderId",
                            WireMock.equalTo(json.readTree(original.getContentAsString()).get("id").asText()))));
        }
    }
}
