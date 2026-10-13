package dev.joaovictor.pixledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class OperatingHoursTest {

    private final OperatingHours hours = new OperatingHours(LocalTime.of(8, 0), LocalTime.of(18, 0));

    @Test
    void shouldMakeCustomerFundsAvailableFromEightUntilJustBeforeEighteenInBrasilia() {
        // Brasília is UTC-3
        assertThat(hours.customerFundsAvailable(Instant.parse("2026-10-13T10:59:59Z"))).isFalse(); // 07:59:59
        assertThat(hours.customerFundsAvailable(Instant.parse("2026-10-13T11:00:00Z"))).isTrue(); // 08:00
        assertThat(hours.customerFundsAvailable(Instant.parse("2026-10-13T20:59:59Z"))).isTrue(); // 17:59:59
        assertThat(hours.customerFundsAvailable(Instant.parse("2026-10-13T21:00:00Z"))).isFalse(); // 18:00
    }

    @Test
    void shouldRejectOpeningAfterClosing() {
        assertThatThrownBy(() -> new OperatingHours(LocalTime.of(18, 0), LocalTime.of(8, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
