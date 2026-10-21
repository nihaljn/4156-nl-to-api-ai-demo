package edu.columbia.subtrack.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Billing dates, periods and status of one subscription (SPEC section 4, R1-R6). */
public final class BillingCalendar {

    private final LocalDate startDate;
    private final LocalDate firstBillingOn;
    private final Interval interval;
    private final LocalDate endsOn;

    public BillingCalendar(LocalDate startDate, LocalDate firstBillingOn, Interval interval, LocalDate endsOn) {
        this.startDate = startDate;
        this.firstBillingOn = firstBillingOn;
        this.interval = interval;
        this.endsOn = endsOn;
    }

    /** R1. */
    public static LocalDate firstBillingOn(LocalDate startDate, int trialDays) {
        return startDate.plusDays(trialDays);
    }

    /** R2: always computed from the first billing date, never from the previous one. */
    public LocalDate billingDate(int k) {
        return interval == Interval.MONTH ? firstBillingOn.plusMonths(k) : firstBillingOn.plusYears(k);
    }

    /** R3. */
    private boolean counts(LocalDate billingDate) {
        return endsOn == null || billingDate.isBefore(endsOn);
    }

    /** Counting billing dates from {@code from} to {@code to}, both included (R2, R3). */
    public List<LocalDate> billingDatesBetween(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (int k = 0; ; k++) {
            LocalDate date = billingDate(k);
            if (date.isAfter(to) || !counts(date)) {
                return dates;
            }
            if (!date.isBefore(from)) {
                dates.add(date);
            }
        }
    }

    /** R4: empty when the status is CANCELED. */
    public Optional<Period> currentPeriod(LocalDate day) {
        if (status(day) == Status.CANCELED) {
            return Optional.empty();
        }
        if (day.isBefore(firstBillingOn)) {
            return Optional.of(new Period(startDate, firstBillingOn));
        }
        int k = periodIndex(day);
        return Optional.of(new Period(billingDate(k), billingDate(k + 1)));
    }

    /** R5. */
    public Optional<LocalDate> nextBillingOn(LocalDate day) {
        int k = day.isBefore(firstBillingOn) ? 0 : periodIndex(day) + 1;
        LocalDate next = billingDate(k);
        return counts(next) ? Optional.of(next) : Optional.empty();
    }

    /** R6. */
    public Status status(LocalDate day) {
        if (endsOn != null && !day.isBefore(endsOn)) {
            return Status.CANCELED;
        }
        return day.isBefore(firstBillingOn) ? Status.TRIALING : Status.ACTIVE;
    }

    /** R6. */
    public boolean cancelAtPeriodEnd(LocalDate day) {
        return endsOn != null && day.isBefore(endsOn);
    }

    /** The largest k with billing(k) on or before {@code day}; {@code day} is not before the first billing date. */
    private int periodIndex(LocalDate day) {
        ChronoUnit unit = interval == Interval.MONTH ? ChronoUnit.MONTHS : ChronoUnit.YEARS;
        int k = (int) unit.between(firstBillingOn, day);
        while (billingDate(k).isAfter(day)) {
            k--;
        }
        while (!billingDate(k + 1).isAfter(day)) {
            k++;
        }
        return k;
    }
}
