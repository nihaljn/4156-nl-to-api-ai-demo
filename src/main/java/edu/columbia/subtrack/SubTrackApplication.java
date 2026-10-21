package edu.columbia.subtrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the SubTrack service. */
@SpringBootApplication
public class SubTrackApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(SubTrackApplication.class, args);
    }
}
