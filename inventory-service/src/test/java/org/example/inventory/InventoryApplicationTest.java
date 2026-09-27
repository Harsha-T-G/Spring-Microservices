package org.example.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InventoryApplicationTest {
    @Test
    void givenIndependentConfiguration_whenApplicationStarts_thenContextLoads() {
    }
}
