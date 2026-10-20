# 02 — Make the agent show its assumptions

**Slide:** How to write a spec with the agent
**Mode:** plan mode (no file edits). Send right after 01.

```text
Don't write any code yet.

Read the spec above. List every assumption you would have to make to
implement it, grouped under these headings:

- Data and money
- Dates and time
- State and lifecycle (trials, cancellation, status)
- Errors and status codes
- Retries and concurrency
- Who can see what

For each assumption give:
1. The question the spec leaves open.
2. What you would pick if nobody answered.
3. What breaks for a client if your pick is wrong.

Finish with the five questions you most need answered, most important first.
```

**What to point out live:** compare its picks with the "Likely agent guess" column in the planning doc (Appendix B). The month-end question (what is "one month after Jan 31"?) and "is status stored or computed?" are the two worth stopping on.
