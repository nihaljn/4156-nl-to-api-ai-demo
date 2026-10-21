package edu.columbia.subtrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.columbia.subtrack.api.ApiException;
import edu.columbia.subtrack.api.ErrorCode;
import edu.columbia.subtrack.domain.BillingCalendar.Interval;
import edu.columbia.subtrack.domain.Plan;
import edu.columbia.subtrack.service.IdempotencyStore.StoredResponse;
import java.security.SecureRandom;
import java.util.Currency;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Creates plans (SPEC.md section 5.1). */
@Service
public class PlanService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final long MAX_PRICE_CENTS = 10_000_000L;
    private static final int MAX_TRIAL_DAYS = 30;
    private static final int ID_SUFFIX_LENGTH = 10;
    private static final String ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final Pattern CURRENCY_FORMAT = Pattern.compile("[A-Z]{3}");
    private static final int CREATED = 201;

    private final SecureRandom random = new SecureRandom();
    private final PlanRepository plans;
    private final IdempotencyStore idempotency;

    /**
     * Creates the service.
     *
     * @param plans where plans are stored
     * @param idempotency replays repeated requests
     */
    public PlanService(final PlanRepository plans, final IdempotencyStore idempotency) {
        this.plans = plans;
        this.idempotency = idempotency;
    }

    /**
     * Validates the request and stores a new plan.
     *
     * @param clientKey the calling client
     * @param idempotencyKey the Idempotency-Key header, or null
     * @param body the parsed request body
     * @return 201 with the stored plan, or the stored response of an earlier identical request
     * @throws ApiException VALIDATION_ERROR, PLAN_NAME_TAKEN or IDEMPOTENCY_MISMATCH
     */
    public StoredResponse create(final String clientKey, final String idempotencyKey, final JsonNode body) {
        final Plan draft = validate(body);
        return idempotency.execute(clientKey, idempotencyKey, body, () -> {
            final Plan plan = new Plan(newId(), draft.name(), draft.priceCents(), draft.currency(),
                draft.interval(), draft.trialDays());
            if (!plans.addIfNameFree(clientKey, plan)) {
                throw new ApiException(ErrorCode.PLAN_NAME_TAKEN, "A plan named '" + plan.name()
                    + "' already exists", "name");
            }
            return new StoredResponse(CREATED, plan);
        });
    }

    private Plan validate(final JsonNode body) {
        final String name = text(body, "name");
        if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH) {
            throw invalid("name", "name must be between 1 and 100 characters");
        }
        final JsonNode price = body.get("priceCents");
        if (price == null || !price.isIntegralNumber() || !price.canConvertToLong()
                || price.longValue() < 1 || price.longValue() > MAX_PRICE_CENTS) {
            throw invalid("priceCents", "priceCents must be between 1 and 10000000");
        }
        final String currency = text(body, "currency");
        if (!isCurrency(currency)) {
            throw invalid("currency", "currency must be a valid 3-letter uppercase currency code");
        }
        final Interval interval = interval(text(body, "interval"));
        final JsonNode trial = body.get("trialDays");
        if (trial == null || !trial.isIntegralNumber() || !trial.canConvertToInt()
                || trial.intValue() < 0 || trial.intValue() > MAX_TRIAL_DAYS) {
            throw invalid("trialDays", "trialDays must be between 0 and 30");
        }
        return new Plan(null, name.strip(), price.longValue(), currency, interval, trial.intValue());
    }

    private static String text(final JsonNode body, final String field) {
        final JsonNode node = body.get(field);
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    private static boolean isCurrency(final String code) {
        if (code == null || !CURRENCY_FORMAT.matcher(code).matches()) {
            return false;
        }
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Interval interval(final String value) {
        for (final Interval candidate : Interval.values()) {
            if (candidate.name().equals(value)) {
                return candidate;
            }
        }
        throw invalid("interval", "interval must be MONTH or YEAR");
    }

    private static ApiException invalid(final String field, final String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message, field);
    }

    private String newId() {
        final StringBuilder id = new StringBuilder("pln_");
        for (int i = 0; i < ID_SUFFIX_LENGTH; i++) {
            id.append(ID_ALPHABET.charAt(random.nextInt(ID_ALPHABET.length())));
        }
        return id.toString();
    }
}
