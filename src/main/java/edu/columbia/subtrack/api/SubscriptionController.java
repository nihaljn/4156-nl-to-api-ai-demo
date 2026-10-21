package edu.columbia.subtrack.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.columbia.subtrack.service.IdempotencyStore.StoredResponse;
import edu.columbia.subtrack.service.SubscriptionService;
import edu.columbia.subtrack.service.SubscriptionService.NewSubscription;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** POST /subscriptions (SPEC.md section 5.2). */
@RestController
public class SubscriptionController {

    private static final String START_DATE = "startDate";
    private static final Pattern CUSTOMER_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final SubscriptionService subscriptionService;
    private final ObjectMapper mapper;
    private final Clock clock;

    /**
     * Creates the controller.
     *
     * @param subscriptionService creates subscriptions
     * @param mapper parses the request body
     * @param clock supplies today's date
     */
    public SubscriptionController(final SubscriptionService subscriptionService, final ObjectMapper mapper,
            final Clock clock) {
        this.subscriptionService = subscriptionService;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Creates a subscription.
     *
     * @param clientKey the calling client
     * @param idempotencyKey optional key that makes repeats safe
     * @param rawBody the request body
     * @return 201 with the subscription view
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<Object> create(@RequestHeader("X-Client-Key") final String clientKey,
            @RequestHeader(value = "Idempotency-Key", required = false) final String idempotencyKey,
            @RequestBody(required = false) final String rawBody) {
        final JsonNode body = RequestBodies.parseObject(mapper, rawBody);
        RequestBodies.checkIdempotencyKey(idempotencyKey);
        final LocalDate today = LocalDate.now(clock);
        final NewSubscription request = validate(body, today);
        final StoredResponse response = subscriptionService.create(clientKey, idempotencyKey, body, request, today);
        return ResponseEntity.status(response.status()).body(response.body());
    }

    private static NewSubscription validate(final JsonNode body, final LocalDate today) {
        final JsonNode planId = body.get("planId");
        if (planId == null || !planId.isTextual() || planId.textValue().isBlank()) {
            throw invalid("planId", "planId is required");
        }
        final JsonNode customerId = body.get("customerId");
        if (customerId == null || !customerId.isTextual()
                || !CUSTOMER_ID.matcher(customerId.textValue()).matches()) {
            throw invalid("customerId", "customerId must be 1 to 64 characters from A-Z a-z 0-9 _ -");
        }
        return new NewSubscription(planId.textValue(), customerId.textValue(), startDate(body.get(START_DATE), today));
    }

    private static LocalDate startDate(final JsonNode node, final LocalDate today) {
        if (node == null || node.isNull()) {
            return today;
        }
        final LocalDate date = node.isTextual() ? parseDate(node.textValue()).orElse(null) : null;
        if (date == null) {
            throw invalid(START_DATE, "startDate must be an ISO date (YYYY-MM-DD)");
        }
        if (date.isAfter(today)) {
            throw invalid(START_DATE, "startDate must not be after today");
        }
        return date;
    }

    private static Optional<LocalDate> parseDate(final String text) {
        try {
            return Optional.of(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static ApiException invalid(final String field, final String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message, field);
    }
}
