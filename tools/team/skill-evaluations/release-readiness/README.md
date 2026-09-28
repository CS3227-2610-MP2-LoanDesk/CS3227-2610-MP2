# `loandesk-release-readiness-review` skill evaluation

A small, controlled evaluation of the release readiness review skill. The candidates are
synthetic and describe a fictional product; they are not a real LoanDesk
release.

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

Candidate A pairs a cross-platform claim with a distribution carrying only
Windows JavaFX artefacts, and omits the acknowledgement section. Candidate B
is clean. Both candidates carry an honestly recorded limitation, a feature the
guide marks unavailable, which a reviewer must classify as accepted rather
than as a defect; that is the false-positive trap. Both also use a `v`-prefixed
tag against an unprefixed build version, which is conventional and not a
mismatch.

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
