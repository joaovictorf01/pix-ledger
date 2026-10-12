package dev.joaovictor.pixledger.application.limit;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dev.joaovictor.pixledger.domain.BusinessTime;
import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.domain.exception.PixLimitExceededException;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;
import dev.joaovictor.pixledger.persistence.PixLimitRepository;

/**
 * Checks that a Pix fits in what the customer may still send today, counted
 * from midnight in Brasília. Callers must hold the account lock, otherwise two
 * payments could both read the same amount already sent.
 */
@Component
public class DailyPixLimitPolicy {

    private final PixLimitRepository limits;
    private final LedgerEntryRepository entries;
    private final Clock clock;

    public DailyPixLimitPolicy(PixLimitRepository limits, LedgerEntryRepository entries, Clock clock) {
        this.limits = limits;
        this.entries = entries;
        this.clock = clock;
    }

    public void check(UUID customerAccountId, long amount) {
        Instant now = clock.instant();
        long limitToday = limits.findById(customerAccountId)
                .orElseThrow(() -> new AccountNotFoundException(customerAccountId.toString()))
                .currentLimit(now);
        long sentToday = entries.outgoingSince(customerAccountId, BusinessTime.startOfDay(now));

        if (sentToday + amount > limitToday) {
            throw new PixLimitExceededException(customerAccountId, limitToday, sentToday, amount);
        }
    }
}
