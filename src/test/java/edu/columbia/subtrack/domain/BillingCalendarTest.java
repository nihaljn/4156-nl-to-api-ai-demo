package edu.columbia.subtrack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.domain.BillingCalendar.Interval;
import edu.columbia.subtrack.domain.BillingCalendar.Status;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BillingCalendarTest {

    private static LocalDate date(final String iso) {
        return LocalDate.parse(iso);
    }

    // AT-1
    @Test
    void R2_billingDateKeepsMonthEndAnchor() {
        final BillingCalendar calendar = new BillingCalendar(date("2026-01-31"), 0, Interval.MONTH, null);

        assertThat(calendar.billingDates(date("2026-02-01"), date("2026-04-30")))
            .containsExactly(date("2026-02-28"), date("2026-03-31"), date("2026-04-30"));
    }

    // AT-2
    @Test
    void R2_billingDateKeepsDay30AnchorAfterFebruary() {
        final BillingCalendar calendar = new BillingCalendar(date("2026-01-30"), 0, Interval.MONTH, null);

        final List<LocalDate> dates = calendar.billingDates(date("2026-02-01"), date("2026-03-31"));

        assertThat(dates).containsExactly(date("2026-02-28"), date("2026-03-30"));
    }

    // AT-3
    @Test
    void R2_R4_yearlyPeriodStartFollowsLeapDayAnchor() {
        final BillingCalendar calendar = new BillingCalendar(date("2028-02-29"), 0, Interval.YEAR, null);

        assertThat(calendar.currentPeriod(date("2029-03-01")).start()).isEqualTo(date("2029-02-28"));
        assertThat(calendar.currentPeriod(date("2032-03-01")).start()).isEqualTo(date("2032-02-29"));
    }

    // AT-4
    @Test
    void R1_R5_R6_trialingUntilFirstBillingDayThenActive() {
        final BillingCalendar calendar = new BillingCalendar(date("2026-01-24"), 7, Interval.MONTH, null);

        assertThat(calendar.firstBillingOn()).isEqualTo(date("2026-01-31"));
        assertThat(calendar.status(date("2026-01-30"))).isEqualTo(Status.TRIALING);
        assertThat(calendar.status(date("2026-01-31"))).isEqualTo(Status.ACTIVE);
        assertThat(calendar.nextBillingOn(date("2026-01-31"))).isEqualTo(date("2026-02-28"));
    }
}
