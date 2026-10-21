package edu.columbia.subtrack.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the application's Clock with the movable {@link TestClock}. */
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public TestClock testClock() {
        return new TestClock();
    }
}
