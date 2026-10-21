package edu.columbia.subtrack.api;

import edu.columbia.subtrack.service.SubscriptionService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptions;
    private final Clock clock;

    public SubscriptionController(SubscriptionService subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    /** SPEC 5.2. */
    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    SubscriptionView subscribe(@RequestAttribute(ClientKeyFilter.CLIENT_KEY) String clientKey,
                               @RequestBody SubscriptionRequest request,
                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        LocalDate today = LocalDate.now(clock);
        var command = request.validated(today);
        IdempotencyKeys.check(idempotencyKey);
        return SubscriptionView.of(subscriptions.subscribe(clientKey, command, idempotencyKey, today));
    }
}
