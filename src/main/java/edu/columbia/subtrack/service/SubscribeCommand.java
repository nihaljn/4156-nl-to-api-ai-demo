package edu.columbia.subtrack.service;

import java.time.LocalDate;

/** The validated body of POST /subscriptions; {@code startDate} is null when the client left it out. */
public record SubscribeCommand(String planId, String customerId, LocalDate startDate) {
}
