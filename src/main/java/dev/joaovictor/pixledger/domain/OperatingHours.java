package dev.joaovictor.pixledger.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Customer Funds are sent to safe custody at the closing time and come back
 * at fundsAvailableFrom the next morning. In between they cannot be used, so
 * the Financial Reserve covers the Instant Payments account.
 */
public record OperatingHours(LocalTime fundsAvailableFrom, LocalTime closingTime) {

    public OperatingHours {
        Objects.requireNonNull(fundsAvailableFrom, "fundsAvailableFrom must not be null");
        Objects.requireNonNull(closingTime, "closingTime must not be null");
        if (!fundsAvailableFrom.isBefore(closingTime)) {
            throw new IllegalArgumentException("fundsAvailableFrom must be before closingTime");
        }
    }

    /** True between fundsAvailableFrom (inclusive) and closingTime (exclusive), Brasília time. */
    public boolean customerFundsAvailable(Instant moment) {
        LocalTime time = moment.atZone(BusinessTime.ZONE).toLocalTime();
        return !time.isBefore(fundsAvailableFrom) && time.isBefore(closingTime);
    }
}
