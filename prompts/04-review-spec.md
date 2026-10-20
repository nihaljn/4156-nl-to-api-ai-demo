# 04 — Stress-test the spec before any code

**Slide:** What does a good spec look like?
**Mode:** plan mode, ideally a fresh session that didn't write the spec.

```text
You are going to implement SPEC.md tomorrow and can't ask anyone
questions. Read it and report:

1. Every sentence two careful engineers could implement differently.
   Quote it and give both readings.
2. Every input whose type, range or unit isn't stated.
3. Every endpoint missing a success example, an error code, or the
   behavior on retry.
4. Any rule that no acceptance test checks.
5. Any acceptance test whose expected result doesn't follow from the rules.

Don't suggest new features. Only report gaps in what's already there.
```

**Why a fresh session:** the agent that wrote the spec fills gaps from its own memory without noticing. A new session only has the file.
