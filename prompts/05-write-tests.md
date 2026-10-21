# 05 — Write the tests from the spec, before the code

**Slide:** Writing Tests
**Mode:** normal. Only `src/test` may change. Run `04a-scaffold.md` first, so `./mvnw` already works.

```text
From SPEC.md, write tests only. Do not write any implementation code.

Write one test per acceptance test in section 7.

Keep the mapping one to one. Each item above is exactly one test method
(a parameterized method counts as one, with one case per row or cell).
Do not split a rule into extra tests, and do not write any test that is
not on this list, even if it seems useful. If SPEC.md leaves something
untested, report it in the list at the end instead of inventing a test.

The project already builds (./mvnw works); don't touch pom.xml or the
config files. The tests may reference only these names, which the
implementation will create under edu.columbia.subtrack: domain.BillingCalendar
(with Interval, Status and Period), service.PlanService,
service.SubscriptionService, service.BillingDatesService, and
api.ApiException(code, message, field).

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
