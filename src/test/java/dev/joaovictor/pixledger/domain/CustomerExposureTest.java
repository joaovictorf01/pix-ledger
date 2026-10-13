package dev.joaovictor.pixledger.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CustomerExposureTest {

    @Test
    void shouldBeLimitedByWhatIsLeftOfTheLimit() {
        assertThat(new CustomerExposure(70_00, 50_00, 0).maxSendable()).isEqualTo(50_00);
        assertThat(new CustomerExposure(90_00, 50_00, 40_00).maxSendable()).isEqualTo(10_00);
    }

    @Test
    void shouldBeLimitedByBalance() {
        assertThat(new CustomerExposure(20_00, 50_00, 0).maxSendable()).isEqualTo(20_00);
    }

    @Test
    void shouldNeverBeNegative() {
        // limit lowered after the customer had already sent more than the new limit
        assertThat(new CustomerExposure(100_00, 50_00, 80_00).maxSendable()).isZero();
        assertThat(new CustomerExposure(0, 50_00, 0).maxSendable()).isZero();
    }

    @Test
    void shouldAddUpEveryCustomerIntoTheInstantPaymentsTarget() {
        // the three friends from the study notes: Ana 50, Bia 20, Caio 10
        long target = CustomerExposure.instantPaymentsTarget(List.of(
                new CustomerExposure(70_00, 50_00, 0),
                new CustomerExposure(20_00, 50_00, 0),
                new CustomerExposure(90_00, 50_00, 40_00)));

        assertThat(target).isEqualTo(80_00);
    }
}
