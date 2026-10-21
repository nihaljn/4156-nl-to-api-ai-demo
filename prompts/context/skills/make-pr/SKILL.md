---
name: make-pr
description: Package a SubTrack change as a branch, commits and a GitHub pull request. Use whenever work is ready for review.
---

# Make a pull request

1. **Branch** from `main`: `feat/<spec-section>-<short-name>` or `fix/<rule-id>-<short-name>`.
2. **Commit in small steps.** Tests first, then code. Message format: `R7: cancel AT_PERIOD_END sets endsOn to period end`.
3. **Run checks** (`run-checks` skill). Don't open the PR until `./mvnw verify` passes.
4. **Open the PR** with `gh pr create`. The title starts with the spec section number (`5.2: POST /subscriptions`). The description's first line is `Spec section: 5.2`, then:
   - **What:** the spec sections and rule IDs this implements or changes, and which sections are *not* in this PR (whose tests still fail).
   - **How:** two or three sentences, plus a Mermaid sequence diagram of the main request.
   - **Tests:** which tests are new (fail-to-pass) and confirmation that existing tests still pass (pass-to-pass).
   - **Out of scope:** anything you noticed but didn't change.
5. **Never** merge your own PR or push to `main`. A reviewer (human or the reviewer agent) approves it.
