package dev.joaovictor.pixledger.application.pix;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.application.LedgerService;
import dev.joaovictor.pixledger.application.account.SystemAccounts;
import dev.joaovictor.pixledger.application.limit.DailyPixLimitPolicy;
import dev.joaovictor.pixledger.domain.Ledger;
import dev.joaovictor.pixledger.domain.LedgerTransaction;
import dev.joaovictor.pixledger.domain.Posting;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.domain.exception.InsufficientFundsException;
import dev.joaovictor.pixledger.persistence.AccountRepository;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;

@Service
public class SendPixUseCase {

    private final AccountRepository accounts;
    private final LedgerEntryRepository entries;
    private final LedgerService ledger;
    private final DailyPixLimitPolicy dailyLimit;
    private final SystemAccounts systemAccounts;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public SendPixUseCase(AccountRepository accounts, LedgerEntryRepository entries, LedgerService ledger,
            DailyPixLimitPolicy dailyLimit, SystemAccounts systemAccounts, ApplicationEventPublisher events,
            Clock clock) {
        this.accounts = accounts;
        this.entries = entries;
        this.ledger = ledger;
        this.dailyLimit = dailyLimit;
        this.systemAccounts = systemAccounts;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID execute(SendPixCommand command) {
        // 1. Lock the customer's account. Another Pix from the same account
        //    waits here; Pix from other accounts are not affected.
        accounts.findByIdForUpdate(command.customerAccountId())
                .orElseThrow(() -> new AccountNotFoundException(command.customerAccountId().toString()));

        // 2. Same endToEndId already recorded: a retry of the same Pix returns it;
        //    the same id with different data is refused.
        String idempotencyKey = PixIdempotencyKey.of(command.endToEndId(), Ledger.CUSTOMER);
        var existing = ledger.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return PixRetry.requireSame(existing.get(), entries, command.customerAccountId(), -command.amount(),
                    command.endToEndId());
        }

        // 3. Only now, holding the lock, the balance and the daily limit are read.
        long balance = entries.balanceOf(command.customerAccountId());
        if (balance < command.amount()) {
            throw new InsufficientFundsException(command.customerAccountId(), balance, command.amount());
        }
        dailyLimit.check(command.customerAccountId(), command.amount());

        // 4. Customer ledger: money leaves the customer and goes to the world.
        LedgerTransaction transaction = LedgerTransaction.create(idempotencyKey, Ledger.CUSTOMER, List.of(
                new Posting(command.customerAccountId(), -command.amount()),
                new Posting(systemAccounts.idOf(SystemAccount.CUSTOMER_WORLD), command.amount())),
                clock.instant());
        ledger.record(transaction);

        // 5. The internal ledger follows after this transaction commits.
        events.publishEvent(new PixSettledEvent(
                command.endToEndId(), command.amount(), PixSettledEvent.Direction.OUTGOING));
        return transaction.getId();
    }
}
