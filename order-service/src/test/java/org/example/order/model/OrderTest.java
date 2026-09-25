package org.example.order.model;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    @Test
    void givenConfirmedOrderWithoutReservation_whenConstructing_thenInvalidOutcomeIsRejected() {
        assertThatThrownBy(() -> new Order(UUID.randomUUID(), "CUST-1", "JAVA-BOOK", 2,
                OrderStatus.CONFIRMED, null, null, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }
}
