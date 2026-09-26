package org.example.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderApplicationTest {
    @Test
    void givenIndependentConfiguration_whenApplicationStarts_thenContextLoads() {
    }
}
