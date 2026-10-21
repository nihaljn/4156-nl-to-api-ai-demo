package edu.columbia.subtrack.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Billing dates, periods and status of one subscription (SPEC.md section 4). */
public final class BillingCalendar {

    private final LocalDate startDate;
    private final LocalDate firstBilling;
    private final Interval interval;
    private final LocalDate endsOn;

    /**
     * Creates a calendar.
     *
     * @param startDate the subscription's start date
     * @param trialDays the plan's trial length
     * @param interval the plan's billing interval
     * @param endsOn the day the subscription ends, or null
     */
    public BillingCalendar(final LocalDate startDate, final int trialDays, final Interval interval,
            final LocalDate endsOn) {
        this.startDate = startDate;
        this.firstBilling = startDate.plusDays(trialDays);
        this.interval = interval;
        this.endsOn = endsOn;
    }

    /** How often a plan bills. */
    public enum Interval {
        MONTH, YEAR
    }

    /** Where a subscription stands on a given day. */
    public enum Status {
        TRIALING, ACTIVE, CANCELED
    }

    /**
     * A billing period.
     *
     * @param start the first day
     * @param end the day after the last day
     */
    public record Period(LocalDate start, LocalDate end) {
    }

    /**
     * R1: the first billing date.
     *
     * @return start date plus trial days
     */
    public LocalDate firstBillingOn() {
        return firstBilling;
    }

    /**
     * R2, R3: every counting billing date in a window.
     *
     * @param from the first day of the window
     * @param to the last day of the window
     * @return the dates, earliest first, both ends included
     */
    public List<LocalDate> billingDates(final LocalDate from, final LocalDate to) {
        final List<LocalDate> dates = new ArrayList<>();
        for (long k = 0;; k++) {
            final LocalDate date = billing(k);
            if (date.isAfter(to) || !counts(date)) {
                return dates;
            }
            if (!date.isBefore(from)) {
                dates.add(date);
            }
        }
    }

    /**
     * R6: the status on a day.
     *
     * @param day the day to evaluate
     * @return CANCELED, TRIALING or ACTIVE
     */
    public Status status(final LocalDate day) {
        if (endsOn != null && !day.isBefore(endsOn)) {
            return Status.CANCELED;
        }
        return day.isBefore(firstBilling) ? Status.TRIALING : Status.ACTIVE;
    }

    /**
     * R6: whether a cancel is scheduled but has not taken effect.
     *
     * @param day the day to evaluate
     * @return true if endsOn is set and the day is before it
     */
    public boolean cancelAtPeriodEnd(final LocalDate day) {
        return endsOn != null && day.isBefore(endsOn);
    }

    /**
     * R4: the current period.
     *
     * @param day the day to evaluate
     * @return the period, or null when the status is CANCELED
     */
    public Period currentPeriod(final LocalDate day) {
        if (status(day) == Status.CANCELED) {
            return null;
        }
        if (day.isBefore(firstBilling)) {
            return new Period(startDate, firstBilling);
        }
        final long k = periodIndex(day);
        return new Period(billing(k), billing(k + 1));
    }

    /**
     * R5: the next billing date.
     *
     * @param day the day to evaluate
     * @return the earliest counting billing date after the day, or null
     */
    public LocalDate nextBillingOn(final LocalDate day) {
        final long k = day.isBefore(firstBilling) ? 0 : periodIndex(day) + 1;
        final LocalDate next = billing(k);
        return counts(next) ? next : null;
    }

    private LocalDate billing(final long k) {
        return interval == Interval.MONTH ? firstBilling.plusMonths(k) : firstBilling.plusYears(k);
    }

    private boolean counts(final LocalDate billingDate) {
        return endsOn == null || billingDate.isBefore(endsOn);
    }

    /** The largest k with billing(k) on or before the day; the day must not precede firstBillingOn. */
    private long periodIndex(final LocalDate day) {
        final ChronoUnit unit = interval == Interval.MONTH ? ChronoUnit.MONTHS : ChronoUnit.YEARS;
        long k = unit.between(firstBilling, day);
        while (!billing(k + 1).isAfter(day)) {
            k++;
        }
        while (billing(k).isAfter(day)) {
            k--;
        }
        return k;
    }
}
