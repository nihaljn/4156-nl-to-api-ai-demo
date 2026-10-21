package edu.columbia.subtrack.api;

import static edu.columbia.subtrack.support.Http.CLIENT_A;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.columbia.subtrack.support.Http;
import edu.columbia.subtrack.support.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RequestValidationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void section5_5_billingDatesWindowIsCappedAt92Days() throws Exception {
        // AT-14
        Http.getAs(mvc, CLIENT_A, "/billing-dates?from=2026-03-01&to=2026-06-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("to"));
        Http.getAs(mvc, CLIENT_A, "/billing-dates?from=2026-03-01&to=2026-05-31")
                .andExpect(status().isOk());
    }

    @Test
    void section5_2_startDateAfterTodayIsRejected() throws Exception {
        // AT-15
        String planId = Http.createM7(mvc, CLIENT_A);

        Http.subscribe(mvc, CLIENT_A, planId, "cus_42", "2026-03-06")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("startDate"));
    }
}
