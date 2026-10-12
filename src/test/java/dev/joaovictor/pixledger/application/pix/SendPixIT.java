package dev.joaovictor.pixledger.application.pix;

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

import dev.joaovictor.pixledger.application.IntegrationTestSupport;
import dev.joaovictor.pixledger.domain.SystemAccount;
import dev.joaovictor.pixledger.domain.exception.InsufficientFundsException;

class SendPixIT extends IntegrationTestSupport {

    @Autowired
    private SendPixUseCase sendPix;

    @Test
    void shouldDebitCustomerAndInstantPaymentsAccount() {
        // given
        UUID ana = newCustomerWithBalance(100_00);
        long instantPaymentsBefore = balanceOf(SystemAccount.INSTANT_PAYMENTS);

        // when
        sendPix.execute(new SendPixCommand(ana, 30_00, newEndToEndId()));

        // then
        assertThat(balanceOf(ana)).isEqualTo(70_00);
        assertThat(balanceOf(SystemAccount.INSTANT_PAYMENTS)).isEqualTo(instantPaymentsBefore - 30_00);
        assertInvariantHolds();
    }

    @Test
    void shouldRejectPixAboveBalanceAndRecordNothing() {
        // given
        UUID bia = newCustomerWithBalance(50_00);

        // when / then
        assertThatThrownBy(() -> sendPix.execute(new SendPixCommand(bia, 80_00, newEndToEndId())))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(balanceOf(bia)).isEqualTo(50_00);
        assertInvariantHolds();
    }

    @Test
    void shouldRecordSameEndToEndIdOnlyOnce() {
        // given
        UUID caio = newCustomerWithBalance(100_00);
        String endToEndId = newEndToEndId();

        // when
        UUID first = sendPix.execute(new SendPixCommand(caio, 30_00, endToEndId));
        UUID retry = sendPix.execute(new SendPixCommand(caio, 30_00, endToEndId));

        // then
        assertThat(retry).isEqualTo(first);
        assertThat(balanceOf(caio)).isEqualTo(70_00);
        assertInvariantHolds();
    }

    @Test
    void shouldNotLetTwoSimultaneousPixSpendTheSameMoney() throws Exception {
        // given
        UUID ana = newCustomerWithBalance(100_00);
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

        // when
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

        // then
        assertThat(succeeded).isEqualTo(1);
        assertThat(rejected).isEqualTo(1);
        assertThat(balanceOf(ana)).isEqualTo(20_00);
        assertInvariantHolds();
    }
}
