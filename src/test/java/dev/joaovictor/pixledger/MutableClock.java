package dev.joaovictor.pixledger;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** A clock tests can move. Follows the real time until {@link #setTo} is called. */
public class MutableClock extends Clock {

    private final AtomicReference<Instant> fixed = new AtomicReference<>();

    public void setTo(Instant instant) {
        fixed.set(instant);
    }

    public void reset() {
        fixed.set(null);
    }

    @Override
    public Instant instant() {
        Instant instant = fixed.get();
        return instant != null ? instant : Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
