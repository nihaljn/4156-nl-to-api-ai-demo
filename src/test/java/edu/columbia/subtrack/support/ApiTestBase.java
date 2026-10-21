package edu.columbia.subtrack.support;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack base: real controllers, services and H2, with the test clock fixed at 2026-03-05.
 * Every test gets a fresh client key, so each starts from an empty service as seen by its client.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
public abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    protected SubTrackApi api;

    protected String client;

    @BeforeEach
    protected void freshClientAndClock() {
        clock.reset();
        api = new SubTrackApi(mvc);
        client = "client-" + UUID.randomUUID();
    }

    /** Plan M7: MONTH, 999 USD, 7-day trial. */
    protected String m7() {
        return api.createPlan(client, "M7", "MONTH", 999, 7);
    }

    /** Plan M0: MONTH, 999 USD, no trial. */
    protected String m0() {
        return api.createPlan(client, "M0", "MONTH", 999, 0);
    }

    /** Plan Y0: YEAR, 999 USD, no trial. */
    protected String y0() {
        return api.createPlan(client, "Y0", "YEAR", 999, 0);
    }
}
