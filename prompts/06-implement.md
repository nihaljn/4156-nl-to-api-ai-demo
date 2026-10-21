# 06 — Implement, one operation at a time

**Slide:** Demo
**Mode:** normal, after `04a-scaffold.md` and `05-write-tests.md`, with the rules file (`context/AGENTS.md`) and the `make-pr` skill loaded.

```text
Implement section 5.1 of SPEC.md (POST /plans).

Make the existing tests for 5.1 pass. Write no new tests, and don't edit,
delete or disable anything under src/test. If a test looks wrong, or the
spec has a case no test covers, stop and tell me instead of changing or
adding tests.

Run only this section's tests while you work (./mvnw test -Dtest=...).
Then run ./mvnw verify. Tests for endpoints I haven't asked for yet
still fail, and that is expected: don't fix them, don't stub the
endpoints, and don't touch pom.xml or config/.

When this section's tests pass and nothing else fails except tests for
later sections, use the make-pr skill: branch from main, commit in small
steps, and open a pull request. Treat that as the verify gate for the PR,
and list the later-section failures in its description. Don't merge it
and don't push to main.

Then stop and give me:
- the PR link,
- the files you changed,
- the rule IDs each change implements,
- the verify summary: tests run and failures, split into this section's
  tests and later sections' tests, plus line coverage from the JaCoCo
  report (report it, no target).
```

**One section per run.** This prompt covers 5.1 only. Stop after the PR is open. Next run `07-review-pr.md` on that PR, and only once it is approved and merged do you come back and run this prompt again for the next section: 5.2, then 5.3, 5.4, 5.5, changing only the section number. Always start with 5.1, because every other test creates its plan through it. The failing count shrinks after each section, and the full `./mvnw verify` should be green only after 5.5. If live generation runs long, switch to the pre-built branch for the remaining sections.
