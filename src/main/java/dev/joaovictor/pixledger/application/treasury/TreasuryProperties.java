package dev.joaovictor.pixledger.application.treasury;

import java.time.LocalTime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import dev.joaovictor.pixledger.domain.OperatingHours;

/**
 * @param fundsAvailableFrom when Customer Funds come back from custody (Brasília time)
 * @param closingTime        when Customer Funds go to custody (Brasília time)
 * @param tolerance          differences up to this amount, in cents, are left alone
 * @param reserveCapital     money the institution puts in the Financial Reserve, in cents
 */
@ConfigurationProperties(prefix = "pixledger.treasury")
public record TreasuryProperties(LocalTime fundsAvailableFrom, LocalTime closingTime, long tolerance,
        long reserveCapital) {

    public OperatingHours operatingHours() {
        return new OperatingHours(fundsAvailableFrom, closingTime);
    }
}
