---
name: write-tests
description: Write SubTrack tests from SPEC.md before implementation exists. Use when asked to write, extend or review tests.
---

# Write tests from the spec

1. **List the claims.** Every sentence in SPEC.md that says something returns, is, must or never happens is a claim. Each has an ID (R1–R8, section 2, section 5.x).
2. **Pick the level:**
   - R1–R6 (dates, status): plain JUnit 5 on `BillingCalendar`, `@ParameterizedTest` with `@CsvSource`.
   - R7, R8: service tests with an in-memory repository.
   - Status codes, error body, check order: `@WebMvcTest` + MockMvc.
   - Idempotency, isolation, concurrency: `@SpringBootTest` on H2.
3. **Derive cases:**
   - Boundaries on both sides of every limit (0/30/31 trial days, 92/93-day window, the day before and on `endsOn`).
   - Every row of the spec's worked example, copied as written.
   - Every cell of every state × input table (cancel: 4 states × 2 modes).
   - Invariants over many generated dates (billing dates increase and never drift; status never goes backward).
4. **Use the test clock.** Inject the `MutableClock` test bean (fixed at 2026-03-05, with `advance(Duration)`). Never rely on the real date.
5. **Name tests after the rule:** `R7_atPeriodEndOnCanceledReturns409`.
6. **Run them red.** `./mvnw test` should fail before implementation. A test that passes against an empty service checks nothing.
7. **Report** any spec sentence you couldn't turn into an assertion. Those are spec gaps.
