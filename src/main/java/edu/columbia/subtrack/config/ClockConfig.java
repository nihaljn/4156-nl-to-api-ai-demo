package edu.columbia.subtrack.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The one source of the current time (SPEC.md section 2, "Today"). */
@Configuration
public class ClockConfig {

    /**
     * Creates the application clock.
     *
     * @return a UTC clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
