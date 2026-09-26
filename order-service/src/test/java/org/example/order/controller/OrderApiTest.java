package org.example.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = "inventory.breaker.open-wait-ms=500")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class OrderApiTest {
    private static final WireMockServer INVENTORY = new WireMockServer(options().dynamicPort());
    private static final String RESERVATION_ID = "223e4567-e89b-42d3-a456-426614174000";
    private static final String REQUEST = "{\"customerId\":\"CUST-1001\",\"sku\":\"java-book\",\"quantity\":2}";

    static {
        INVENTORY.start();
    }

    private final MockMvc mvc;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;

    @Autowired
    OrderApiTest(MockMvc mvc, ObjectMapper json, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.json = json;
        this.jdbc = jdbc;
    }

    @DynamicPropertySource
    static void inventoryProperties(DynamicPropertyRegistry registry) {
        registry.add("inventory.base-url", INVENTORY::baseUrl);
    }

    @BeforeEach
    void resetInventory() {
        INVENTORY.resetAll();
        jdbc.update("DELETE FROM orders");
    }

    @AfterAll
    static void stopInventory() {
        INVENTORY.stop();
    }

    @Test
    void givenAvailableInventory_whenCreatingOrder_thenConfirmedOrderAndContractAreReturned() throws Exception {
        reserveSuccessfully();
        var response = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "customer-order-1")
                        .header("X-Correlation-Id", "order-success").contentType("application/json").content(REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.reservationId").value(RESERVATION_ID))
                .andExpect(jsonPath("$.sku").value("JAVA-BOOK"))
                .andReturn().getResponse();
        String orderId = json.readTree(response.getContentAsString()).get("id").asText();
        assertThat(response.getHeader("Location")).isEqualTo("/api/v1/orders/" + orderId);
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("order-success");
        INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/inventory/JAVA-BOOK/reservations"))
                .withHeader("Idempotency-Key", WireMock.equalTo("customer-order-1"))
                .withHeader("X-Correlation-Id", WireMock.equalTo("order-success"))
                .withRequestBody(WireMock.equalToJson("{\"orderId\":\"%s\",\"quantity\":2}".formatted(orderId))));
    }

    @Test
    void givenCreatedOrder_whenReadingById_thenStoredOrderIsReturned() throws Exception {
        reserveSuccessfully();
        var created = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "read-order")
                        .contentType("application/json").content(REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse();
        mvc.perform(get(created.getHeader("Location")))
                .andExpect(status().isOk()).andExpect(content().json(created.getContentAsString()));
    }

    @Test
    void givenOrders_whenListing_thenEmptyArrayOrCreationOrderIsReturned() throws Exception {
        mvc.perform(get("/api/v1/orders")).andExpect(status().isOk()).andExpect(content().json("[]"));
        reserveSuccessfully();
        var ids = new java.util.ArrayList<String>();
        for (int index = 0; index < 3; index++) {
            var response = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "list-" + index)
                            .contentType("application/json").content(REQUEST))
                    .andExpect(status().isCreated()).andReturn().getResponse();
            ids.add(json.readTree(response.getContentAsString()).get("id").asText());
        }
        mvc.perform(get("/api/v1/orders")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(ids.get(0)))
                .andExpect(jsonPath("$[1].id").value(ids.get(1)))
                .andExpect(jsonPath("$[2].id").value(ids.get(2)));
    }

    @ParameterizedTest
    @CsvSource({"404,SKU_NOT_FOUND", "409,INSUFFICIENT_STOCK"})
    void givenInventoryBusinessRejection_whenCreatingOrder_thenRejectedOrderIsStored(int remoteStatus, String code)
            throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse()
                .withStatus(remoteStatus).withHeader("Content-Type", "application/json")
                .withBody("{\"code\":\"" + code + "\"}")));
        var result = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "rejected")
                        .header("X-Correlation-Id", "rejection").contentType("application/json").content(REQUEST))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.correlationId").value("rejection"))
                .andReturn().getResponse();
        String orderId = json.readTree(result.getContentAsString()).get("orderId").asText();
        mvc.perform(get("/api/v1/orders/" + orderId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value(code))
                .andExpect(jsonPath("$.reservationId").isEmpty());
        INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void givenCompletedOrder_whenReplayingWithNewCorrelation_thenOriginalOrderWithoutRemoteCallIsReturned()
            throws Exception {
        reserveSuccessfully();
        String first = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "order-replay")
                        .header("X-Correlation-Id", "first-attempt").contentType("application/json").content(REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var replay = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "order-replay")
                        .header("X-Correlation-Id", "second-attempt").contentType("application/json").content(REQUEST))
                .andExpect(status().isCreated()).andExpect(content().json(first)).andReturn().getResponse();
        assertThat(replay.getHeader("X-Correlation-Id")).isEqualTo("second-attempt");
        INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.anyUrl()));
        mvc.perform(get("/api/v1/orders")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void givenInventoryServerFailure_whenCreatingOrder_thenExactlyTwoStableAttemptsReturnSafeUnavailable()
            throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse()
                .withStatus(500).withBody("private-inventory-detail")));
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "retry-order")
                        .header("X-Correlation-Id", "retry-correlation").contentType("application/json").content(REQUEST))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("INVENTORY_UNAVAILABLE"))
                .andExpect(jsonPath("$.correlationId").value("retry-correlation"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-inventory-detail"))));
        var attempts = INVENTORY.findAll(WireMock.postRequestedFor(WireMock.anyUrl()));
        assertThat(attempts).hasSize(2);
        assertThat(attempts).allSatisfy(attempt -> {
            assertThat(attempt.getHeader("Idempotency-Key")).isEqualTo("retry-order");
            assertThat(attempt.getHeader("X-Correlation-Id")).isEqualTo("retry-correlation");
            assertThat(attempt.getBodyAsString()).isEqualTo(attempts.getFirst().getBodyAsString());
        });
        mvc.perform(get("/api/v1/orders")).andExpect(content().json("[]"));
    }

    @Test
    void givenRepeatedDependencyFailures_whenCircuitOpens_thenNoRequestsUntilTimedRecovery() throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(500)));
        for (int index = 0; index < 4; index++) {
            mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "failure-" + index)
                            .contentType("application/json").content(REQUEST))
                    .andExpect(status().isServiceUnavailable());
        }
        INVENTORY.verify(8, WireMock.postRequestedFor(WireMock.anyUrl()));
        reserveSuccessfully();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "recovery")
                        .contentType("application/json").content(REQUEST))
                .andExpect(status().isServiceUnavailable());
        INVENTORY.verify(8, WireMock.postRequestedFor(WireMock.anyUrl()));
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(3))
                .pollInterval(java.time.Duration.ofMillis(50)).untilAsserted(() ->
                        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "recovery")
                                        .contentType("application/json").content(REQUEST))
                                .andExpect(status().isCreated()));
        INVENTORY.verify(9, WireMock.postRequestedFor(WireMock.anyUrl()));
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "after-recovery")
                        .contentType("application/json").content(REQUEST)).andExpect(status().isCreated());
        INVENTORY.verify(10, WireMock.postRequestedFor(WireMock.anyUrl()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"customerId\":\"OTHER\",\"sku\":\"JAVA-BOOK\",\"quantity\":2}",
            "{\"customerId\":\"CUST-1001\",\"sku\":\"KEYBOARD-01\",\"quantity\":2}",
            "{\"customerId\":\"CUST-1001\",\"sku\":\"JAVA-BOOK\",\"quantity\":3}"
    })
    void givenCompletedOrder_whenKeyIsReusedForDifferentInput_thenConflictWithoutRemoteCall(String changed)
            throws Exception {
        reserveSuccessfully();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "conflict")
                .contentType("application/json").content(REQUEST)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "conflict")
                        .contentType("application/json").content(changed))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void givenRejectedOrder_whenReplayingAfterRecovery_thenOriginalRejectionWithCurrentCorrelation() throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(409)
                .withHeader("Content-Type", "application/json").withBody("{\"code\":\"INSUFFICIENT_STOCK\"}")));
        var first = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "rejected-replay")
                        .contentType("application/json").content(REQUEST))
                .andExpect(status().isUnprocessableEntity()).andReturn().getResponse();
        String id = json.readTree(first.getContentAsString()).get("orderId").asText();
        reserveSuccessfully();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "rejected-replay")
                        .header("X-Correlation-Id", "new-replay").contentType("application/json").content(REQUEST))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.orderId").value(id))
                .andExpect(jsonPath("$.correlationId").value("new-replay"));
        INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void givenUncertainOutcome_whenClientRetriesLater_thenOriginalOrderIdentityIsRetained() throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(500)));
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "uncertain")
                .contentType("application/json").content(REQUEST)).andExpect(status().isServiceUnavailable());
        var attempts = INVENTORY.findAll(WireMock.postRequestedFor(WireMock.anyUrl()));
        String originalId = json.readTree(attempts.getFirst().getBodyAsString()).get("orderId").asText();
        mvc.perform(get("/api/v1/orders/" + originalId)).andExpect(status().isNotFound());
        reserveSuccessfully();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "uncertain")
                        .header("X-Correlation-Id", "later-request").contentType("application/json").content(REQUEST))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(originalId));
        INVENTORY.verify(3, WireMock.postRequestedFor(WireMock.anyUrl())
                .withHeader("Idempotency-Key", WireMock.equalTo("uncertain"))
                .withRequestBody(WireMock.matchingJsonPath("$.orderId", WireMock.equalTo(originalId))));
    }

    @ParameterizedTest
    @CsvSource({"400,VALIDATION_ERROR,502", "404,UNKNOWN_CODE,502", "409,UNKNOWN_CODE,502",
            "409,IDEMPOTENCY_CONFLICT,409", "401,UNAUTHORIZED,502"})
    void givenUnexpectedOrConflictingRemoteResponse_whenCreatingOrder_thenSafeNonRetryableError(
            int remoteStatus, String code, int expectedStatus) throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(remoteStatus)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"code\":\"" + code + "\",\"message\":\"private-detail\"}")));
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "remote-contract")
                        .contentType("application/json").content(REQUEST))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedStatus == 409 ? "IDEMPOTENCY_CONFLICT" : "INVENTORY_CONTRACT_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-detail"))));
        INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.anyUrl()));
        mvc.perform(get("/api/v1/orders")).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "{}", "{\"reservationId\":\"invalid-uuid\"}",
            "{\"reservationId\":\"223e4567-e89b-42d3-a456-426614174000\",\"orderId\":\"123e4567-e89b-42d3-a456-426614174000\",\"sku\":\"JAVA-BOOK\",\"quantity\":2,\"status\":\"RESERVED\"}"})
    void givenInvalidRemoteSuccess_whenCreatingOrder_thenContractErrorWithoutRetry(String response) throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json").withBody(response)));
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "malformed-success")
                        .contentType("application/json").content(REQUEST))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("INVENTORY_CONTRACT_ERROR"));
        INVENTORY.verify(1, WireMock.postRequestedFor(WireMock.anyUrl()));
        mvc.perform(get("/api/v1/orders")).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"customerId\":\" \",\"sku\":\"JAVA-BOOK\",\"quantity\":2}",
            "{\"customerId\":\"C\",\"sku\":\" \",\"quantity\":2}",
            "{\"customerId\":\"C\",\"sku\":\"JAVA-BOOK\",\"quantity\":0}",
            "{\"customerId\":\"C\",\"sku\":\"JAVA-BOOK\",\"quantity\":-1}",
            "{\"customerId\":\"C\",\"sku\":\"JAVA-BOOK\",\"quantity\":null}",
            "{\"customerId\":\"C\",\"sku\":\"JAVA-BOOK\",\"quantity\":2.5}",
            "{\"customerId\":\"C\",\"sku\":\"JAVA-BOOK\",\"quantity\":2147483648}",
            "{\"customerId\":\"C\",\"sku\":\"JAVA-BOOK\",\"quantity\":\"2\"}"})
    void givenInvalidInput_whenCreatingOrder_thenBadRequestWithoutRemoteCall(String body) throws Exception {
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "invalid")
                        .header("X-Correlation-Id", "invalid-order").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.correlationId").value("invalid-order"));
        INVENTORY.verify(0, WireMock.postRequestedFor(WireMock.anyUrl()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "bad key", "bad/key"})
    void givenInvalidKey_whenCreatingOrder_thenBadRequestWithoutRemoteCall(String key) throws Exception {
        var request = post("/api/v1/orders").contentType("application/json").content(REQUEST);
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        mvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        INVENTORY.verify(0, WireMock.postRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void givenSlowInventory_whenCreatingOrder_thenRealReadTimeoutRetriesWithinBoundedTime() throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse()
                .withStatus(201).withFixedDelay(2500).withBody("{}")));
        long start = System.nanoTime();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "slow")
                        .header("X-Correlation-Id", "slow-correlation").contentType("application/json").content(REQUEST))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("INVENTORY_UNAVAILABLE"));
        assertThat(java.time.Duration.ofNanos(System.nanoTime() - start)).isBetween(
                java.time.Duration.ofMillis(1900), java.time.Duration.ofSeconds(4));
        INVENTORY.verify(2, WireMock.postRequestedFor(WireMock.anyUrl())
                .withHeader("Idempotency-Key", WireMock.equalTo("slow"))
                .withHeader("X-Correlation-Id", WireMock.equalTo("slow-correlation")));
        mvc.perform(get("/api/v1/orders")).andExpect(content().json("[]"));
    }

    @Test
    void givenRepeatedBusinessRejections_whenInventoryRecovers_thenBreakerRemainsClosed() throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(409)
                .withHeader("Content-Type", "application/json").withBody("{\"code\":\"INSUFFICIENT_STOCK\"}")));
        for (int index = 0; index < 6; index++) {
            mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "business-" + index)
                    .contentType("application/json").content(REQUEST)).andExpect(status().isUnprocessableEntity());
        }
        reserveSuccessfully();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "business-recovery")
                .contentType("application/json").content(REQUEST)).andExpect(status().isCreated());
        INVENTORY.verify(7, WireMock.postRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void givenStoppedInventoryServer_whenCreatingOrder_thenConnectionFailureIsBoundedAndSafe() throws Exception {
        INVENTORY.stop();
        long start = System.nanoTime();
        try {
            mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "connection-refused")
                            .contentType("application/json").content(REQUEST))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("INVENTORY_UNAVAILABLE"));
            assertThat(java.time.Duration.ofNanos(System.nanoTime() - start)).isLessThan(java.time.Duration.ofSeconds(4));
            mvc.perform(get("/api/v1/orders")).andExpect(content().json("[]"));
            mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        } finally {
            INVENTORY.start();
        }
    }

    @Test
    void givenOpenCircuit_whenRecoveryTrialFails_thenCircuitReopensUntilNextHealthyTrial() throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(500)));
        for (int index = 0; index < 4; index++) {
            mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "open-" + index)
                    .contentType("application/json").content(REQUEST)).andExpect(status().isServiceUnavailable());
        }
        INVENTORY.verify(8, WireMock.postRequestedFor(WireMock.anyUrl()));
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(3))
                .pollInterval(java.time.Duration.ofMillis(50)).untilAsserted(() -> {
                    mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "failed-trial")
                            .contentType("application/json").content(REQUEST)).andExpect(status().isServiceUnavailable());
                    INVENTORY.verify(10, WireMock.postRequestedFor(WireMock.anyUrl()));
                });
        reserveSuccessfully();
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "healthy-trial")
                .contentType("application/json").content(REQUEST)).andExpect(status().isServiceUnavailable());
        INVENTORY.verify(10, WireMock.postRequestedFor(WireMock.anyUrl()));
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(3))
                .pollInterval(java.time.Duration.ofMillis(50)).untilAsserted(() ->
                        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "healthy-trial")
                                .contentType("application/json").content(REQUEST)).andExpect(status().isCreated()));
        INVENTORY.verify(11, WireMock.postRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void givenUnknownReservationOutcome_whenInventoryFails_thenLogsRetainOrderIdentity(
            CapturedOutput output) throws Exception {
        INVENTORY.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(500)));
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "logged-attempt")
                        .header("X-Correlation-Id", "logged-uncertainty").contentType("application/json").content(REQUEST))
                .andExpect(status().isServiceUnavailable());
        var remoteRequest = INVENTORY.findAll(WireMock.postRequestedFor(WireMock.anyUrl())).getFirst();
        String orderId = json.readTree(remoteRequest.getBodyAsString()).get("orderId").asText();
        assertThat(output.getOut()).contains("orderId=" + orderId, "[order-service,logged-uncertainty]")
                .doesNotContain("CUST-1001", "logged-attempt");
    }

    private void reserveSuccessfully() {
        INVENTORY.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/inventory/JAVA-BOOK/reservations"))
                .willReturn(WireMock.aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"reservationId\":\"" + RESERVATION_ID
                                + "\",\"orderId\":\"{{jsonPath request.body '$.orderId'}}\","
                                + "\"sku\":\"JAVA-BOOK\",\"quantity\":2,\"status\":\"RESERVED\"}")
                        .withTransformers("response-template")));
    }
}
