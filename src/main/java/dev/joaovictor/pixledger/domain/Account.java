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

/**
 * An account in one ledger. It has no balance column: the balance is always
 * the sum of its entries. The row exists to be locked while a balance is
 * checked, so two payments from the same account run one after the other.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Ledger ledger;
    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(Ledger ledger, String code) {
        Objects.requireNonNull(ledger, "ledger must not be null");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }

        this.id = UUID.randomUUID();
        this.ledger = ledger;
        this.code = code;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public String getCode() {
        return code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
