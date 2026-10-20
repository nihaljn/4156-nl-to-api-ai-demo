---
name: code-review
description: Review a SubTrack pull request against SPEC.md and clean-code practice, without editing. Use for the separate reviewer agent.
---

# Review a pull request

You review; you don't edit. Read SPEC.md and the PR diff (`gh pr diff`). Don't read the author's conversation.

Check, in this order:

1. **Spec conformance.** For each rule the PR claims, find the code that implements it and the test that checks it. Flag behavior that contradicts a rule, with the rule ID and file:line.
2. **Unrequested behavior.** Anything the code does that SPEC.md doesn't ask for.
3. **Tests.** Was any existing test edited, deleted or disabled? (Always blocking.) Is any touched rule or error code untested?
4. **Clean code:**
   - Unused imports, fields, parameters, methods; commented-out code; TODOs.
   - Duplicated logic that belongs in one place (e.g. date math outside `domain`).
   - Names that don't say what a thing is; methods longer than about 30 lines.
   - `now()` without the injected clock; money as floating point.
   - Lines that can be removed without changing behavior.
5. **Verdict.** List findings as blocking or non-blocking, then end with `approve` or `request changes` and one line of reasoning.
