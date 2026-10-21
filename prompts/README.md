# Demo prompts

Every prompt used in the Oct 22 demo, in slide order. Each file says which slide it belongs to, which mode to run it in, and what to point out live.

| # | File | Slide | What it shows |
|---|---|---|---|
| 01 | [01-v0-spec.md](01-v0-spec.md) | How to write a spec with the agent | The vague starting spec |
| 02 | [02-list-assumptions.md](02-list-assumptions.md) | How to write a spec with the agent | The agent lists what it would have to guess |
| 03 | [03-write-spec.md](03-write-spec.md) | What does a good spec look like? | Decisions turned into SPEC.md |
| 04 | [04-review-spec.md](04-review-spec.md) | What does a good spec look like? | A fresh session stress-tests the spec |
| 04a | [04a-scaffold.md](04a-scaffold.md) | Writing Tests (setup) | The build, coverage and lint set up once |
| 05 | [05-write-tests.md](05-write-tests.md) | Writing Tests | Tests from the spec, before any code |
| 06 | [06-implement.md](06-implement.md) | Demo | Implement one operation at a time |
| 07 | [07-review-pr.md](07-review-pr.md) | Understand and approve the PR | A separate reviewer agent |
| 08 | [08-explain-changes.md](08-explain-changes.md) | Understand and approve the PR | Diagrams and explanations instead of reading every line |
| 09 | [09-visualize-repo.md](09-visualize-repo.md) | AI-generated visualization of our repo | A one-page map of the repo |
| 10 | [10-debug.md](10-debug.md) | Debugging and Updating Features | Diagnose before fixing (planted bug) |
| 11 | [11-change-feature.md](11-change-feature.md) | Debugging and Updating Features | Spec → tests → code; fail-to-pass and pass-to-pass |

## Standing context (slide: Prompting, Skills, and Tools)

| File | Kind | Applies |
|---|---|---|
| [context/AGENTS.md](context/AGENTS.md) | Rules file | Every session in the repo: conventions, approvals, "must make PRs" |
| [context/skills/add-endpoint/SKILL.md](context/skills/add-endpoint/SKILL.md) | Skill | Implementing an endpoint, test first |
| [context/skills/write-tests/SKILL.md](context/skills/write-tests/SKILL.md) | Skill | Writing tests from the spec |
| [context/skills/run-checks/SKILL.md](context/skills/run-checks/SKILL.md) | Skill | Using Maven and Newman, reading reports |
| [context/skills/make-pr/SKILL.md](context/skills/make-pr/SKILL.md) | Skill | Branch, commits and PR description |
| [context/skills/code-review/SKILL.md](context/skills/code-review/SKILL.md) | Skill | The reviewer agent's checklist |

The system prompt for a session is the spec itself: "You are building SubTrack. SPEC.md is the source of truth."

When the build starts, copy `context/AGENTS.md` to the repo root, and copy the skills to wherever your agent loads skills from (for Claude Code: `.claude/skills/<name>/SKILL.md`).
