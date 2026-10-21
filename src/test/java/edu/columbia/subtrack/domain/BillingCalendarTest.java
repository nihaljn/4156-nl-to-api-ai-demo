package edu.columbia.subtrack.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import edu.columbia.subtrack.domain.BillingCalendar.Interval;
import edu.columbia.subtrack.domain.BillingCalendar.Status;
import edu.columbia.subtrack.support.TestClock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Plain JUnit tests for the date and status rules (no Spring). */
class BillingCalendarTest {

    private final TestClock clock = new TestClock();

    @Test
    void R2_billingDateKeepsMonthEndAnchor() {
        // AT-1: M0, subscription starting 2026-01-31
        LocalDate first = BillingCalendar.firstBillingOn(LocalDate.of(2026, 1, 31), 0);

        List<LocalDate> dates = BillingCalendar.billingDates(first, Interval.MONTH, null,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30));

        assertEquals(List.of(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)),
                dates);
    }

    @Test
    void R2_billingDateKeepsDay30AnchorAfterFebruary() {
        // AT-2: M0, subscription starting 2026-01-30
        LocalDate first = BillingCalendar.firstBillingOn(LocalDate.of(2026, 1, 30), 0);

        List<LocalDate> dates = BillingCalendar.billingDates(first, Interval.MONTH, null,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31));

        assertEquals(List.of(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 30)), dates);
    }

    @Test
    void R2_R4_yearlyPeriodStartFollowsLeapDayAnchor() {
        // AT-3: clock 2032-03-01; Y0, subscription starting 2028-02-29
        clock.setToday(LocalDate.of(2032, 3, 1));
        LocalDate start = LocalDate.of(2028, 2, 29);
        LocalDate first = BillingCalendar.firstBillingOn(start, 0);

        BillingCalendar.Period in2029 = BillingCalendar.currentPeriod(start, first, Interval.YEAR, null,
                LocalDate.of(2029, 3, 1));
        BillingCalendar.Period in2032 = BillingCalendar.currentPeriod(start, first, Interval.YEAR, null,
                LocalDate.now(clock));

        assertEquals(LocalDate.of(2029, 2, 28), in2029.start());
        assertEquals(LocalDate.of(2032, 2, 29), in2032.start());
    }

    @Test
    void R1_R5_R6_trialingUntilFirstBillingDayThenActive() {
        // AT-4: M7, subscription starting 2026-01-24
        LocalDate first = BillingCalendar.firstBillingOn(LocalDate.of(2026, 1, 24), 7);
        assertEquals(LocalDate.of(2026, 1, 31), first);

        LocalDate beforeBilling = LocalDate.of(2026, 1, 30);
        LocalDate onBilling = LocalDate.of(2026, 1, 31);

        assertEquals(Status.TRIALING, BillingCalendar.status(first, null, beforeBilling));
        assertEquals(Status.ACTIVE, BillingCalendar.status(first, null, onBilling));
        assertEquals(LocalDate.of(2026, 2, 28),
                BillingCalendar.nextBillingOn(first, Interval.MONTH, null, onBilling));
    }
}
