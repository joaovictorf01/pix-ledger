package dev.joaovictor.pixledger.application.account;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.persistence.AccountRepository;

/**
 * Finds the database id of each system account. The ids never change after
 * the migration runs, so they are cached after the first lookup.
 */
@Component
public class SystemAccounts {

    private final AccountRepository accounts;
    private final Map<SystemAccount, UUID> ids = new ConcurrentHashMap<>();

    public SystemAccounts(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public UUID idOf(SystemAccount systemAccount) {
        return ids.computeIfAbsent(systemAccount, account -> accounts
                .findByLedgerAndCode(account.ledger(), account.code())
                .orElseThrow(() -> new AccountNotFoundException(account.name()))
                .getId());
    }
}
