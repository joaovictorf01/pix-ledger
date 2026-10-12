package dev.joaovictor.pixledger.application.limit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.joaovictor.pixledger.domain.PixLimit;
import dev.joaovictor.pixledger.domain.exception.PixLimitExceededException;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;
import dev.joaovictor.pixledger.persistence.PixLimitRepository;

class DailyPixLimitPolicyTest {

    // 01:30 UTC on Oct 13 is still 22:30 on Oct 12 in Brasília
    private final Instant now = Instant.parse("2026-10-13T01:30:00Z");
    private final Instant brasiliaMidnight = Instant.parse("2026-10-12T03:00:00Z");

    private final UUID account = UUID.randomUUID();
    private final PixLimitRepository limits = mock(PixLimitRepository.class);
    private final LedgerEntryRepository entries = mock(LedgerEntryRepository.class);
    private final DailyPixLimitPolicy policy = new DailyPixLimitPolicy(limits, entries,
            Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void shouldAllowPixThatFitsInWhatIsLeftToday() {
        // given
        givenLimit(1_000_00);
        when(entries.outgoingSince(account, brasiliaMidnight)).thenReturn(700_00L);

        // when / then
        assertThatCode(() -> policy.check(account, 300_00)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectPixThatGoesOverTheLimit() {
        // given
        givenLimit(1_000_00);
        when(entries.outgoingSince(account, brasiliaMidnight)).thenReturn(700_00L);

        // when / then
        assertThatThrownBy(() -> policy.check(account, 300_01))
                .isInstanceOf(PixLimitExceededException.class);
    }

    private void givenLimit(long dailyLimit) {
        when(limits.findById(account)).thenReturn(Optional.of(new PixLimit(account, dailyLimit, now)));
    }
}
