# 06 — Implement, one operation at a time

**Slide:** Demo
**Mode:** normal, after `04a-scaffold.md` and `05-write-tests.md`, with the rules file (`context/AGENTS.md`) and the `add-endpoint` skill loaded.

```text
Implement section 5.1 of SPEC.md (POST /plans) using the
add-endpoint skill.

Make the tests for 5.1 pass, including its idempotency and validation
cases. Don't edit, delete or disable anything under src/test. If a test
looks wrong, stop and tell me which spec rule it contradicts instead of
changing it.

Run only this section's tests while you work (./mvnw test -Dtest=...).
Then run ./mvnw verify. Tests for endpoints I haven't asked for yet
still fail, and that is expected: don't fix them, don't stub the
endpoints, and don't touch pom.xml or config/.

Stop and give me:
- the files you changed,
- the rule IDs each change implements,
- the verify summary: tests run and failures, split into this section's
  tests and later sections' tests, plus line coverage from the JaCoCo
  report (report it, no target).
```

Repeat for 5.2, 5.3, 5.4 and 5.5, changing the section number. Start with 5.1 because every other test creates its plan through it. The failing count shrinks after each operation, and the full `./mvnw verify` should be green only after 5.5. If live generation runs long, switch to the pre-built branch for the remaining operations.
