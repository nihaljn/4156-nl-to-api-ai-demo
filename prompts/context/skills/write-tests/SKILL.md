---
name: write-tests
description: Write SubTrack tests from SPEC.md before implementation exists. Use when asked to write, extend or review tests.
---

# Write tests from the spec

**One to one.** Write exactly the tests the prompt lists, each as one test method (a parameterized method counts as one, with one case per row or cell). Don't split a rule into extra tests, and don't write a test that isn't on the list, even if it seems useful. A spec sentence with no listed test goes in the report (step 5), not in a new test.

1. **Read the list in the prompt** and map each item to the spec text it comes from (R1–R8, a worked-example row, an acceptance test AT-n, a cancel-table cell). That mapping is the whole test plan.
2. **Pick the level:**
   - Date and status rules (R1–R6): plain JUnit 5 on `BillingCalendar`, `@ParameterizedTest` with `@CsvSource` for table rows.
   - Acceptance tests that call endpoints: `@SpringBootTest` with MockMvc. `@WebMvcTest` only where the test needs no real service.
3. **Copy the spec's cases as written:** every row of the worked example, every cell of the cancel state table (4 states × 2 modes), and the acceptance tests' givens and expected results. Don't derive boundaries or variants the spec doesn't state.
4. **Use the test clock.** Inject the `MutableClock` test bean (fixed at 2026-03-05, with `advance(Duration)`). Never rely on the real date.
5. **Name tests after the rule:** `R7_atPeriodEndOnCanceledReturns409`.
6. **Run them red.** `./mvnw test` should fail before implementation. A test that passes against an empty service checks nothing.
7. **Report** every spec sentence the listed tests don't assert (limits, check order, error codes, response fields and so on). Those are spec gaps for the reader to decide on; don't write tests for them.
