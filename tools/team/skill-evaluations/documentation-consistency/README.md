# `loandesk-documentation-consistency-review` skill evaluation

A small, controlled evaluation of the documentation consistency review skill. The candidates are
synthetic and describe a fictional product; they are not a real LoanDesk
guide.

## Walkthrough

1. Read `requirements.md`. It defines what correct means for this exercise.
2. Read `case-a.md` and `case-b.md`. Each is the complete supplied evidence
   for one candidate. Neutral names avoid announcing an answer.
3. Give a fresh reviewer only the skill file, `requirements.md`, `case-a.md`
   and `case-b.md`, using `review-prompt.txt`. Do not supply
   `expected-results.md`, prior findings or project logs.
4. Preserve the original response before revealing the answer sheet, then
   compare against `expected-results.md`.
5. Record your assessment, any correction to the skill and any rerun as a
   separate result. Do not overwrite an earlier unsuccessful attempt.

## What the fixture tests

Candidate A's guide says an approval note is required while the code accepts a
blank one; candidate B's guide says it is optional, matching the code. Both
candidates carry two traps: a documented 30-day window that genuinely matches
the constant, and a feature correctly documented as unavailable that is
genuinely absent. Reporting either as a defect is a false positive.

**There is no executable ground truth for this fixture.** Unlike the Java
fixture pairs, nothing here can be run, so `expected-results.md` is the only
ground truth and the assessment rests entirely on a human comparing the
response against it. Treat its result as weaker evidence than a fixture with
JUnit ground truth, and say so wherever it is cited.

## Isolation

The candidates describe a fictional product so the reviewer cannot resolve a
question by reading the real repository. The reviewer shares the workspace,
so the blinding is procedural rather than enforced; its observed file access
is part of the evidence. Dated findings are retained under `logs/`.
