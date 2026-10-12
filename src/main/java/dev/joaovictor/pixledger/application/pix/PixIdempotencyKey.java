package dev.joaovictor.pixledger.application.pix;

import dev.joaovictor.pixledger.domain.Ledger;

/**
 * One Pix produces one transaction per ledger. Both sides use the endToEndId,
 * so a retried Pix or a repeated event finds what was already recorded.
 */
final class PixIdempotencyKey {

    private PixIdempotencyKey() {
    }

    static String of(String endToEndId, Ledger ledger) {
        return "pix:" + endToEndId + ":" + ledger;
    }
}
