package org.example.inventory.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiTest {
    private final MockMvc mvc;

    @Autowired
    OpenApiTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void givenRunningService_whenReadingOpenApi_thenBusinessContractAndServiceIdentityAreAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Inventory API"))
                .andExpect(jsonPath("$.info.version").value("0.0.1-SNAPSHOT"))
                .andExpect(jsonPath("$.paths['/api/v1/inventory/{sku}']").exists())
                .andExpect(jsonPath("$.paths['/actuator/health']").doesNotExist());
    }
    @Test
    void givenApiConsumer_whenInspectingPost_thenHeadersExamplesAndOutcomeSchemasAreUsable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/inventory/{sku}/reservations'].post.parameters[?(@.name == 'Idempotency-Key')].required")
                        .value(org.hamcrest.Matchers.contains(true)))
                .andExpect(jsonPath("$.paths['/api/v1/inventory/{sku}/reservations'].post.parameters[?(@.name == 'X-Correlation-Id')].required")
                        .value(org.hamcrest.Matchers.contains(false)))
                .andExpect(jsonPath("$.paths['/api/v1/inventory/{sku}/reservations'].post.responses['201'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ReservationResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/inventory/{sku}/reservations'].post.responses['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ApiError"))
                .andExpect(jsonPath("$.components.schemas.ReservationRequest.example.quantity").value(2));
    }

    @Test
    void givenApiConsumer_whenOpeningSwagger_thenUiAndLocalApiConfigurationAreServed() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

}
