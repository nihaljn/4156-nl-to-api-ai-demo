# 07 — A separate reviewer agent

**Slide:** How do we understand and approve the AI-generated PR?
**Mode:** normal, in the author's session or any session. The review itself runs in a subagent that starts cold: it gets SPEC.md, the `code-review` skill and the PR, and nothing from the author's conversation.

```text
Create a subagent to review the open pull request. Give it only:
- SPEC.md,
- the code-review skill,
- the PR number, so it can read the description with gh pr view and the
  diff with gh pr diff.

Do not give it this conversation, your summary of the change, or any
write access. It must not edit files. Tell it:

Review the open pull request against SPEC.md using the code-review skill.
Don't edit any files.

The PR description says which spec sections this PR covers. Judge the
diff against those sections, the rules they cite, and section 2. Don't
report sections the description says are not in this PR as missing, but
do report any claim in the description that the diff doesn't back up,
and any behavior in the diff outside the stated scope.

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

**Why a subagent:** it can't be talked into approving. It has only the spec and the diff, so it can't lean on the author's explanations, and it has no tools that edit.

**Two ways to use the output:** paste it back to the author agent as feedback, or read it yourself as a summary before you open the diff.

**Then:** if the verdict is "request changes", send the findings back to the author agent and review again. Only when the PR is approved and merged do you go back to `06-implement.md` for the next section.
