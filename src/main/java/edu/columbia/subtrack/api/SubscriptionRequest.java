package edu.columbia.subtrack.api;

import edu.columbia.subtrack.service.SubscribeCommand;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Body of POST /subscriptions. Fields are nullable here so a missing one is a VALIDATION_ERROR, not a parse error. */
public record SubscriptionRequest(String planId, String customerId, String startDate) {

    private static final int MAX_CUSTOMER_ID_LENGTH = 64;

    /** Checks the fields in the order SPEC 5.2 lists them and returns the validated command. */
    public SubscribeCommand validated(LocalDate today) {
        if (planId == null || planId.isBlank()) {
            throw invalid("planId", "planId is required");
        }
        if (customerId == null || customerId.isEmpty() || customerId.length() > MAX_CUSTOMER_ID_LENGTH
                || !customerId.matches("[A-Za-z0-9_-]+")) {
            throw invalid("customerId", "customerId must be 1 to 64 characters from A-Z a-z 0-9 _ -");
        }
        return new SubscribeCommand(planId, customerId, parseStartDate(today));
    }

    private LocalDate parseStartDate(LocalDate today) {
        if (startDate == null) {
            return null;
        }
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(startDate);
        } catch (DateTimeParseException e) {
            throw invalid("startDate", "startDate must be a date in the form YYYY-MM-DD");
        }
        if (parsed.isAfter(today)) {
            throw invalid("startDate", "startDate must not be after today");
        }
        return parsed;
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException("VALIDATION_ERROR", message, field);
    }
}
