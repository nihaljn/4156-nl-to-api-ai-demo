package edu.columbia.subtrack.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates the service's IDs: a prefix plus 10 lowercase letters or digits. */
@Component
public class IdGenerator {

    private static final int SUFFIX_LENGTH = 10;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom random = new SecureRandom();

    /**
     * Creates an ID.
     *
     * @param prefix {@code pln_} or {@code sub_}
     * @return the new ID
     */
    public String newId(final String prefix) {
        final StringBuilder id = new StringBuilder(prefix);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            id.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
