package org.example.inventory.model;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {
    @Test
    void givenNonpositiveQuantity_whenCreatingReservation_thenInvalidStateIsRejected() {
        assertThatThrownBy(() -> new Reservation(UUID.randomUUID(), UUID.randomUUID(), "JAVA-BOOK", 0,
                "reservation", Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }
}
