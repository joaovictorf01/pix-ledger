package dev.joaovictor.pixledger.application.pix;

/**
 * Published when the customer ledger has recorded a Pix. The internal ledger
 * is updated from this event, after the customer side is committed.
 */
public record PixSettledEvent(String endToEndId, long amount, Direction direction) {

    public enum Direction {
        OUTGOING,
        INCOMING
    }
}
