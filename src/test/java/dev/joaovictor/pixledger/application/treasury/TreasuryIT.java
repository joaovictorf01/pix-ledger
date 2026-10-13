package dev.joaovictor.pixledger.application.treasury;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.joaovictor.pixledger.application.IntegrationTestSupport;
import dev.joaovictor.pixledger.application.pix.SendPixCommand;
import dev.joaovictor.pixledger.application.pix.SendPixUseCase;
import dev.joaovictor.pixledger.application.treasury.CloseOfDayUseCase.ClosingReport;
import dev.joaovictor.pixledger.domain.exception.ClosingNotDueException;

class TreasuryIT extends IntegrationTestSupport {

    // Brasília is UTC-3
    private static final Instant NOON = Instant.parse("2026-10-20T15:00:00Z");
    private static final Instant CLOSING = Instant.parse("2026-10-20T21:00:00Z");
    private static final Instant TEN_PM = Instant.parse("2026-10-21T01:00:00Z");
    private static final Instant NEXT_MORNING = Instant.parse("2026-10-21T11:00:00Z");

    @Autowired
    private RebalanceInstantPaymentsUseCase rebalance;
    @Autowired
    private CloseOfDayUseCase closeOfDay;
    @Autowired
    private SendPixUseCase sendPix;
    @Autowired
    private GetInvariantsUseCase invariants;
    @Autowired
    private TreasuryProperties properties;

    @Test
    void shouldKeepCustomerMoneyCoveredThroughAWholeDay() {
        // given: noon, a customer receives money
        clock.setTo(NOON);
        UUID ana = newCustomerWithBalance(10_000_00);

        // when: the rebalance runs during the day
        rebalance.execute();

        // then: Instant Payments is at its target, funded by customer money only
        assertInstantPaymentsAtTarget();
        assertThat(treasury.position(NOON).reserveAdvance()).isZero();
        assertInvariantHolds();

        // when: the 18h closing runs
        clock.setTo(CLOSING);
        ClosingReport report = closeOfDay.execute();

        // then: requirement 2, and the Reserve funds the night
        assertThat(report.customerFundsMatch()).isTrue();
        assertThat(report.customerFunds()).isEqualTo(report.customerTotal());
        assertThat(treasury.position(CLOSING).reserveAdvance()).isPositive();
        assertInstantPaymentsAtTarget();
        assertInvariantHolds();

        // and: closing twice the same day changes nothing
        assertThat(closeOfDay.execute().alreadyClosed()).isTrue();

        // when: a Pix goes out at night and the rebalance runs again
        clock.setTo(TEN_PM);
        sendPix.execute(new SendPixCommand(ana, 1_000_00, newEndToEndId()));
        rebalance.execute();

        // then: still covered, with the Reserve
        assertInstantPaymentsAtTarget();
        assertInvariantHolds();

        // when: customer funds come back the next morning
        clock.setTo(NEXT_MORNING);
        rebalance.execute();

        // then: the Reserve was paid back first, and every check passes
        assertThat(treasury.position(NEXT_MORNING).reserveAdvance()).isZero();
        assertInstantPaymentsAtTarget();
        assertInvariantHolds();
        assertThat(invariants.execute().status()).isEqualTo("OK");
    }

    @Test
    void shouldRefuseToCloseTheDayBeforeClosingTime() {
        // given: 15h in Brasília, three hours before the closing
        clock.setTo(Instant.parse("2026-10-22T18:00:00Z"));

        // when / then: refused, and the day can still be closed at 18h
        assertThatThrownBy(() -> closeOfDay.execute()).isInstanceOf(ClosingNotDueException.class);
        clock.setTo(Instant.parse("2026-10-22T21:00:00Z"));
        assertThat(closeOfDay.execute().alreadyClosed()).isFalse();
    }

    /** Never below the target; above it by at most the tolerance. */
    private void assertInstantPaymentsAtTarget() {
        TreasuryPosition position = treasury.position(clock.instant());
        assertThat(position.instantPayments()).isGreaterThanOrEqualTo(position.instantPaymentsTarget());
        assertThat(position.instantPayments() - position.instantPaymentsTarget())
                .isLessThanOrEqualTo(properties.tolerance());
    }
}
