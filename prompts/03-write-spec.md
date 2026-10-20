# 03 — Turn decisions into SPEC.md

**Slides:** How to write a spec with the agent · What does a good spec look like?
**Mode:** normal (the agent writes `SPEC.md`).

```text
Here are our decisions for the open questions you listed:

- Money is integer cents. One ISO 4217 currency per plan.
- Plans: name (unique per client, ignoring case), priceCents 1–10,000,000,
  interval MONTH or YEAR, trialDays 0–30. Plans can't be edited.
- All dates are UTC calendar dates (YYYY-MM-DD), no times. "Today" comes
  from an injected java.time.Clock. Read endpoints take an optional asOf date.
- firstBillingOn = startDate + trialDays. Billing date k = firstBillingOn
  plus k intervals, always computed from firstBillingOn, never from the
  previous billing date. java.time's month-end clamping is the intended behavior.
- Status is computed from dates on every read, never stored:
  CANCELED if endsOn is set and asOf >= endsOn; else TRIALING if
  asOf < firstBillingOn; else ACTIVE.
- Cancel has two modes. AT_PERIOD_END sets endsOn to the end of the
  current period; IMMEDIATE sets it to today. Repeating a mode changes
  nothing. AT_PERIOD_END on a CANCELED subscription is 409 ALREADY_CANCELED.
- One live subscription per customer per plan (409 ALREADY_SUBSCRIBED).
- POST /plans and POST /subscriptions accept an Idempotency-Key header.
- Every request carries X-Client-Key; clients only see their own data;
  another client's IDs return 404.
- Errors are always {"error", "message", "field"} with fixed codes.
- Billing-dates window: at most 92 days, both ends included.

Write SPEC.md for this service. It must have:

1. Scope, plus an explicit out-of-scope list.
2. Conventions stated once (identity, dates and the clock, money, IDs,
   idempotency, error shape).
3. A data model: every stored field with its type and limits. Say which
   values are derived and never stored.
4. Rules numbered R1, R2, ... Each rule must be specific enough to write
   an assertion for. Include one worked example traced across several dates.
5. For each endpoint: request, the order checks run in, a full JSON
   example response, and its error codes.
6. One table of every error code and its HTTP status.
7. Acceptance tests as Given / When / Then, each citing the rule it checks.

Don't add features we didn't decide on. If something is still undecided,
write it as "OPEN:" instead of picking.
```

**Reference answer:** the "SPEC.md (final example)" tab of the planning doc.
