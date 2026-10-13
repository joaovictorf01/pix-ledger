package dev.joaovictor.pixledger.application.treasury;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dev.joaovictor.pixledger.application.LedgerService;
import dev.joaovictor.pixledger.application.account.SystemAccounts;
import dev.joaovictor.pixledger.domain.BusinessTime;
import dev.joaovictor.pixledger.domain.CustomerExposure;
import dev.joaovictor.pixledger.domain.Ledger;
import dev.joaovictor.pixledger.domain.LedgerTransaction;
import dev.joaovictor.pixledger.domain.PixLimit;
import dev.joaovictor.pixledger.domain.Posting;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.persistence.AccountRepository;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;
import dev.joaovictor.pixledger.persistence.PixLimitRepository;

/**
 * Shared operations of the treasury jobs: reading the position, moving money
 * between internal accounts and taking the lock that keeps them in order.
 */
@Component
public class Treasury {

    static final String RESERVE_CAPITAL_KEY_PREFIX = "reserve:capital:";

    private final AccountRepository accounts;
    private final LedgerEntryRepository entries;
    private final PixLimitRepository limits;
    private final LedgerService ledger;
    private final SystemAccounts systemAccounts;

    public Treasury(AccountRepository accounts, LedgerEntryRepository entries, PixLimitRepository limits,
            LedgerService ledger, SystemAccounts systemAccounts) {
        this.accounts = accounts;
        this.entries = entries;
        this.limits = limits;
        this.ledger = ledger;
        this.systemAccounts = systemAccounts;
    }

    /**
     * Locks the Instant Payments account. Every treasury operation and every
     * Pix reaching the internal ledger takes this lock, so a job never works
     * on a balance that is changing under it. Call inside a transaction.
     */
    public void lock() {
        accounts.findByIdForUpdate(systemAccounts.idOf(SystemAccount.INSTANT_PAYMENTS));
    }

    public TreasuryPosition position(Instant now) {
        long reserve = balanceOf(SystemAccount.FINANCIAL_RESERVE);
        long capital = entries.sumOfAccountByKeyPrefix(
                systemAccounts.idOf(SystemAccount.FINANCIAL_RESERVE), RESERVE_CAPITAL_KEY_PREFIX);

        return new TreasuryPosition(
                -balanceOf(SystemAccount.CUSTOMER_WORLD),
                balanceOf(SystemAccount.CUSTOMER_FUNDS),
                balanceOf(SystemAccount.INSTANT_PAYMENTS),
                reserve,
                capital,
                capital - reserve,
                instantPaymentsTarget(now));
    }

    public long balanceOf(SystemAccount account) {
        return entries.balanceOf(systemAccounts.idOf(account));
    }

    public long sumOfLedger(Ledger ledgerToSum) {
        return entries.sumOfLedger(ledgerToSum);
    }

    /** Moves money between two internal accounts. Amounts of zero or less are ignored. */
    public void move(SystemAccount from, SystemAccount to, long amount, String idempotencyKey, Instant now) {
        if (amount <= 0) {
            return;
        }
        ledger.record(LedgerTransaction.create(idempotencyKey, Ledger.INTERNAL, List.of(
                new Posting(systemAccounts.idOf(from), -amount),
                new Posting(systemAccounts.idOf(to), amount)), now));
    }

    private long instantPaymentsTarget(Instant now) {
        Map<UUID, PixLimit> limitsByAccount = limits.findAll().stream()
                .collect(Collectors.toMap(PixLimit::getAccountId, Function.identity()));

        List<CustomerExposure> exposures = entries.customerActivity(BusinessTime.startOfDay(now)).stream()
                .map(activity -> new CustomerExposure(
                        activity.getBalance(),
                        limitsByAccount.get(activity.getAccountId()).currentLimit(now),
                        activity.getSentSince()))
                .toList();
        return CustomerExposure.instantPaymentsTarget(exposures);
    }
}
