package dev.joaovictor.pixledger.application.pix;

import java.util.UUID;

import dev.joaovictor.pixledger.domain.LedgerTransaction;
import dev.joaovictor.pixledger.domain.exception.IdempotencyConflictException;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;

/**
 * A Pix arriving with an endToEndId already recorded is a retry only if it
 * asks for exactly the same thing: same account, same amount, same direction.
 * Anything else reusing the id is refused instead of answered with the old payment.
 */
final class PixRetry {

    private PixRetry() {
    }

    static UUID requireSame(LedgerTransaction existing, LedgerEntryRepository entries, UUID customerAccountId,
            long expectedCustomerAmount, String endToEndId) {
        boolean same = entries.findByTransactionId(existing.getId()).stream()
                .anyMatch(entry -> entry.getAccountId().equals(customerAccountId)
                        && entry.getAmount() == expectedCustomerAmount);
        if (!same) {
            throw new IdempotencyConflictException(endToEndId);
        }
        return existing.getId();
    }
}
