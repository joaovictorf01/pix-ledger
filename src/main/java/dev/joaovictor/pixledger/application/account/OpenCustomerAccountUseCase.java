package dev.joaovictor.pixledger.application.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.Account;
import dev.joaovictor.pixledger.domain.Ledger;
import dev.joaovictor.pixledger.persistence.AccountRepository;

@Service
public class OpenCustomerAccountUseCase {

    private final AccountRepository accounts;

    public OpenCustomerAccountUseCase(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /** Opens the account, or returns the existing one with the same code. */
    @Transactional
    public Account execute(String customerCode) {
        return accounts.findByLedgerAndCode(Ledger.CUSTOMER, customerCode)
                .orElseGet(() -> accounts.save(new Account(Ledger.CUSTOMER, customerCode)));
    }
}
