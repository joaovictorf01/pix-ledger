package dev.joaovictor.pixledger.application.treasury;

import static dev.joaovictor.pixledger.domain.SystemAccount.CUSTOMER_FUNDS;
import static dev.joaovictor.pixledger.domain.SystemAccount.FINANCIAL_RESERVE;
import static dev.joaovictor.pixledger.domain.SystemAccount.INSTANT_PAYMENTS;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.BusinessTime;
import dev.joaovictor.pixledger.domain.Closing;
import dev.joaovictor.pixledger.persistence.ClosingRepository;

/**
 * The 18h closing. Instant Payments is emptied so that Customer Funds holds
 * exactly the sum of all customer balances; then the Reserve lends Instant
 * Payments what it needs for the night.
 */
@Service
public class CloseOfDayUseCase {

    private final Treasury treasury;
    private final RebalanceInstantPaymentsUseCase rebalance;
    private final ClosingRepository closings;
    private final Clock clock;

    public CloseOfDayUseCase(Treasury treasury, RebalanceInstantPaymentsUseCase rebalance,
            ClosingRepository closings, Clock clock) {
        this.treasury = treasury;
        this.rebalance = rebalance;
        this.closings = closings;
        this.clock = clock;
    }

    /**
     * @param customerFundsMatch true when Customer Funds equalled the customer total
     *                           right after the sweep, which is what the challenge asks at 18h
     */
    public record ClosingReport(LocalDate businessDay, boolean alreadyClosed, long customerTotal,
            long customerFunds, boolean customerFundsMatch, List<Movement> movements) {

        static ClosingReport of(Closing closing, boolean alreadyClosed, List<Movement> movements) {
            return new ClosingReport(closing.getBusinessDay(), alreadyClosed, closing.getCustomerTotal(),
                    closing.getCustomerFunds(), closing.isCustomerFundsMatch(), movements);
        }
    }

    @Transactional
    public ClosingReport execute() {
        treasury.lock();
        Instant now = clock.instant();
        LocalDate businessDay = now.atZone(BusinessTime.ZONE).toLocalDate();

        var existing = closings.findById(businessDay);
        if (existing.isPresent()) {
            return ClosingReport.of(existing.get(), true, List.of());
        }

        String key = "close:" + businessDay + ":";
        List<Movement> movements = new ArrayList<>();
        TreasuryPosition before = treasury.position(now);

        // 1. Whatever the Reserve still has in Instant Payments goes back to it.
        long repay = Math.max(0, Math.min(before.reserveAdvance(), before.instantPayments()));
        if (repay > 0) {
            treasury.move(INSTANT_PAYMENTS, FINANCIAL_RESERVE, repay, key + "repay", now);
            movements.add(new Movement(INSTANT_PAYMENTS, FINANCIAL_RESERVE, repay));
        }

        // 2. The rest is customer money: sweep it into Customer Funds.
        long sweep = before.instantPayments() - repay;
        if (sweep > 0) {
            treasury.move(INSTANT_PAYMENTS, CUSTOMER_FUNDS, sweep, key + "sweep", now);
            movements.add(new Movement(INSTANT_PAYMENTS, CUSTOMER_FUNDS, sweep));
        }

        // 3. Requirement 2 is measured here, with Instant Payments empty, and kept as a record.
        TreasuryPosition swept = treasury.position(now);
        Closing closing = closings.save(new Closing(businessDay, now, swept.customerTotal(),
                swept.customerFunds()));

        // 4. Customer Funds are now in custody: the Reserve funds the night.
        movements.addAll(rebalance.rebalance(now, false));

        return ClosingReport.of(closing, false, movements);
    }
}
