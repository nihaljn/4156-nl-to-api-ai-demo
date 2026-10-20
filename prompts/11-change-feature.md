# 11 — When the agent built it differently, or the feature changes

**Slide:** Debugging and Updating Features
**Rule of thumb:** change it everywhere, in order: spec → tests → code.

## A. The agent implemented a feature differently from what you wanted

```text
SPEC.md rule R7 says AT_PERIOD_END on a CANCELED subscription returns
409 ALREADY_CANCELED. The implementation returns 200 instead.

Don't change any code yet. First tell me which is true:
(a) the spec is ambiguous here, so your reading was reasonable, or
(b) the spec is clear and the code is wrong.

If (a): propose the exact wording change to SPEC.md and the test that
pins it down. If (b): write a failing test that cites R7, then fix the code.
```

## B. You want to change a feature that already works

Example change: plans can bill every N months (quarterly = 3), not only every month or year.

```text
We're changing a feature. Work in this order and stop after each step
for my review:

1. SPEC.md: add intervalCount (integer 1–12, default 1) to Plan. Update
   R2 so billing(k) = firstBillingOn.plusMonths(k * intervalCount) for
   MONTH. Update section 5.1, the error table and the acceptance tests.
   Show me the spec diff.

2. Tests: add fail-to-pass tests for the new behavior (for example, a
   quarterly plan first billed on 2026-01-31 bills next on 2026-04-30,
   then 2026-07-31). Run them and confirm they fail for the right reason.
   Every existing test is a pass-to-pass test: don't edit any of them.

3. Code: make the new tests pass. Then run ./mvnw verify and confirm
   that every previously passing test still passes.

Report the fail-to-pass and pass-to-pass counts at the end.
```

**Fail-to-pass** = new tests that fail before the change and pass after. **Pass-to-pass** = old tests that must keep passing. Together they show the change works and broke nothing.
