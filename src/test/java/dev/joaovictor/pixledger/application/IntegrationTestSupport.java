package dev.joaovictor.pixledger.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import dev.joaovictor.pixledger.MutableClock;
import dev.joaovictor.pixledger.TestcontainersConfiguration;
import dev.joaovictor.pixledger.application.account.OpenCustomerAccountUseCase;
import dev.joaovictor.pixledger.application.account.SystemAccounts;
import dev.joaovictor.pixledger.application.pix.ReceivePixCommand;
import dev.joaovictor.pixledger.application.pix.ReceivePixUseCase;
import dev.joaovictor.pixledger.application.treasury.Treasury;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;

/**
 * Base for tests against a real Postgres (Testcontainers). Every test opens its
 * own accounts, so tests never depend on each other's data. Scheduled jobs are
 * off and the clock can be moved, so each test decides what time it is.
 */
@SpringBootTest(properties = "pixledger.scheduling.enabled=false")
@Import({ TestcontainersConfiguration.class, IntegrationTestSupport.TestClockConfig.class })
public abstract class IntegrationTestSupport {

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    @Autowired
    protected MutableClock clock;
    @Autowired
    protected OpenCustomerAccountUseCase openAccount;
    @Autowired
    protected ReceivePixUseCase receivePix;
    @Autowired
    protected LedgerEntryRepository entries;
    @Autowired
    protected SystemAccounts systemAccounts;
    @Autowired
    protected Treasury treasury;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    protected UUID newCustomerWithBalance(long balance) {
        UUID account = openAccount.execute("customer-" + UUID.randomUUID()).getId();
        if (balance > 0) {
            receivePix.execute(new ReceivePixCommand(account, balance, newEndToEndId()));
        }
        return account;
    }

    protected long balanceOf(UUID account) {
        return entries.balanceOf(account);
    }

    protected long balanceOf(SystemAccount account) {
        return entries.balanceOf(systemAccounts.idOf(account));
    }

    /**
     * Customer Funds + Instant Payments - Reserve advance = sum of all customer
     * balances. Without an advance this is exactly the challenge's invariant.
     */
    protected void assertInvariantHolds() {
        assertThat(treasury.position(clock.instant()).customerMoneyDifference()).isZero();
    }

    protected static String newEndToEndId() {
        return "E" + UUID.randomUUID().toString().replace("-", "");
    }
}
