package dev.joaovictor.pixledger.application.account;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.Account;
import dev.joaovictor.pixledger.domain.Ledger;
import dev.joaovictor.pixledger.domain.PixLimit;
import dev.joaovictor.pixledger.persistence.AccountRepository;
import dev.joaovictor.pixledger.persistence.PixLimitRepository;

@Service
public class OpenCustomerAccountUseCase {

    private final AccountRepository accounts;
    private final PixLimitRepository limits;
    private final Clock clock;
    private final long defaultDailyLimit;

    public OpenCustomerAccountUseCase(AccountRepository accounts, PixLimitRepository limits, Clock clock,
            @Value("${pixledger.pix.default-daily-limit}") long defaultDailyLimit) {
        this.accounts = accounts;
        this.limits = limits;
        this.clock = clock;
        this.defaultDailyLimit = defaultDailyLimit;
    }

    /** Opens the account with the default Pix limit, or returns the existing one with the same code. */
    @Transactional
    public Account execute(String customerCode) {
        return accounts.findByLedgerAndCode(Ledger.CUSTOMER, customerCode)
                .orElseGet(() -> {
                    Account account = accounts.save(new Account(Ledger.CUSTOMER, customerCode));
                    limits.save(new PixLimit(account.getId(), defaultDailyLimit, clock.instant()));
                    return account;
                });
    }
}
