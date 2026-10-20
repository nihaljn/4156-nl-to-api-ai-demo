# 05 — Write the tests from the spec, before the code

**Slide:** Writing Tests
**Mode:** normal. Only `src/test` may change.

```text
From SPEC.md, write tests only. Do not write any implementation code.

Write:
- one test per rule in section 4 (R1–R8),
- one parameterized case per row of the worked example,
- one test per acceptance test in section 7,
- one test per cell of the cancel state table (4 states × 2 modes),
- an invariant test that billing dates never drift: for every first
  billing date in 2024–2028 and k = 1..24, billing date k falls on the
  first date's day of month, or on the last day of a shorter month.

Put date and status rules in plain JUnit tests against a BillingCalendar
class (no Spring). Put status codes and error shape in @WebMvcTest
tests. Put idempotency, isolation and concurrency in @SpringBootTest
tests. Every test uses a test Clock fixed at 2026-03-05 that tests can
advance.

Name each test after the rule it checks, for example
"R2_billingDateKeepsMonthEndAnchor".

When you're done, run ./mvnw test. Every test should fail or not compile,
because there is no implementation yet. Then list every sentence in
SPEC.md you couldn't turn into an assertion.
```

**What to point out live:** the last list is a free spec review. Then commit the tests before any implementation exists.
