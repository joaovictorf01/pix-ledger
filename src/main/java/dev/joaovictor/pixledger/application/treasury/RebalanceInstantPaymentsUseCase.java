package dev.joaovictor.pixledger.application.treasury;

import static dev.joaovictor.pixledger.domain.SystemAccount.CUSTOMER_FUNDS;
import static dev.joaovictor.pixledger.domain.SystemAccount.FINANCIAL_RESERVE;
import static dev.joaovictor.pixledger.domain.SystemAccount.INSTANT_PAYMENTS;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.SystemAccount;

/**
 * Keeps Instant Payments at or above its target: the most customers could
 * legitimately send right now. A shortfall is always covered; the tolerance
 * only avoids giving back small surpluses.
 *
 * During the day it uses Customer Funds, and first pays back anything the
 * Reserve lent overnight. After the closing, Customer Funds are in custody,
 * so the Reserve lends what is missing.
 */
@Service
public class RebalanceInstantPaymentsUseCase {

    private final Treasury treasury;
    private final TreasuryProperties properties;
    private final Clock clock;

    public RebalanceInstantPaymentsUseCase(Treasury treasury, TreasuryProperties properties, Clock clock) {
        this.treasury = treasury;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public List<Movement> execute() {
        treasury.lock();
        Instant now = clock.instant();
        boolean customerFundsAvailable = properties.operatingHours().customerFundsAvailable(now);
        return rebalance(now, customerFundsAvailable);
    }

    /** Also used right after the closing, with Customer Funds unavailable. Caller holds the lock. */
    List<Movement> rebalance(Instant now, boolean customerFundsAvailable) {
        TreasuryPosition position = treasury.position(now);
        String run = "rebalance:" + UUID.randomUUID() + ":";
        List<Movement> movements = new ArrayList<>();

        long gap = position.instantPaymentsTarget() - position.instantPayments();

        if (customerFundsAvailable) {
            // 1. Pay the Reserve back before anything else.
            long repay = Math.max(0, Math.min(position.reserveAdvance(), position.customerFunds()));
            move(CUSTOMER_FUNDS, FINANCIAL_RESERVE, repay, run + "repay", now, movements);
            long customerFundsLeft = position.customerFunds() - repay;

            // 2. Any shortfall is topped up; a surplus goes back only beyond the tolerance.
            if (gap > 0) {
                move(CUSTOMER_FUNDS, INSTANT_PAYMENTS, Math.min(gap, customerFundsLeft), run + "top-up", now,
                        movements);
            } else if (gap < -properties.tolerance()) {
                move(INSTANT_PAYMENTS, CUSTOMER_FUNDS, -gap, run + "give-back", now, movements);
            }
        } else {
            if (gap > 0) {
                // The Reserve lends: the advance is what the Reserve is missing from its capital.
                move(FINANCIAL_RESERVE, INSTANT_PAYMENTS, Math.min(gap, position.financialReserve()),
                        run + "advance", now, movements);
            } else if (gap < -properties.tolerance()) {
                // Pix received at night can pay part of the advance back.
                move(INSTANT_PAYMENTS, FINANCIAL_RESERVE, Math.min(-gap, position.reserveAdvance()),
                        run + "repay", now, movements);
            }
        }
        return movements;
    }

    private void move(SystemAccount from, SystemAccount to, long amount, String key, Instant now,
            List<Movement> movements) {
        if (amount > 0) {
            treasury.move(from, to, amount, key, now);
            movements.add(new Movement(from, to, amount));
        }
    }
}
