package dev.joaovictor.pixledger.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.joaovictor.pixledger.domain.Account;
import dev.joaovictor.pixledger.domain.Ledger;
import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByLedgerAndCode(Ledger ledger, String code);

    /**
     * SELECT ... FOR UPDATE: a second caller for the same account waits here
     * until the first transaction commits or rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);
}
