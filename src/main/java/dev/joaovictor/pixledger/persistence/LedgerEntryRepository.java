package dev.joaovictor.pixledger.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.joaovictor.pixledger.domain.Ledger;
import dev.joaovictor.pixledger.domain.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.accountId = :accountId")
    long balanceOf(@Param("accountId") UUID accountId);

    /** Total that left the account since the given moment, as a positive number. */
    @Query("select coalesce(-sum(e.amount), 0) from LedgerEntry e "
            + "where e.accountId = :accountId and e.amount < 0 and e.createdAt >= :since")
    long outgoingSince(@Param("accountId") UUID accountId, @Param("since") Instant since);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    /** Must be zero for each ledger: every transaction adds up to zero. */
    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.ledger = :ledger")
    long sumOfLedger(@Param("ledger") Ledger ledger);

    /** Money put into an account by transactions whose key starts with the prefix. */
    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e, LedgerTransaction t "
            + "where e.transactionId = t.id and e.accountId = :accountId and t.idempotencyKey like concat(:prefix, '%')")
    long sumOfAccountByKeyPrefix(@Param("accountId") UUID accountId, @Param("prefix") String prefix);

    /** Balance and amount sent since a moment, for every customer account with a Pix limit. */
    @Query(nativeQuery = true, value = """
            SELECT a.id AS accountId,
                   CAST(COALESCE(SUM(e.amount), 0) AS BIGINT) AS balance,
                   CAST(COALESCE(SUM(CASE WHEN e.amount < 0 AND e.created_at >= :since
                                          THEN -e.amount ELSE 0 END), 0) AS BIGINT) AS sentSince
              FROM accounts a
              JOIN pix_limits l ON l.account_id = a.id
              LEFT JOIN ledger_entries e ON e.account_id = a.id
             WHERE a.ledger = 'CUSTOMER'
             GROUP BY a.id
            """)
    List<CustomerActivity> customerActivity(@Param("since") Instant since);

    interface CustomerActivity {
        UUID getAccountId();

        long getBalance();

        long getSentSince();
    }
}
