package dev.joaovictor.pixledger.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Daily Pix limit of a customer account, following the Central Bank rule:
 * a decrease applies immediately; an increase only applies after a waiting
 * period, so a customer forced to raise the limit during a scam is protected.
 *
 * There is at most one pending increase. A new increase replaces it and
 * restarts the waiting period; a decrease cancels it.
 */
@Entity
@Table(name = "pix_limits")
public class PixLimit {

    public static final Duration INCREASE_WAITING_PERIOD = Duration.ofHours(24);

    @Id
    private UUID accountId;
    @Column(nullable = false)
    private long dailyLimit;
    private Long pendingLimit;
    private Instant pendingEffectiveAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected PixLimit() {
    }

    public PixLimit(UUID accountId, long dailyLimit, Instant now) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        requirePositive(dailyLimit);

        this.accountId = accountId;
        this.dailyLimit = dailyLimit;
        this.updatedAt = now;
    }

    /** The limit in force at the given moment, including a matured increase. */
    public long currentLimit(Instant now) {
        return hasMaturedIncrease(now) ? pendingLimit : dailyLimit;
    }

    public void requestChange(long newLimit, Instant now) {
        requirePositive(newLimit);
        promoteMaturedIncrease(now);

        if (newLimit > dailyLimit) {
            pendingLimit = newLimit;
            pendingEffectiveAt = now.plus(INCREASE_WAITING_PERIOD);
        } else {
            dailyLimit = newLimit;
            pendingLimit = null;
            pendingEffectiveAt = null;
        }
        updatedAt = now;
    }

    /** Pending increase still waiting, or null. */
    public Long pendingLimit(Instant now) {
        return hasMaturedIncrease(now) ? null : pendingLimit;
    }

    /** When the pending increase applies, or null. */
    public Instant pendingEffectiveAt(Instant now) {
        return hasMaturedIncrease(now) ? null : pendingEffectiveAt;
    }

    public UUID getAccountId() {
        return accountId;
    }

    private boolean hasMaturedIncrease(Instant now) {
        return pendingLimit != null && !now.isBefore(pendingEffectiveAt);
    }

    private void promoteMaturedIncrease(Instant now) {
        if (hasMaturedIncrease(now)) {
            dailyLimit = pendingLimit;
            pendingLimit = null;
            pendingEffectiveAt = null;
        }
    }

    private static void requirePositive(long limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }
}
