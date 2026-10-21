package edu.columbia.subtrack.domain;

import java.time.LocalDate;

/** A billing period; {@code end} is exclusive (R4). */
public record Period(LocalDate start, LocalDate end) {
}
