package edu.columbia.subtrack.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the application's Clock bean with a {@link TestClock}. */
@TestConfiguration
public class TestClockConfig {

    /**
     * Creates the test clock.
     *
     * @return a clock fixed at {@link TestClock#START}
     */
    @Bean
    @Primary
    public TestClock testClock() {
        return new TestClock();
    }
}
