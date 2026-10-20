# 07 — A separate reviewer agent

**Slide:** How do we understand and approve the AI-generated PR?
**Mode:** a fresh session with the `code-review` skill loaded. It sees the diff and SPEC.md, not the author's conversation.

```text
Review the open pull request against SPEC.md using the code-review skill.
Don't edit any files.

Report, in this order:
1. Spec violations: any behavior that contradicts a rule. Cite the rule
   ID and the file and line.
2. Spec gaps: behavior the code has that the spec doesn't mention.
3. Test gaps: rules or error codes this PR touches that no test checks.
4. Clean-code issues: unused code, duplicated logic, unclear names, and
   lines that can be removed without changing behavior.

For each finding, say whether it should block the merge. End with
"approve" or "request changes" and a one-line reason.
```

**Two ways to use the output:** paste it back to the author agent as feedback, or read it yourself as a summary before you open the diff.
