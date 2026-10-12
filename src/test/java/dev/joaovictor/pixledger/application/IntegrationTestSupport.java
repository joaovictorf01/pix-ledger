package dev.joaovictor.pixledger.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import dev.joaovictor.pixledger.TestcontainersConfiguration;
import dev.joaovictor.pixledger.application.account.OpenCustomerAccountUseCase;
import dev.joaovictor.pixledger.application.account.SystemAccounts;
import dev.joaovictor.pixledger.application.pix.ReceivePixCommand;
import dev.joaovictor.pixledger.application.pix.ReceivePixUseCase;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;

/**
 * Base for tests against a real Postgres (Testcontainers). Every test opens its
 * own accounts, so tests never depend on each other's data.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    @Autowired
    protected OpenCustomerAccountUseCase openAccount;
    @Autowired
    protected ReceivePixUseCase receivePix;
    @Autowired
    protected LedgerEntryRepository entries;
    @Autowired
    protected SystemAccounts systemAccounts;

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
     * Sum of all customer balances = Customer Funds + Instant Payments.
     * Customer balances add up to minus the customer WORLD account, because
     * every customer transaction balances against it.
     */
    protected void assertInvariantHolds() {
        long customerBalances = -balanceOf(SystemAccount.CUSTOMER_WORLD);

        assertThat(balanceOf(SystemAccount.CUSTOMER_FUNDS) + balanceOf(SystemAccount.INSTANT_PAYMENTS))
                .isEqualTo(customerBalances);
    }

    protected static String newEndToEndId() {
        return "E" + UUID.randomUUID().toString().replace("-", "");
    }
}
