package dev.joaovictor.pixledger.domain;

import java.util.Collection;

/**
 * How much one customer could still send by Pix today.
 *
 * @param balance    current balance
 * @param limitToday daily Pix limit in force now
 * @param sentToday  already sent since midnight in Brasília
 */
public record CustomerExposure(long balance, long limitToday, long sentToday) {

    /** The smaller of the balance and what is left of today's limit. */
    public long maxSendable() {
        long limitLeft = Math.max(0, limitToday - sentToday);
        return Math.max(0, Math.min(balance, limitLeft));
    }

    /**
     * What the Instant Payments account must hold so that no legitimate Pix
     * fails, even if every customer sends everything they can at once.
     */
    public static long instantPaymentsTarget(Collection<CustomerExposure> exposures) {
        long target = 0;
        for (CustomerExposure exposure : exposures) {
            target = Math.addExact(target, exposure.maxSendable());
        }
        return target;
    }
}
