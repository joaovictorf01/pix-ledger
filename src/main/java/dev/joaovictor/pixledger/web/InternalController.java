package dev.joaovictor.pixledger.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.joaovictor.pixledger.application.treasury.CloseOfDayUseCase;
import dev.joaovictor.pixledger.application.treasury.CloseOfDayUseCase.ClosingReport;
import dev.joaovictor.pixledger.application.treasury.GetInvariantsUseCase;
import dev.joaovictor.pixledger.application.treasury.GetInvariantsUseCase.InvariantsReport;
import dev.joaovictor.pixledger.application.treasury.Movement;
import dev.joaovictor.pixledger.application.treasury.RebalanceInstantPaymentsUseCase;

/**
 * Operations for the internal team. In production these would sit behind
 * authentication and on a separate port; here they are open for the demo.
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final GetInvariantsUseCase invariants;
    private final RebalanceInstantPaymentsUseCase rebalance;
    private final CloseOfDayUseCase closeOfDay;

    public InternalController(GetInvariantsUseCase invariants, RebalanceInstantPaymentsUseCase rebalance,
            CloseOfDayUseCase closeOfDay) {
        this.invariants = invariants;
        this.rebalance = rebalance;
        this.closeOfDay = closeOfDay;
    }

    @GetMapping("/invariants")
    public InvariantsReport invariants() {
        return invariants.execute();
    }

    @PostMapping("/rebalance")
    public List<Movement> rebalance() {
        return rebalance.execute();
    }

    @PostMapping("/close-of-day")
    public ClosingReport closeOfDay() {
        return closeOfDay.execute();
    }
}
