package dev.joaovictor.pixledger.application.limit;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.PixLimit;
import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.persistence.AccountRepository;
import dev.joaovictor.pixledger.persistence.PixLimitRepository;

@Service
public class ChangePixLimitUseCase {

    private final AccountRepository accounts;
    private final PixLimitRepository limits;
    private final Clock clock;

    public ChangePixLimitUseCase(AccountRepository accounts, PixLimitRepository limits, Clock clock) {
        this.accounts = accounts;
        this.limits = limits;
        this.clock = clock;
    }

    @Transactional
    public PixLimitView execute(UUID customerAccountId, long newLimit) {
        // Same lock as a Pix: a limit change and a payment from the same
        // account never interleave.
        accounts.findByIdForUpdate(customerAccountId)
                .orElseThrow(() -> new AccountNotFoundException(customerAccountId.toString()));
        PixLimit limit = limits.findById(customerAccountId)
                .orElseThrow(() -> new AccountNotFoundException(customerAccountId.toString()));

        Instant now = clock.instant();
        limit.requestChange(newLimit, now);
        return PixLimitView.of(limit, now);
    }
}
