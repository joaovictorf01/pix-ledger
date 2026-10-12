package dev.joaovictor.pixledger.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.joaovictor.pixledger.domain.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.accountId = :accountId")
    long balanceOf(@Param("accountId") UUID accountId);

    /** Total that left the account since the given moment, as a positive number. */
    @Query("select coalesce(-sum(e.amount), 0) from LedgerEntry e "
            + "where e.accountId = :accountId and e.amount < 0 and e.createdAt >= :since")
    long outgoingSince(@Param("accountId") UUID accountId, @Param("since") Instant since);

    List<LedgerEntry> findByTransactionId(UUID transactionId);
}
