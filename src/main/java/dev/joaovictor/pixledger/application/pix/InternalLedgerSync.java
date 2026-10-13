package dev.joaovictor.pixledger.application.pix;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.joaovictor.pixledger.application.LedgerService;
import dev.joaovictor.pixledger.application.account.SystemAccounts;
import dev.joaovictor.pixledger.domain.Ledger;
import dev.joaovictor.pixledger.domain.LedgerTransaction;
import dev.joaovictor.pixledger.domain.Posting;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.persistence.AccountRepository;

/**
 * Brings the internal ledger up to date after a Pix is recorded for the
 * customer. Runs only after the customer transaction commits, in a new
 * transaction of its own: this is the "eventually consistent" step.
 *
 * Today the event is in-process. If the application stops between the two
 * commits, the internal side is missing until it is replayed; the invariants
 * check shows the difference.
 */
@Component
public class InternalLedgerSync {

    private final AccountRepository accounts;
    private final LedgerService ledger;
    private final SystemAccounts systemAccounts;
    private final Clock clock;

    public InternalLedgerSync(AccountRepository accounts, LedgerService ledger, SystemAccounts systemAccounts,
            Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.systemAccounts = systemAccounts;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PixSettledEvent event) {
        // Same lock the treasury jobs take, so the 18h closing never reads the
        // Instant Payments balance while a Pix is changing it.
        accounts.findByIdForUpdate(systemAccounts.idOf(SystemAccount.INSTANT_PAYMENTS));

        long instantPaymentsChange = switch (event.direction()) {
            case OUTGOING -> -event.amount();
            case INCOMING -> event.amount();
        };

        String idempotencyKey = PixIdempotencyKey.of(event.endToEndId(), Ledger.INTERNAL);
        ledger.record(LedgerTransaction.create(idempotencyKey, Ledger.INTERNAL, List.of(
                new Posting(systemAccounts.idOf(SystemAccount.INSTANT_PAYMENTS), instantPaymentsChange),
                new Posting(systemAccounts.idOf(SystemAccount.INTERNAL_WORLD), -instantPaymentsChange)),
                clock.instant()));
    }
}
