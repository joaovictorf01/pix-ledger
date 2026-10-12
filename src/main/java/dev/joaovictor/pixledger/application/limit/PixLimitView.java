package dev.joaovictor.pixledger.application.limit;

import java.time.Instant;
import java.util.UUID;

import dev.joaovictor.pixledger.domain.PixLimit;

/**
 * @param status ACTIVE, or PENDING_INCREASE while an increase waits its 24 hours
 */
public record PixLimitView(UUID accountId, long dailyLimit, Long pendingLimit, Instant pendingEffectiveAt,
        String status) {

    static PixLimitView of(PixLimit limit, Instant now) {
        Long pending = limit.pendingLimit(now);
        return new PixLimitView(limit.getAccountId(), limit.currentLimit(now), pending,
                limit.pendingEffectiveAt(now), pending == null ? "ACTIVE" : "PENDING_INCREASE");
    }
}
