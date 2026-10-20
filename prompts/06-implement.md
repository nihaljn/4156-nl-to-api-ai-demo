# 06 — Implement, one operation at a time

**Slide:** Demo
**Mode:** normal, with the rules file (`context/AGENTS.md`) and the `add-endpoint` skill loaded.

```text
Implement section 5.1 of SPEC.md (POST /plans) using the
add-endpoint skill.

Make the tests for 5.1 pass, including its idempotency and validation
cases. Don't edit, delete or disable anything under src/test. If a test
looks wrong, stop and tell me which spec rule it contradicts instead of
changing it.

When ./mvnw verify passes, stop and give me:
- the files you changed,
- the rule IDs each change implements,
- the verify summary (tests run, failures, coverage).
```

Repeat for 5.2, 5.3, 5.4 and 5.5. Start with 5.1 because every other test creates its plan through it. If live generation runs long, switch to the pre-built branch for the remaining operations.
