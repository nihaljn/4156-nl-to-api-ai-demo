# Rules for coding agents — SubTrack

<!-- Copy this to the repo root as AGENTS.md (or CLAUDE.md) when the build starts.
     It applies to every session in the repo. -->

## Source of truth
- `SPEC.md` decides behavior. If code and spec disagree, the spec wins.
- If the spec is unclear or silent, stop and ask. Don't guess, and don't add features it doesn't list.
- Cite rule IDs (R1–R8) in commit messages, test names and PR descriptions.

## Stack
- Java 17, Spring Boot 3, Maven wrapper. Package root: `edu.columbia.subtrack`.
- Layers: `api` (controllers, DTOs, error handler) → `service` → `domain` (pure logic, no Spring) → `repository`.
- No new dependencies without asking.

## Code conventions
- Dates: `LocalDate` in UTC, from the injected `java.time.Clock` bean. Never call `now()` without the clock.
- Money: `long` cents. Never `double`, `float` or `BigDecimal`.
- Errors: throw `ApiException(code, message, field)`; one `@RestControllerAdvice` turns it into `{error, message, field}`. Use only the codes in SPEC.md section 6.
- No commented-out code, TODOs, unused imports or unused parameters left behind.

## Tests
- Never edit, delete, or disable a test to make it pass. If a test looks wrong, stop and say which rule it contradicts.
- New tests are named after the rule they check, e.g. `R2_billingDateKeepsMonthEndAnchor`.

## Approvals
- Ask before: changing SPEC.md, editing any existing test, adding a dependency, editing `pom.xml` or `config/`, or deleting files.
- Every change goes through a pull request (see the `make-pr` skill). Never push to `main`.

## Done means
- `./mvnw verify` passes (tests, JaCoCo, Checkstyle, PMD), and you've shown its summary.
