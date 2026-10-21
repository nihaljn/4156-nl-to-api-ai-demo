package edu.columbia.subtrack.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A UTC clock fixed at 2026-03-05 that tests can move to another day. */
public class TestClock extends Clock {

    public static final LocalDate DEFAULT_TODAY = LocalDate.of(2026, 3, 5);

    private volatile Instant now = DEFAULT_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant();

    public void setToday(LocalDate day) {
        now = day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
