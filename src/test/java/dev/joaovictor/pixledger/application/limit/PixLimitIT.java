package dev.joaovictor.pixledger.application.limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.joaovictor.pixledger.application.IntegrationTestSupport;
import dev.joaovictor.pixledger.application.pix.SendPixCommand;
import dev.joaovictor.pixledger.application.pix.SendPixUseCase;
import dev.joaovictor.pixledger.domain.exception.PixLimitExceededException;

class PixLimitIT extends IntegrationTestSupport {

    @Autowired
    private SendPixUseCase sendPix;
    @Autowired
    private ChangePixLimitUseCase changePixLimit;

    @Test
    void shouldRejectPixAboveDailyLimitAfterImmediateDecrease() {
        // given
        UUID dani = newCustomerWithBalance(10_000_00);
        changePixLimit.execute(dani, 1_000_00);
        sendPix.execute(new SendPixCommand(dani, 700_00, newEndToEndId()));

        // when / then
        assertThatThrownBy(() -> sendPix.execute(new SendPixCommand(dani, 400_00, newEndToEndId())))
                .isInstanceOf(PixLimitExceededException.class);
        assertThat(balanceOf(dani)).isEqualTo(9_300_00);
        assertInvariantHolds();
    }

    @Test
    void shouldKeepOldLimitWhileIncreaseIsPending() {
        // given: default limit is R$ 5.000,00
        UUID edu = newCustomerWithBalance(20_000_00);

        // when
        PixLimitView view = changePixLimit.execute(edu, 10_000_00);

        // then
        assertThat(view.status()).isEqualTo("PENDING_INCREASE");
        assertThatThrownBy(() -> sendPix.execute(new SendPixCommand(edu, 6_000_00, newEndToEndId())))
                .isInstanceOf(PixLimitExceededException.class);
    }
}
