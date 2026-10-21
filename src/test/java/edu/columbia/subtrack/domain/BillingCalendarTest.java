package edu.columbia.subtrack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.columbia.subtrack.support.MutableClock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * SPEC section 4, R1-R6, against the pure domain class (no Spring).
 *
 * <p>Assumed API, since none exists yet: {@code BillingCalendar} is built from a subscription's
 * stored dates and answers every question for a date {@code D} passed in explicitly.
 * The test clock supplies "today" where a test needs one.
 */
class BillingCalendarTest {

    private final MutableClock clock = new MutableClock();

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private static BillingCalendar monthly(String start, String firstBilling, String endsOn) {
        return new BillingCalendar(d(start), d(firstBilling), Interval.MONTH,
                endsOn == null ? null : d(endsOn));
    }

    private static BillingCalendar yearly(String start, String firstBilling, String endsOn) {
        return new BillingCalendar(d(start), d(firstBilling), Interval.YEAR,
                endsOn == null ? null : d(endsOn));
    }

    // ---- R1 ----------------------------------------------------------------------------------

    @ParameterizedTest(name = "R1: {0} + {1} trial days = {2}")
    @CsvSource({
        "2026-01-24, 7,  2026-01-31",
        "2026-01-24, 0,  2026-01-24",
        "2026-01-24, 30, 2026-02-23",
        "2026-02-20, 30, 2026-03-22",
        "2028-02-01, 28, 2028-02-29",
        "2026-12-25, 30, 2027-01-24",
        "2026-01-31, 1,  2026-02-01"
    })
    void R1_firstBillingOnIsStartDatePlusTrialDays(String start, int trialDays, String expected) {
        assertThat(BillingCalendar.firstBillingOn(d(start), trialDays)).isEqualTo(d(expected));
    }

    // ---- R2 ----------------------------------------------------------------------------------

    @ParameterizedTest(name = "R2: first {0} {1}, k={2} -> {3}")
    @CsvSource({
        "2026-03-05, MONTH, 0,  2026-03-05",
        "2026-01-31, MONTH, 1,  2026-02-28",
        "2026-01-31, MONTH, 2,  2026-03-31",
        "2026-01-31, MONTH, 3,  2026-04-30",
        "2026-01-31, MONTH, 13, 2027-02-28",
        "2026-01-30, MONTH, 1,  2026-02-28",
        "2026-01-30, MONTH, 2,  2026-03-30",
        "2026-01-29, MONTH, 1,  2026-02-28",
        "2028-01-29, MONTH, 1,  2028-02-29",
        "2028-01-31, MONTH, 1,  2028-02-29",
        "2028-02-29, YEAR,  1,  2029-02-28",
        "2028-02-29, YEAR,  4,  2032-02-29"
    })
    void R2_billingDateKeepsMonthEndAnchor(String first, Interval interval, int k, String expected) {
        BillingCalendar calendar = new BillingCalendar(d(first), d(first), interval, null);

        assertThat(calendar.billingDate(k)).isEqualTo(d(expected));
    }

    @Test
    void R2_billingDateIsNeverComputedFromThePreviousBillingDate() {
        BillingCalendar calendar = monthly("2026-01-31", "2026-01-31", null);

        // chaining plusMonths(1) from Feb 28 would give Mar 28
        assertThat(calendar.billingDate(2)).isEqualTo(d("2026-03-31"));
    }

    @Test
    void R2_billingDatesNeverDrift() {
        for (LocalDate first = d("2024-01-01"); !first.isAfter(d("2028-12-31")); first = first.plusDays(1)) {
            for (Interval interval : Interval.values()) {
                BillingCalendar calendar = new BillingCalendar(first, first, interval, null);
                LocalDate previous = first;
                for (int k = 1; k <= 24; k++) {
                    LocalDate actual = calendar.billingDate(k);
                    YearMonth month = interval == Interval.MONTH
                            ? YearMonth.from(first).plusMonths(k)
                            : YearMonth.from(first).plusYears(k);
                    int anchoredDay = Math.min(first.getDayOfMonth(), month.lengthOfMonth());

                    assertThat(YearMonth.from(actual))
                            .as("month of %s billing date %d for first %s", interval, k, first)
                            .isEqualTo(month);
                    assertThat(actual.getDayOfMonth())
                            .as("day of %s billing date %d for first %s", interval, k, first)
                            .isEqualTo(anchoredDay);
                    assertThat(actual).as("billing dates increase").isAfter(previous);
                    previous = actual;
                }
            }
        }
    }

    // ---- R3 ----------------------------------------------------------------------------------

    @Test
    void R3_openEndedSubscriptionCountsEveryBillingDate() {
        BillingCalendar calendar = monthly("2026-01-31", "2026-01-31", null);

        assertThat(calendar.billingDatesBetween(d("2026-01-01"), d("2026-05-31")))
                .containsExactly(d("2026-01-31"), d("2026-02-28"), d("2026-03-31"),
                        d("2026-04-30"), d("2026-05-31"));
    }

    @Test
    void R3_billingDateBeforeEndsOnCounts() {
        BillingCalendar calendar = monthly("2026-01-31", "2026-01-31", "2026-04-01");

        assertThat(calendar.billingDatesBetween(d("2026-01-01"), d("2026-12-31")))
                .containsExactly(d("2026-01-31"), d("2026-02-28"), d("2026-03-31"));
    }

    @Test
    void R3_billingDateOnEndsOnDoesNotCount() {
        BillingCalendar calendar = monthly("2026-01-31", "2026-01-31", "2026-03-31");

        assertThat(calendar.billingDatesBetween(d("2026-01-01"), d("2026-12-31")))
                .containsExactly(d("2026-01-31"), d("2026-02-28"));
    }

    @Test
    void R3_noBillingDateCountsWhenEndsOnIsTheFirstBillingDate() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-01-31");

        assertThat(calendar.billingDatesBetween(d("2026-01-01"), d("2026-12-31"))).isEmpty();
    }

    @Test
    void R3_noBillingDateCountsWhenEndsOnIsBeforeTheFirstBillingDate() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-01-26");

        assertThat(calendar.billingDatesBetween(d("2026-01-01"), d("2026-12-31"))).isEmpty();
    }

    @Test
    void S5_5_billingDatesWindowIncludesBothEnds() {
        BillingCalendar calendar = monthly("2026-01-31", "2026-01-31", null);

        assertThat(calendar.billingDatesBetween(d("2026-02-28"), d("2026-03-31")))
                .containsExactly(d("2026-02-28"), d("2026-03-31"));
        assertThat(calendar.billingDatesBetween(d("2026-03-31"), d("2026-03-31")))
                .containsExactly(d("2026-03-31"));
        assertThat(calendar.billingDatesBetween(d("2026-03-01"), d("2026-03-30"))).isEmpty();
    }

    // ---- R4 ----------------------------------------------------------------------------------

    @Test
    void R4_beforeFirstBillingThePeriodRunsFromStartDateToFirstBilling() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", null);

        assertThat(calendar.currentPeriod(d("2026-01-24")))
                .contains(new Period(d("2026-01-24"), d("2026-01-31")));
        assertThat(calendar.currentPeriod(d("2026-01-30")))
                .contains(new Period(d("2026-01-24"), d("2026-01-31")));
    }

    @Test
    void R4_firstBillingDateStartsTheFirstFullPeriod() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", null);

        assertThat(calendar.currentPeriod(d("2026-01-31")))
                .contains(new Period(d("2026-01-31"), d("2026-02-28")));
    }

    @Test
    void R4_periodEndIsExclusive() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", null);

        assertThat(calendar.currentPeriod(d("2026-02-27")))
                .contains(new Period(d("2026-01-31"), d("2026-02-28")));
        assertThat(calendar.currentPeriod(d("2026-02-28")))
                .contains(new Period(d("2026-02-28"), d("2026-03-31")));
    }

    @Test
    void R4_periodBoundariesUseTheMonthEndAnchor() {
        BillingCalendar calendar = monthly("2026-01-31", "2026-01-31", null);

        assertThat(calendar.currentPeriod(d("2026-03-30")))
                .contains(new Period(d("2026-02-28"), d("2026-03-31")));
        assertThat(calendar.currentPeriod(d("2026-04-30")))
                .contains(new Period(d("2026-04-30"), d("2026-05-31")));
    }

    @Test
    void R4_yearlyPeriodAfterALeapDayAnchor() {
        BillingCalendar calendar = yearly("2028-02-29", "2028-02-29", null);

        assertThat(calendar.currentPeriod(d("2029-03-01")))
                .contains(new Period(d("2029-02-28"), d("2030-02-28")));
        assertThat(calendar.currentPeriod(d("2032-03-01")))
                .contains(new Period(d("2032-02-29"), d("2033-02-28")));
    }

    @Test
    void R4_periodEndIsNotClippedToEndsOn() {
        // Cancelled IMMEDIATE on 03-05, read as of 03-01: the cancel is already stored.
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-03-05");

        assertThat(calendar.currentPeriod(d("2026-03-01")))
                .contains(new Period(d("2026-02-28"), d("2026-03-31")));
    }

    @Test
    void R4_canceledHasNoCurrentPeriod() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-03-31");

        assertThat(calendar.currentPeriod(d("2026-03-31"))).isEmpty();
        assertThat(calendar.currentPeriod(d("2026-04-15"))).isEmpty();
    }

    // ---- R5 ----------------------------------------------------------------------------------

    @Test
    void R5_nextBillingDateIsStrictlyAfterD() {
        BillingCalendar calendar = monthly("2026-01-31", "2026-01-31", null);

        assertThat(calendar.nextBillingOn(d("2026-02-28"))).contains(d("2026-03-31"));
        assertThat(calendar.nextBillingOn(d("2026-02-27"))).contains(d("2026-02-28"));
    }

    @Test
    void R5_beforeFirstBillingTheNextBillingDateIsTheFirst() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", null);

        assertThat(calendar.nextBillingOn(d("2026-01-24"))).contains(d("2026-01-31"));
    }

    @Test
    void R5_billingDateBeforeEndsOnStillCounts() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-04-10");

        assertThat(calendar.nextBillingOn(d("2026-03-05"))).contains(d("2026-03-31"));
        assertThat(calendar.nextBillingOn(d("2026-03-31"))).isEmpty();
    }

    @Test
    void R5_isEmptyWhenEndsOnLeavesNoLaterCountingDate() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-03-31");

        assertThat(calendar.nextBillingOn(d("2026-01-31"))).contains(d("2026-02-28"));
        assertThat(calendar.nextBillingOn(d("2026-02-28"))).isEmpty();
        assertThat(calendar.nextBillingOn(d("2026-03-30"))).isEmpty();
    }

    @Test
    void R5_isEmptyOnceCanceled() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-03-31");

        assertThat(calendar.nextBillingOn(d("2026-03-31"))).isEmpty();
        assertThat(calendar.nextBillingOn(d("2026-06-01"))).isEmpty();
    }

    // ---- R6 ----------------------------------------------------------------------------------

    @Test
    void R6_trialingBeforeFirstBillingDate() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", null);

        assertThat(calendar.status(d("2026-01-24"))).isEqualTo(Status.TRIALING);
        assertThat(calendar.status(d("2026-01-30"))).isEqualTo(Status.TRIALING);
    }

    @Test
    void R6_activeFromTheFirstBillingDate() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", null);

        assertThat(calendar.status(d("2026-01-31"))).isEqualTo(Status.ACTIVE);
        assertThat(calendar.status(d("2030-01-01"))).isEqualTo(Status.ACTIVE);
    }

    @Test
    void R6_noTrialIsActiveFromTheStartDate() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-24", null);

        assertThat(calendar.status(d("2026-01-24"))).isEqualTo(Status.ACTIVE);
    }

    @Test
    void R6_canceledFromEndsOnOnward() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-03-31");

        assertThat(calendar.status(d("2026-03-30"))).isEqualTo(Status.ACTIVE);
        assertThat(calendar.status(d("2026-03-31"))).isEqualTo(Status.CANCELED);
        assertThat(calendar.status(d("2026-04-01"))).isEqualTo(Status.CANCELED);
    }

    @Test
    void R6_canceledWinsOverTrialing() {
        LocalDate today = LocalDate.now(clock);
        BillingCalendar calendar = new BillingCalendar(today.minusDays(2), today.plusDays(5),
                Interval.MONTH, today);

        assertThat(calendar.status(today)).isEqualTo(Status.CANCELED);
    }

    @Test
    void R6_scheduledCancelDuringTheTrialKeepsTrialing() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-01-31");

        assertThat(calendar.status(d("2026-01-30"))).isEqualTo(Status.TRIALING);
        assertThat(calendar.cancelAtPeriodEnd(d("2026-01-30"))).isTrue();
        assertThat(calendar.status(d("2026-01-31"))).isEqualTo(Status.CANCELED);
    }

    @Test
    void R6_cancelAtPeriodEndIsTrueOnlyBetweenTheCancelAndEndsOn() {
        BillingCalendar open = monthly("2026-01-24", "2026-01-31", null);
        BillingCalendar scheduled = monthly("2026-01-24", "2026-01-31", "2026-03-31");

        assertThat(open.cancelAtPeriodEnd(d("2026-03-05"))).isFalse();
        assertThat(scheduled.cancelAtPeriodEnd(d("2026-03-05"))).isTrue();
        assertThat(scheduled.cancelAtPeriodEnd(d("2026-03-30"))).isTrue();
        assertThat(scheduled.cancelAtPeriodEnd(d("2026-03-31"))).isFalse();
        assertThat(scheduled.cancelAtPeriodEnd(d("2026-04-30"))).isFalse();
    }

    @Test
    void R6_statusNeverGoesBackward() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", "2026-03-31");
        int rank = 0;
        boolean sawCanceled = false;

        for (LocalDate day = d("2026-01-24"); !day.isAfter(d("2026-06-01")); day = day.plusDays(1)) {
            int current = switch (calendar.status(day)) {
                case TRIALING -> 0;
                case ACTIVE -> 1;
                case CANCELED -> 2;
            };
            assertThat(current).as("status rank on %s", day).isGreaterThanOrEqualTo(rank);
            rank = current;
            sawCanceled |= calendar.status(day) == Status.CANCELED;
        }
        assertThat(sawCanceled).isTrue();
    }

    // ---- Worked example (SPEC section 4) -----------------------------------------------------

    /**
     * Plan MONTH with a 7-day trial, start 2026-01-24. The first three rows are read before the
     * cancel (endsOn null); the last two after the 2026-03-05 AT_PERIOD_END cancel (endsOn 03-31).
     */
    @ParameterizedTest(name = "worked example D={0}")
    @CsvSource(nullValues = "null", textBlock = """
            # D,          endsOn,     status,   periodStart, periodEnd,  nextBillingOn, cancelAtPeriodEnd
            2026-01-24,   null,       TRIALING, 2026-01-24,  2026-01-31, 2026-01-31,    false
            2026-01-31,   null,       ACTIVE,   2026-01-31,  2026-02-28, 2026-02-28,    false
            2026-03-05,   null,       ACTIVE,   2026-02-28,  2026-03-31, 2026-03-31,    false
            2026-03-30,   2026-03-31, ACTIVE,   2026-02-28,  2026-03-31, null,          true
            2026-03-31,   2026-03-31, CANCELED, null,        null,       null,          false
            """)
    void R4R5R6_workedExampleRows(LocalDate day, LocalDate endsOn, Status status,
                                  LocalDate periodStart, LocalDate periodEnd,
                                  LocalDate nextBillingOn, boolean cancelAtPeriodEnd) {
        LocalDate start = d("2026-01-24");
        BillingCalendar calendar = new BillingCalendar(start,
                BillingCalendar.firstBillingOn(start, 7), Interval.MONTH, endsOn);

        assertThat(calendar.status(day)).isEqualTo(status);
        Optional<Period> expectedPeriod = periodStart == null
                ? Optional.empty() : Optional.of(new Period(periodStart, periodEnd));
        assertThat(calendar.currentPeriod(day)).isEqualTo(expectedPeriod);
        assertThat(calendar.nextBillingOn(day)).isEqualTo(Optional.ofNullable(nextBillingOn));
        assertThat(calendar.cancelAtPeriodEnd(day)).isEqualTo(cancelAtPeriodEnd);
    }

    @Test
    void S5_5_workedExampleWithoutCancelListsFebAndMarchBilling() {
        BillingCalendar calendar = monthly("2026-01-24", "2026-01-31", null);

        List<LocalDate> dates = calendar.billingDatesBetween(d("2026-02-01"), d("2026-03-31"));

        assertThat(dates).containsExactly(d("2026-02-28"), d("2026-03-31"));
    }
}
