package edu.columbia.subtrack.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Test clock fixed at 2026-03-05 (UTC) until a test moves it. */
public final class MutableClock extends Clock {

    public static final LocalDate START_DATE = LocalDate.of(2026, 3, 5);

    private final AtomicReference<Instant> now = new AtomicReference<>(startOf(START_DATE));

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public void advance(Duration duration) {
        now.updateAndGet(current -> current.plus(duration));
    }

    public void setDate(LocalDate date) {
        now.set(startOf(date));
    }

    public void reset() {
        setDate(START_DATE);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
