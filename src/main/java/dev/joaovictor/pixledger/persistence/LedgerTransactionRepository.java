package dev.joaovictor.pixledger.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.joaovictor.pixledger.domain.LedgerTransaction;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {

    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);
}
