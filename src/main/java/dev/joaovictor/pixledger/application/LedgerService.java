package dev.joaovictor.pixledger.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.Account;
import dev.joaovictor.pixledger.domain.AccountNotFoundException;
import dev.joaovictor.pixledger.domain.InvalidTransactionException;
import dev.joaovictor.pixledger.domain.LedgerEntry;
import dev.joaovictor.pixledger.domain.LedgerTransaction;
import dev.joaovictor.pixledger.persistence.AccountRepository;
import dev.joaovictor.pixledger.persistence.LedgerTransactionRepository;
import jakarta.persistence.EntityManager;

/**
 * The only place that writes to the ledger. A transaction and its entries are
 * saved in the same database transaction: all of them or none.
 */
@Service
public class LedgerService {

    private final LedgerTransactionRepository transactions;
    private final AccountRepository accounts;
    private final EntityManager entityManager;

    public LedgerService(LedgerTransactionRepository transactions, AccountRepository accounts,
            EntityManager entityManager) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.entityManager = entityManager;
    }

    /**
     * Records the transaction, or returns the one already recorded with the
     * same idempotency key. Retrying the same operation never moves money twice.
     */
    @Transactional
    public LedgerTransaction record(LedgerTransaction transaction) {
        var existing = transactions.findByIdempotencyKey(transaction.getIdempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        for (LedgerEntry entry : transaction.getEntries()) {
            Account account = accounts.findById(entry.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(entry.getAccountId().toString()));
            if (account.getLedger() != transaction.getLedger()) {
                throw new InvalidTransactionException("account " + account.getCode() + " belongs to ledger "
                        + account.getLedger() + ", not " + transaction.getLedger());
            }
        }

        // persist instead of save: ids are generated in Java, so save() would
        // first run a SELECT to decide between insert and update.
        entityManager.persist(transaction);
        transaction.getEntries().forEach(entityManager::persist);
        return transaction;
    }
}
