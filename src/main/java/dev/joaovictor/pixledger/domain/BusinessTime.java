package dev.joaovictor.pixledger.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Business rules use Brasília time: the day, the 18h closing, the night period. */
public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private BusinessTime() {
    }

    public static Instant startOfDay(Instant moment) {
        LocalDate day = moment.atZone(ZONE).toLocalDate();
        return day.atStartOfDay(ZONE).toInstant();
    }
}
