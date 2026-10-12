package dev.joaovictor.pixledger.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dev.joaovictor.pixledger.domain.exception.InvalidTransactionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    private static final int MIN_POSTINGS = 2;

    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Ledger ledger;
    @Column(nullable = false)
    private Instant createdAt;

    // Built together with the transaction so both are saved at once.
    // Not loaded back from the database: entries are queried by transactionId.
    @Transient
    private List<LedgerEntry> entries = List.of();

    protected LedgerTransaction() {
    }

    private LedgerTransaction(String idempotencyKey, Ledger ledger) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.ledger = ledger;
        this.createdAt = Instant.now();
    }

    /**
     * Creates a balanced transaction. Every posting becomes one entry in the
     * same ledger, and the amounts must add up to zero: money only moves,
     * it is never created or destroyed.
     */
    public static LedgerTransaction create(String idempotencyKey, Ledger ledger, List<Posting> postings) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransactionException("idempotencyKey must not be blank");
        }
        Objects.requireNonNull(ledger, "ledger must not be null");
        Objects.requireNonNull(postings, "postings must not be null");

        if (postings.size() < MIN_POSTINGS) {
            throw new InvalidTransactionException(
                    "a transaction needs at least " + MIN_POSTINGS + " postings, got " + postings.size());
        }

        long sum = 0;
        try {
            for (Posting posting : postings) {
                sum = Math.addExact(sum, posting.amount());
            }
        } catch (ArithmeticException overflow) {
            throw new InvalidTransactionException("posting amounts overflow");
        }
        if (sum != 0) {
            throw new InvalidTransactionException("postings must add up to zero, got " + sum);
        }

        LedgerTransaction transaction = new LedgerTransaction(idempotencyKey, ledger);
        transaction.entries = postings.stream()
                .map(posting -> new LedgerEntry(transaction.id, posting.accountId(), ledger, posting.amount()))
                .toList();
        return transaction;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<LedgerEntry> getEntries() {
        return entries;
    }
}
