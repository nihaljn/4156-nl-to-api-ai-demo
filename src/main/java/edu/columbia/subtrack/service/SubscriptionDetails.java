package edu.columbia.subtrack.service;

import edu.columbia.subtrack.domain.Interval;
import edu.columbia.subtrack.domain.Subscription;
import java.time.LocalDate;

/** A subscription with what is needed to compute its view for the day {@code asOf}. */
public record SubscriptionDetails(Subscription subscription, Interval interval, LocalDate asOf) {
}
