package edu.columbia.subtrack.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates IDs such as {@code pln_3k8d0a2zpr}: a prefix and 10 lowercase letters or digits. */
@Component
public class IdGenerator {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    public String next(String prefix) {
        StringBuilder id = new StringBuilder(prefix);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            id.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return id.toString();
    }
}
