package edu.columbia.subtrack.api;

import edu.columbia.subtrack.domain.Interval;
import edu.columbia.subtrack.domain.PlanTerms;
import java.util.Currency;

/** Body of POST /plans. Fields are nullable here so a missing one is a VALIDATION_ERROR, not a parse error. */
public record PlanRequest(String name, Long priceCents, String currency, String interval, Integer trialDays) {

    private static final long MAX_PRICE_CENTS = 10_000_000L;

    /** Checks the fields in the order SPEC section 3 lists them and returns the validated terms. */
    public PlanTerms validated() {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty() || trimmedName.length() > 100) {
            throw invalid("name", "name must be between 1 and 100 characters");
        }
        if (priceCents == null || priceCents < 1 || priceCents > MAX_PRICE_CENTS) {
            throw invalid("priceCents", "priceCents must be between 1 and 10000000");
        }
        if (!isCurrencyCode(currency)) {
            throw invalid("currency", "currency must be a valid 3-letter uppercase currency code");
        }
        Interval parsedInterval = parseInterval();
        if (trialDays == null || trialDays < 0 || trialDays > 30) {
            throw invalid("trialDays", "trialDays must be between 0 and 30");
        }
        return new PlanTerms(trimmedName, priceCents, currency, parsedInterval, trialDays);
    }

    private static boolean isCurrencyCode(String code) {
        if (code == null || !code.matches("[A-Z]{3}")) {
            return false;
        }
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Interval parseInterval() {
        try {
            return Interval.valueOf(interval);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw invalid("interval", "interval must be MONTH or YEAR");
        }
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException("VALIDATION_ERROR", message, field);
    }
}
