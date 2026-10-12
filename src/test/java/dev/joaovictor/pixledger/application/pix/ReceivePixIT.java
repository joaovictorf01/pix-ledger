package dev.joaovictor.pixledger.application.pix;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.joaovictor.pixledger.application.IntegrationTestSupport;
import dev.joaovictor.pixledger.domain.SystemAccount;

class ReceivePixIT extends IntegrationTestSupport {

    @Test
    void shouldCreditCustomerAndInstantPaymentsAccount() {
        // given
        UUID ana = newCustomerWithBalance(0);
        long instantPaymentsBefore = balanceOf(SystemAccount.INSTANT_PAYMENTS);

        // when
        receivePix.execute(new ReceivePixCommand(ana, 100_00, newEndToEndId()));

        // then
        assertThat(balanceOf(ana)).isEqualTo(100_00);
        assertThat(balanceOf(SystemAccount.INSTANT_PAYMENTS)).isEqualTo(instantPaymentsBefore + 100_00);
        assertInvariantHolds();
    }

    @Test
    void shouldRecordSameIncomingPixOnlyOnce() {
        // given
        UUID bia = newCustomerWithBalance(0);
        String endToEndId = newEndToEndId();

        // when
        UUID first = receivePix.execute(new ReceivePixCommand(bia, 100_00, endToEndId));
        UUID redelivery = receivePix.execute(new ReceivePixCommand(bia, 100_00, endToEndId));

        // then
        assertThat(redelivery).isEqualTo(first);
        assertThat(balanceOf(bia)).isEqualTo(100_00);
        assertInvariantHolds();
    }
}
