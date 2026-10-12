package dev.joaovictor.pixledger.application.limit;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.persistence.PixLimitRepository;

@Service
public class GetPixLimitUseCase {

    private final PixLimitRepository limits;
    private final Clock clock;

    public GetPixLimitUseCase(PixLimitRepository limits, Clock clock) {
        this.limits = limits;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PixLimitView execute(UUID customerAccountId) {
        return limits.findById(customerAccountId)
                .map(limit -> PixLimitView.of(limit, clock.instant()))
                .orElseThrow(() -> new AccountNotFoundException(customerAccountId.toString()));
    }
}
