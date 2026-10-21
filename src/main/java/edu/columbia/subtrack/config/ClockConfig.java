package edu.columbia.subtrack.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /** The only source of "today" (SPEC section 2); tests replace it. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
