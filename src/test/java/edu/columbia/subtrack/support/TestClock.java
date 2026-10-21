package edu.columbia.subtrack.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A UTC clock fixed at a date that tests can move. */
public final class TestClock extends Clock {

    /** The date every test starts on. */
    public static final LocalDate START = LocalDate.of(2026, 3, 5);

    private Instant now = START.atStartOfDay(ZoneOffset.UTC).toInstant();

    /**
     * Moves the clock to the start of the given day.
     *
     * @param date the new current date
     */
    public synchronized void setDate(final LocalDate date) {
        now = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(final ZoneId zone) {
        return this;
    }

    @Override
    public synchronized Instant instant() {
        return now;
    }
}
