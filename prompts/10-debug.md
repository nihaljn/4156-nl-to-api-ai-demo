# 10 — Debug a failing test

**Slide:** Debugging and Updating Features
**Mode:** plan mode for steps 1–4, then normal for the fix. Run on the `planted-bug` branch.

The planted bug: the next billing date is computed as `previousBillingDate.plusMonths(1)`, so a subscription first billed on Jan 31 bills on Feb 28, then Mar 28 instead of Mar 31.

```text
./mvnw verify fails. Here is the output:

<paste the failing test names and assertion messages>

Don't edit any files yet.
1. Restate what the failing test expects and what actually happened.
2. Find the smallest input that reproduces it (one plan, one
   subscription, a fixed clock).
3. Explain the root cause. Cite the line of code and the SPEC.md rule
   it breaks.
4. Propose a fix that removes the cause, and explain why it isn't just
   making this one test pass.

Wait for my go-ahead before changing code.
```

After approval:

```text
Apply the fix. Keep the failing test as a regression test, run
./mvnw verify, and show me the diff.
```

**What to reject live:** a fix that special-cases day 31, adds an "end of month" flag, catches an exception, or edits the test.
