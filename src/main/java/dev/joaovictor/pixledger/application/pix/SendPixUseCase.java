package dev.joaovictor.pixledger.application.pix;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.application.LedgerService;
import dev.joaovictor.pixledger.application.account.SystemAccounts;
import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.domain.BusinessTime;
import dev.joaovictor.pixledger.domain.exception.InsufficientFundsException;
import dev.joaovictor.pixledger.domain.Ledger;
import dev.joaovictor.pixledger.domain.LedgerTransaction;
import dev.joaovictor.pixledger.domain.PixLimit;
import dev.joaovictor.pixledger.domain.exception.PixLimitExceededException;
import dev.joaovictor.pixledger.domain.Posting;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.persistence.AccountRepository;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;
import dev.joaovictor.pixledger.persistence.LedgerTransactionRepository;
import dev.joaovictor.pixledger.persistence.PixLimitRepository;

@Service
public class SendPixUseCase {

    private final AccountRepository accounts;
    private final LedgerEntryRepository entries;
    private final LedgerTransactionRepository transactions;
    private final LedgerService ledger;
    private final SystemAccounts systemAccounts;
    private final ApplicationEventPublisher events;
    private final PixLimitRepository limits;
    private final Clock clock;

    public SendPixUseCase(AccountRepository accounts, LedgerEntryRepository entries,
            LedgerTransactionRepository transactions, LedgerService ledger, SystemAccounts systemAccounts,
            ApplicationEventPublisher events, PixLimitRepository limits, Clock clock) {
        this.accounts = accounts;
        this.entries = entries;
        this.transactions = transactions;
        this.ledger = ledger;
        this.systemAccounts = systemAccounts;
        this.events = events;
        this.limits = limits;
        this.clock = clock;
    }

    @Transactional
    public UUID execute(SendPixCommand command) {
        // 1. Lock the customer's account. Another Pix from the same account
        //    waits here; Pix from other accounts are not affected.
        accounts.findByIdForUpdate(command.customerAccountId())
                .orElseThrow(() -> new AccountNotFoundException(command.customerAccountId().toString()));

        // 2. Same endToEndId already recorded: it is a retry, return it as is.
        String idempotencyKey = "pix:" + command.endToEndId() + ":" + Ledger.CUSTOMER;
        var existing = transactions.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        // 3. Only now, holding the lock, the balance is read.
        long balance = entries.balanceOf(command.customerAccountId());
        if (balance < command.amount()) {
            throw new InsufficientFundsException(command.customerAccountId(), balance, command.amount());
        }

        // 4. Daily limit, counted from midnight in Brasília.
        Instant now = clock.instant();
        PixLimit limit = limits.findById(command.customerAccountId())
                .orElseThrow(() -> new AccountNotFoundException(command.customerAccountId().toString()));
        long limitToday = limit.currentLimit(now);
        long sentToday = entries.outgoingSince(command.customerAccountId(), BusinessTime.startOfDay(now));
        if (sentToday + command.amount() > limitToday) {
            throw new PixLimitExceededException(command.customerAccountId(), limitToday, sentToday, command.amount());
        }

        // 5. Customer ledger: money leaves the customer and goes to the world.
        LedgerTransaction transaction = LedgerTransaction.create(idempotencyKey, Ledger.CUSTOMER, List.of(
                new Posting(command.customerAccountId(), -command.amount()),
                new Posting(systemAccounts.idOf(SystemAccount.CUSTOMER_WORLD), command.amount())));
        ledger.record(transaction);

        // 6. The internal ledger follows after this transaction commits.
        events.publishEvent(new PixSettledEvent(
                command.endToEndId(), command.amount(), PixSettledEvent.Direction.OUTGOING));
        return transaction.getId();
    }
}
