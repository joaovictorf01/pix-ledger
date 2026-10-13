package dev.joaovictor.pixledger.application.pix;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.application.LedgerService;
import dev.joaovictor.pixledger.application.account.SystemAccounts;
import dev.joaovictor.pixledger.domain.Ledger;
import dev.joaovictor.pixledger.domain.LedgerTransaction;
import dev.joaovictor.pixledger.domain.Posting;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.persistence.AccountRepository;

@Service
public class ReceivePixUseCase {

    private final AccountRepository accounts;
    private final LedgerService ledger;
    private final SystemAccounts systemAccounts;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ReceivePixUseCase(AccountRepository accounts, LedgerService ledger, SystemAccounts systemAccounts,
            ApplicationEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.systemAccounts = systemAccounts;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public UUID execute(ReceivePixCommand command) {
        // Locked so that the same Pix delivered twice at once is recorded once:
        // the second delivery waits, then finds the first one below.
        accounts.findByIdForUpdate(command.customerAccountId())
                .orElseThrow(() -> new AccountNotFoundException(command.customerAccountId().toString()));

        String idempotencyKey = PixIdempotencyKey.of(command.endToEndId(), Ledger.CUSTOMER);
        var existing = ledger.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        // Money comes from the world into the customer. No balance check:
        // receiving never makes a balance negative.
        LedgerTransaction transaction = LedgerTransaction.create(idempotencyKey, Ledger.CUSTOMER, List.of(
                new Posting(systemAccounts.idOf(SystemAccount.CUSTOMER_WORLD), -command.amount()),
                new Posting(command.customerAccountId(), command.amount())),
                clock.instant());
        ledger.record(transaction);

        events.publishEvent(new PixSettledEvent(
                command.endToEndId(), command.amount(), PixSettledEvent.Direction.INCOMING));
        return transaction.getId();
    }
}
