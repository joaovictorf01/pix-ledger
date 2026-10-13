package dev.joaovictor.pixledger.application.treasury;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.joaovictor.pixledger.domain.Ledger;

/**
 * The internal dashboard in text form: balances, what they should be, and
 * whether each rule of the challenge holds right now.
 */
@Service
public class GetInvariantsUseCase {

    private final Treasury treasury;
    private final Clock clock;

    public GetInvariantsUseCase(Treasury treasury, Clock clock) {
        this.treasury = treasury;
        this.clock = clock;
    }

    public record Check(String name, boolean ok, String detail) {
    }

    /** @param status OK when every check passes, ATTENTION otherwise */
    public record InvariantsReport(Instant checkedAt, String status, TreasuryPosition position,
            List<Check> checks) {
    }

    @Transactional(readOnly = true)
    public InvariantsReport execute() {
        Instant now = clock.instant();
        TreasuryPosition position = treasury.position(now);
        long customerLedger = treasury.sumOfLedger(Ledger.CUSTOMER);
        long internalLedger = treasury.sumOfLedger(Ledger.INTERNAL);

        List<Check> checks = List.of(
                new Check("customer-ledger-balanced", customerLedger == 0,
                        "sum of all customer ledger entries is " + customerLedger + ", must be 0"),
                new Check("internal-ledger-balanced", internalLedger == 0,
                        "sum of all internal ledger entries is " + internalLedger + ", must be 0"),
                new Check("customer-money-covered", position.customerMoneyDifference() == 0,
                        "customer funds + instant payments - reserve advance - customer total = "
                                + position.customerMoneyDifference()
                                + "; a short non-zero value means a Pix is still reaching the internal ledger"),
                new Check("instant-payments-covers-target",
                        position.instantPayments() >= position.instantPaymentsTarget(),
                        "instant payments has " + position.instantPayments() + ", target is "
                                + position.instantPaymentsTarget()),
                new Check("reserve-advance-not-negative", position.reserveAdvance() >= 0,
                        "reserve has lent " + position.reserveAdvance()));

        boolean allOk = checks.stream().allMatch(Check::ok);
        return new InvariantsReport(now, allOk ? "OK" : "ATTENTION", position, checks);
    }
}
