---
name: run-checks
description: Run SubTrack's build and checks with Maven and Newman, and read the reports. Use after every change and before saying work is done.
---

# Run the checks

1. `./mvnw -q verify` runs, in order: compile, unit and integration tests (JUnit 5), JaCoCo coverage, Checkstyle, and PMD. It fails on any test failure or rule violation.
2. On failure, read the right report instead of guessing:
   - Tests: `target/surefire-reports/*.txt` (and `failsafe-reports` for integration tests).
   - Coverage: `target/site/jacoco/index.html`, or `jacoco.csv` for numbers.
   - Style: `target/checkstyle-result.xml`.
   - Static analysis: `target/pmd.xml`.
3. API tests over HTTP: start the jar with `./mvnw spring-boot:run` in the background, then run `npx newman run postman/subtrack.postman_collection.json`. Stop the server afterwards.
4. Run a single test class while iterating: `./mvnw -q test -Dtest=BillingCalendarTest`. Always finish with a full `verify`.
5. Report: tests run, failures, line coverage, and any Checkstyle or PMD findings.
