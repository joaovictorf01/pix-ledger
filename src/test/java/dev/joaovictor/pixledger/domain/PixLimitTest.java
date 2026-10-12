package dev.joaovictor.pixledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PixLimitTest {

    private final Instant start = Instant.parse("2026-10-12T15:00:00Z");
    private final PixLimit limit = new PixLimit(UUID.randomUUID(), 1_000_00, start);

    @Test
    void decreaseAppliesImmediately() {
        limit.requestChange(500_00, start);

        assertThat(limit.currentLimit(start)).isEqualTo(500_00);
        assertThat(limit.pendingLimit(start)).isNull();
    }

    @Test
    void increaseWaitsTwentyFourHours() {
        limit.requestChange(5_000_00, start);

        Instant almost = start.plus(Duration.ofHours(24)).minusSeconds(1);
        assertThat(limit.currentLimit(almost)).isEqualTo(1_000_00);
        assertThat(limit.pendingLimit(almost)).isEqualTo(5_000_00);
        assertThat(limit.pendingEffectiveAt(almost)).isEqualTo(start.plus(Duration.ofHours(24)));

        Instant after = start.plus(Duration.ofHours(24));
        assertThat(limit.currentLimit(after)).isEqualTo(5_000_00);
        assertThat(limit.pendingLimit(after)).isNull();
    }

    @Test
    void decreaseCancelsPendingIncrease() {
        limit.requestChange(5_000_00, start);
        limit.requestChange(500_00, start.plus(Duration.ofHours(2)));

        Instant dayAfter = start.plus(Duration.ofHours(48));
        assertThat(limit.currentLimit(dayAfter)).isEqualTo(500_00);
        assertThat(limit.pendingLimit(dayAfter)).isNull();
    }

    @Test
    void newIncreaseReplacesPendingOneAndRestartsTheWait() {
        limit.requestChange(3_000_00, start);
        Instant later = start.plus(Duration.ofHours(10));
        limit.requestChange(5_000_00, later);

        assertThat(limit.currentLimit(start.plus(Duration.ofHours(24)))).isEqualTo(1_000_00);
        assertThat(limit.currentLimit(later.plus(Duration.ofHours(24)))).isEqualTo(5_000_00);
    }

    @Test
    void maturedIncreaseIsTheBaseForTheNextRequest() {
        limit.requestChange(5_000_00, start);
        Instant afterIncrease = start.plus(Duration.ofHours(25));

        // 3.000 is below the 5.000 already in force, so it is a decrease
        limit.requestChange(3_000_00, afterIncrease);

        assertThat(limit.currentLimit(afterIncrease)).isEqualTo(3_000_00);
    }

    @Test
    void rejectsZeroOrNegativeLimit() {
        assertThatThrownBy(() -> limit.requestChange(0, start)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limit.requestChange(-1, start)).isInstanceOf(IllegalArgumentException.class);
    }
}
