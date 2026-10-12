package dev.joaovictor.pixledger.domain;

import java.util.UUID;

/**
 * One side of a transaction before it is recorded: which account and how much.
 * Positive amounts go into the account, negative amounts come out of it.
 */
public record Posting(UUID accountId, long amount) {
}
