package dev.joaovictor.pixledger.application.account;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.persistence.AccountRepository;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;

@Service
public class GetBalanceUseCase {

    private final AccountRepository accounts;
    private final LedgerEntryRepository entries;

    public GetBalanceUseCase(AccountRepository accounts, LedgerEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public long execute(UUID accountId) {
        if (!accounts.existsById(accountId)) {
            throw new AccountNotFoundException(accountId.toString());
        }
        return entries.balanceOf(accountId);
    }
}
