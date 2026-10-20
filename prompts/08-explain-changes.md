# 08 — Understand the change without reading every line

**Slide:** How do we understand and approve the AI-generated PR?
**Mode:** plan mode (read-only).

```text
Explain this pull request to a reviewer who hasn't read the code.
Don't change anything.

1. A Mermaid sequence diagram of POST /subscriptions/{id}/cancel, from
   the controller down to the repository and back.
2. A Mermaid flowchart of the cancel rule (R7): each state, each mode,
   and the result.
3. A table: endpoint → controller method → service method → rule IDs it
   implements → tests that cover it.
4. For each changed file, two or three sentences on what it does and
   why it changed.
5. What was added, modified or removed compared with the previous
   iteration (the last merged commit), grouped by spec section.
6. Anything in the code that the spec doesn't ask for.
```

**Tip:** ask for the diagrams as an artifact or an HTML page if your tool can render one; Mermaid in a PR description also renders on GitHub.
