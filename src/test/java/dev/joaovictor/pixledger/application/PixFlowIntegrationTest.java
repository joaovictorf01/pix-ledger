package dev.joaovictor.pixledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import dev.joaovictor.pixledger.TestcontainersConfiguration;
import dev.joaovictor.pixledger.application.account.OpenCustomerAccountUseCase;
import dev.joaovictor.pixledger.application.account.SystemAccounts;
import dev.joaovictor.pixledger.application.pix.ReceivePixCommand;
import dev.joaovictor.pixledger.application.pix.ReceivePixUseCase;
import dev.joaovictor.pixledger.application.pix.SendPixCommand;
import dev.joaovictor.pixledger.application.pix.SendPixUseCase;
import dev.joaovictor.pixledger.domain.InsufficientFundsException;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.persistence.LedgerEntryRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PixFlowIntegrationTest {

    @Autowired
    private OpenCustomerAccountUseCase openAccount;
    @Autowired
    private ReceivePixUseCase receivePix;
    @Autowired
    private SendPixUseCase sendPix;
    @Autowired
    private LedgerEntryRepository entries;
    @Autowired
    private SystemAccounts systemAccounts;

    @Test
    void receiveThenSendUpdatesBothLedgers() {
        UUID ana = openAccount.execute("ana-" + UUID.randomUUID()).getId();

        receivePix.execute(new ReceivePixCommand(ana, 100_00, newEndToEndId()));
        sendPix.execute(new SendPixCommand(ana, 30_00, newEndToEndId()));

        assertThat(entries.balanceOf(ana)).isEqualTo(70_00);
        assertInvariantHolds();
    }

    @Test
    void rejectsPixAboveBalanceAndRecordsNothing() {
        UUID bia = openAccount.execute("bia-" + UUID.randomUUID()).getId();
        receivePix.execute(new ReceivePixCommand(bia, 50_00, newEndToEndId()));

        assertThatThrownBy(() -> sendPix.execute(new SendPixCommand(bia, 80_00, newEndToEndId())))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(entries.balanceOf(bia)).isEqualTo(50_00);
        assertInvariantHolds();
    }

    @Test
    void sameEndToEndIdIsRecordedOnce() {
        UUID caio = openAccount.execute("caio-" + UUID.randomUUID()).getId();
        receivePix.execute(new ReceivePixCommand(caio, 100_00, newEndToEndId()));

        String endToEndId = newEndToEndId();
        UUID first = sendPix.execute(new SendPixCommand(caio, 30_00, endToEndId));
        UUID retry = sendPix.execute(new SendPixCommand(caio, 30_00, endToEndId));

        assertThat(retry).isEqualTo(first);
        assertThat(entries.balanceOf(caio)).isEqualTo(70_00);
        assertInvariantHolds();
    }

    @Test
    void twoSimultaneousPixCannotSpendTheSameMoney() throws Exception {
        UUID ana = openAccount.execute("ana-" + UUID.randomUUID()).getId();
        receivePix.execute(new ReceivePixCommand(ana, 100_00, newEndToEndId()));

        int attempts = 2;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Future<UUID>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            Callable<UUID> pix = () -> {
                start.await();
                return sendPix.execute(new SendPixCommand(ana, 80_00, newEndToEndId()));
            };
            results.add(pool.submit(pix));
        }
        start.countDown();

        int succeeded = 0;
        int rejected = 0;
        for (Future<UUID> result : results) {
            try {
                result.get();
                succeeded++;
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(InsufficientFundsException.class);
                rejected++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(1);
        assertThat(rejected).isEqualTo(1);
        assertThat(entries.balanceOf(ana)).isEqualTo(20_00);
        assertInvariantHolds();
    }

    /**
     * Sum of all customer balances = Customer Funds + Instant Payments.
     * Customer balances add up to minus the customer WORLD account, because
     * every customer transaction balances against it.
     */
    private void assertInvariantHolds() {
        long customerBalances = -entries.balanceOf(systemAccounts.idOf(SystemAccount.CUSTOMER_WORLD));
        long customerFunds = entries.balanceOf(systemAccounts.idOf(SystemAccount.CUSTOMER_FUNDS));
        long instantPayments = entries.balanceOf(systemAccounts.idOf(SystemAccount.INSTANT_PAYMENTS));

        assertThat(customerFunds + instantPayments).isEqualTo(customerBalances);
    }

    private static String newEndToEndId() {
        return "E" + UUID.randomUUID().toString().replace("-", "");
    }
}
