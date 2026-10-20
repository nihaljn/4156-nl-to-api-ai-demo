# SubTrack — Project Proposal (demo)

COMS W4156 Advanced Software Engineering, Fall 2026 · in-class demo, Oct 22

> SubTrack is a made-up project for the AI-driven API development demo. It is written in the same T1 format the class used for its team proposals. No team proposed a subscription service; the closest is one team that lists a subscription-management app as a possible client of its budget service.

## Part 1: Team

Team **Leaders (demo)**: Nihal Jain (nbj2116) and Reya Vir (rrv2116). This is a demo project, so there are no team or repository changes to report.

## Part 2: Service operations

SubTrack keeps track of subscriptions for client programs: the plans they sell, which customers are on which plan, when each subscription bills next, and whether it is active today. It never charges anyone. Clients take payment through their own processor and use SubTrack to know when to charge and how much.

| # | Operation | Endpoint | Computes | Writes |
|---|---|---|---|---|
| 1 | Create plan | `POST /plans` | Validates price, interval, trial | Plan |
| 2 | Subscribe | `POST /subscriptions` | First billing date, initial status | Subscription |
| 3 | Get subscription | `GET /subscriptions/{id}` | Status, current period, next billing date | Nothing |
| 4 | Cancel | `POST /subscriptions/{id}/cancel` | When the subscription ends | End date |
| 5 | List billing dates | `GET /billing-dates` | Every charge due in a date window | Nothing |

### Conventions for every operation

- **Caller identity.** Every request sends an `X-Client-Key` header. Plans and subscriptions are visible only to the client that created them. Another client's IDs return `404`, the same as IDs that don't exist. A missing or unknown key returns `401`.
- **Dates.** Every date is a calendar date (`YYYY-MM-DD`) in UTC, with no time of day. "Today" comes from an injected `Clock`, never from the system time. Read operations take an optional `asOf` date, so tests can ask about any day.
- **Money.** Integer cents (`priceCents`), with one ISO 4217 currency per plan.
- **Errors.** Every error returns `{"error": "CODE", "message": "…", "field": "…"}`. `field` appears only for validation errors.
- **Safe retries.** Operations 1 and 2 accept an `Idempotency-Key` header. The same key with the same body returns the original response and writes nothing. The same key with a different body returns `409 IDEMPOTENCY_MISMATCH`.

### The two rules everything else depends on

**Billing dates.** The first billing date is the start date plus the trial days. Billing date *k* is the first billing date plus *k* intervals, computed from the first billing date every time, not from the previous billing date. When the target month is shorter, the date moves back to that month's last day:

| First billing date | Interval | Next four billing dates |
|---|---|---|
| 2026-01-31 | MONTH | 2026-02-28, 2026-03-31, 2026-04-30, 2026-05-31 |
| 2028-02-29 | YEAR | 2029-02-28, 2030-02-28, 2031-02-28, 2032-02-29 |

The current period runs from the latest billing date on or before `asOf` up to, but not including, the next one. During a trial, the period is the trial itself.

**Status.** Status is computed from dates on every read, never stored, so no background job is needed to flip it:

1. `CANCELED` if the subscription has an end date and `asOf` is on or after it.
2. `TRIALING` if `asOf` is before the first billing date.
3. `ACTIVE` otherwise. If an end date is set but still in the future, the response also shows `cancelAtPeriodEnd: true`.

### 1. Create plan — `POST /plans`

**Client sends** a name (1–100 characters, unique per client, ignoring case), a price from 1 to 10,000,000 cents, a currency, an interval (`MONTH` or `YEAR`), and trial days (0–30).

```json
{ "name": "Pro monthly", "priceCents": 999, "currency": "USD",
  "interval": "MONTH", "trialDays": 7 }
```

**Service does:** validates the fields and stores the plan. Plans can't be edited; a price change is a new plan, so existing subscribers keep the price they signed up for.

**Client receives** `201 Created` with `planId` (`pln_…`) and the stored fields. **Fails with** `400 VALIDATION_ERROR` or `409 PLAN_NAME_TAKEN`.

### 2. Subscribe — `POST /subscriptions`

**Client sends** a plan ID, its own customer ID (1–64 characters), and an optional start date. The start date defaults to today and can be in the past (to import existing subscribers) but not in the future.

```json
{ "planId": "pln_3k", "customerId": "cus_42", "startDate": "2026-01-24" }
```

**Service does:** checks that the plan exists and that this customer has no subscription to this plan that hasn't ended yet. It computes the first billing date and stores the subscription.

**Client receives** `201 Created`:

```json
{ "subscriptionId": "sub_9f", "planId": "pln_3k", "customerId": "cus_42",
  "startDate": "2026-01-24", "firstBillingOn": "2026-01-31",
  "status": "TRIALING", "nextBillingOn": "2026-01-31" }
```

**Fails with** `404 PLAN_NOT_FOUND`, `400 VALIDATION_ERROR` (start date in the future, bad customer ID), `409 ALREADY_SUBSCRIBED` (with the existing `subscriptionId`), or `409 IDEMPOTENCY_MISMATCH`.

### 3. Get subscription — `GET /subscriptions/{id}?asOf=YYYY-MM-DD`

**Client sends** the subscription ID and an optional `asOf` date (default today).

**Service does:** applies the status and billing-date rules above for that day. It reads only and writes nothing.

**Client receives** `200 OK`. For the subscription above, as of 2026-03-05:

```json
{ "subscriptionId": "sub_9f", "status": "ACTIVE",
  "currentPeriodStart": "2026-02-28", "currentPeriodEnd": "2026-03-31",
  "nextBillingOn": "2026-03-31", "endsOn": null, "cancelAtPeriodEnd": false }
```

`nextBillingOn` is `null` when the subscription ends on or before its next billing date. **Fails with** `404 SUBSCRIPTION_NOT_FOUND` or `400 VALIDATION_ERROR` (`asOf` before the start date).

### 4. Cancel — `POST /subscriptions/{id}/cancel`

**Client sends** a mode:

```json
{ "mode": "AT_PERIOD_END" }
```

**Service does:**

- `AT_PERIOD_END` sets the end date to the end of the current period. A trialing subscription therefore ends when its trial does and is never billed.
- `IMMEDIATE` sets the end date to today, so the status is `CANCELED` at once. Refunds are out of scope; the client decides on them.

Repeating the same mode returns the same result and changes nothing. `IMMEDIATE` after `AT_PERIOD_END` is allowed and moves the end date to today. Once a subscription is `CANCELED`, `AT_PERIOD_END` returns `409 ALREADY_CANCELED`. After the end date passes, the customer can subscribe to the same plan again.

**Client receives** `200 OK`:

```json
{ "subscriptionId": "sub_9f", "status": "ACTIVE",
  "endsOn": "2026-03-31", "cancelAtPeriodEnd": true }
```

**Fails with** `404 SUBSCRIPTION_NOT_FOUND`, `400 VALIDATION_ERROR` (unknown mode), or `409 ALREADY_CANCELED`.

### 5. List billing dates — `GET /billing-dates?from=&to=&customerId=`

**Client sends** a date window (both ends included, at most 92 days) and an optional customer ID.

**Service does:** lists every billing date of the caller's subscriptions inside the window, skipping any date on or after a subscription's end date. Results are sorted by date, then by subscription ID. It reads only.

**Client receives** `200 OK`:

```json
{ "from": "2026-03-01", "to": "2026-03-31",
  "charges": [ { "date": "2026-03-31", "subscriptionId": "sub_9f", "customerId": "cus_42",
                 "planId": "pln_3k", "amountCents": 999, "currency": "USD" } ] }
```

**Fails with** `400 VALIDATION_ERROR` (`to` before `from`, or a window longer than 92 days).

## Part 3: Client programs

| Client program | Operations | Purpose |
|---|---|---|
| Climbing-gym front desk | 2, 3, 4 | Signs members up, and at check-in calls operation 3 to admit `ACTIVE` and `TRIALING` members |
| Nightly billing job | 5 | Each night, asks for tomorrow's charges and runs them through the business's own payment processor |
| Newsletter paywall | 3 | Before serving a premium article, checks that the reader's subscription is not `CANCELED` |

Each program has a different purpose and calls a different set of operations. The billing job never subscribes anyone, and the paywall never cancels.

## Part 4: Tools and tests

Java 17, Spring Boot, and an H2 in-memory database for the demo. In the demo, every tool is also a check the agent runs on its own work.

| Category | Tool | Role in the demo |
|---|---|---|
| Build | Maven (`./mvnw verify`) | One command runs every check below; the agent runs it after each change |
| Unit tests | JUnit 5 + AssertJ | Billing-date and status rules, with a fixed `Clock` |
| Mocking | Mockito | Service-layer tests without the database |
| API tests over the network | Postman collection, run with Newman | Sends the example requests from Part 2 to the running service |
| Coverage | JaCoCo | Shows which error paths are still untested |
| Style | Checkstyle (Google Java Style) | Flags code the agent wrote in a different style |
| Static analysis | PMD | Flags unused code and imports the agent leaves behind |
| CI | GitHub Actions | Runs `./mvnw verify` and Newman on every push |
| Project management | GitHub Projects | One card per operation |

These tests are written from the spec before the code exists:

| Test | Expected |
|---|---|
| Billing keeps the month-end anchor | First billing on 2026-01-31 bills next on 02-28, 03-31, 04-30 |
| Leap day, yearly | 2028-02-29 bills next on 2029-02-28, then 2032-02-29 |
| Trial boundary | 7-day trial from 2026-01-24: `TRIALING` on 01-30, `ACTIVE` on 01-31 |
| Cancel at period end | Canceled on 03-05: `ACTIVE` on 03-30, `CANCELED` on 03-31, and 03-31 is not in the billing dates |
| Cancel during a trial | Never appears in the billing dates |
| Immediate cancel, twice | `CANCELED` the same day; the second call returns the same `endsOn` |
| Duplicate subscribe | `409 ALREADY_SUBSCRIBED`; after the first subscription ends, `201` |
| Same `Idempotency-Key`, same body | Same `subscriptionId`; one stored subscription |
| Another client's subscription | `404 SUBSCRIPTION_NOT_FOUND` |
| A 93-day window | `400 VALIDATION_ERROR` |
