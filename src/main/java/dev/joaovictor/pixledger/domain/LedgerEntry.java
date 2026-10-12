package dev.joaovictor.pixledger.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID transactionId;
    @Column(nullable = false)
    private UUID accountId;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Ledger ledger;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private long amount;

    protected LedgerEntry() {
    }

    public LedgerEntry(UUID transactionId, UUID accountId, Ledger ledger, long amount) {
        if (amount == 0) {
            throw new IllegalArgumentException("amount must not be zero");
        }

        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(ledger, "ledger must not be null");
        this.id = UUID.randomUUID();
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.ledger = ledger;
        this.createdAt = Instant.now();
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getAmount() {
        return amount;
    }
}
