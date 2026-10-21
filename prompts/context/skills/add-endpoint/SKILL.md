---
name: add-endpoint
description: Add or change one SubTrack REST endpoint from its SPEC.md section, test first. Use when asked to implement or modify an endpoint.
---

# Add an endpoint

1. **Read the spec section** (SPEC.md 5.x) and every rule it cites. List the request fields, the check order, the success response and the error codes before writing anything.
2. **Find or write the failing tests.** Tests for this section should already exist under `src/test`. If any case from the spec has no test, write it first (named after its rule) and confirm it fails.
3. **Domain logic first.** Put rule logic (dates, status, cancel) in `domain` as plain Java with no Spring, so unit tests run fast.
4. **Then service, then controller.** Controller: parse, validate in the spec's check order, call the service, map to the response DTO. Errors only via `ApiException` with codes from SPEC.md section 6.
5. **Add the Postman request** for the spec's example to `postman/subtrack.postman_collection.json`, with tests for status code and key fields.
6. **Run checks** with the `run-checks` skill. Fix failures in the code, never in the tests.
7. **Report:** files changed, rule IDs implemented, and the `./mvnw verify` summary.
