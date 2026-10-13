package dev.joaovictor.pixledger.application.treasury;

import java.time.Clock;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.SystemAccount;

/**
 * Puts the institution's own money in the Financial Reserve on first start.
 * It comes from outside (WORLD), like a capital contribution. The key makes it
 * happen once, however many times the application starts.
 */
@Component
public class ReserveCapitalInitializer implements ApplicationRunner {

    private final Treasury treasury;
    private final TreasuryProperties properties;
    private final Clock clock;

    public ReserveCapitalInitializer(Treasury treasury, TreasuryProperties properties, Clock clock) {
        this.treasury = treasury;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        treasury.move(SystemAccount.INTERNAL_WORLD, SystemAccount.FINANCIAL_RESERVE, properties.reserveCapital(),
                Treasury.RESERVE_CAPITAL_KEY_PREFIX + "initial", clock.instant());
    }
}
