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

@SpringBootTest(properties = "spring.datasource.url=jdbc:tc:postgresql:17-alpine:///inventory_dev_test")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class DevelopmentStockTest {
    private final MockMvc mvc;

    @Autowired
    DevelopmentStockTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void givenConfiguredProfile_whenLookingUpStock_thenOnlyExplicitSeedIsAvailable() throws Exception {
        mvc.perform(get("/api/v1/inventory/java-book")).andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(20));
        mvc.perform(get("/api/v1/inventory/KEYBOARD-01")).andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(10));
        mvc.perform(get("/api/v1/inventory/MONITOR-24")).andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(5));
    }
}
