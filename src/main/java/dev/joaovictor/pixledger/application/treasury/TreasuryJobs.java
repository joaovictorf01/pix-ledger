package dev.joaovictor.pixledger.application.treasury;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the treasury on its own. Turned off with pixledger.scheduling.enabled=false,
 * so tests decide when each job runs. Both jobs can also be triggered by hand
 * through the internal endpoints.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "pixledger.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class TreasuryJobs {

    private static final Logger log = LoggerFactory.getLogger(TreasuryJobs.class);

    private final RebalanceInstantPaymentsUseCase rebalance;
    private final CloseOfDayUseCase closeOfDay;

    public TreasuryJobs(RebalanceInstantPaymentsUseCase rebalance, CloseOfDayUseCase closeOfDay) {
        this.rebalance = rebalance;
        this.closeOfDay = closeOfDay;
    }

    @Scheduled(fixedDelayString = "${pixledger.treasury.rebalance-interval}")
    void rebalance() {
        var movements = rebalance.execute();
        if (!movements.isEmpty()) {
            log.info("Rebalance moved {}", movements);
        }
    }

    // Must match pixledger.treasury.closing-time
    @Scheduled(cron = "${pixledger.treasury.closing-cron}", zone = "America/Sao_Paulo")
    void closeOfDay() {
        var report = closeOfDay.execute();
        if (report.customerFundsMatch()) {
            log.info("Closing {}: customer funds match customer total {}", report.businessDay(),
                    report.customerTotal());
        } else {
            log.error("Closing {}: customer funds {} do not match customer total {}", report.businessDay(),
                    report.customerFunds(), report.customerTotal());
        }
    }
}
