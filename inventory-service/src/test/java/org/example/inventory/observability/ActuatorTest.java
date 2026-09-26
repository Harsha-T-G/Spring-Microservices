package org.example.inventory.observability;

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
class ActuatorTest {
    private final MockMvc mvc;

    @Autowired
    ActuatorTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void givenRunningService_whenInspectingActuator_thenOnlyLocalHealthAndInfoAreExposed() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("inventory-service"))
                .andExpect(jsonPath("$.app.version").value("0.0.1-SNAPSHOT"));
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }
}
