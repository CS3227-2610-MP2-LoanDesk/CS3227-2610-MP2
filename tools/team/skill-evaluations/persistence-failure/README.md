# `loandesk-persistence-failure-test-review` skill evaluation

A small, controlled evaluation of the persistence-failure test review skill. It is not an
implementation of LoanDesk storage or returns. All identities and records are synthetic
and live only in memory.

## Walkthrough

1. Read `requirements.md`. It defines what correct behaviour means for this
   exercise and is deliberately narrower than the real project policy.
2. Inspect `case-a` and `case-b`. They expose the same service API. Neutral
   names avoid announcing an answer to the reviewer.
3. Run the 6 shared JUnit tests against both implementations:

   ```powershell
   .\gradlew.bat -p tools/team/skill-evaluations/persistence-failure verifyFixtures --no-daemon
   ```

   Run this from the LoanDesk repository root. The standalone build reuses the
   committed wrapper and JUnit version without modifying the application build.
4. One example intentionally fails one test. `verifyFixtures` succeeds only if
   case A fails exactly `aFailedSaveLeavesTheInMemoryStateUnchanged` and case B passes all 6, with no
   errors or skips. Never read the underlying `test` task's exit status by
   itself; expected-failure checking is deferred to `verifyFixtures`.
5. Give a fresh reviewer only the skill file, `requirements.md` and the two
   Java source files, using `review-prompt.txt`. Do not supply tests, build
   files, previous findings or `expected-results.md`. Explicit invocation
   tests review ability; it does not test automatic skill selection.
6. Preserve the original response before revealing the answer sheet, then
   compare against `expected-results.md`: count detected defects, misses and
   unsupported findings. Different wording is fine; correctness matters.
7. Record your assessment, any correction to the skill and any rerun as a
   separate result. Do not overwrite an earlier unsuccessful attempt.

## What the fixture tests

The seeded defect is on the failure path only: the happy path, the refusal
paths and exception propagation are all correct in both cases, so a reviewer
that checks only that the operation works will miss it. The injected failure
is a real write failure through the Sink, not a validation error that never
reaches storage.

Covered: a successful damaged return, both record sets committed in one write,
in-memory state after a failed write, nothing committed after a failed write,
an unknown loan, and an already-returned loan. Restart, schema migration and
concurrency are outside the written requirements.

JUnit supplies evidence about the code. The separate reviewer response
supplies evidence about the skill. Neither substitutes for the other.

## Reports and isolation

Generated reports are under ignored `build/persistence-failure-evaluation/case-a/` and `case-b/`
at the repository root. Normal `.\gradlew.bat clean test --no-daemon` does not
include this standalone project. Never move the defective case into
`src/main/java` or the application tests. This is one unit-level skill
evaluation, not an integration or system test, and not evidence that all
defects of this class will be detected.

The separate reviewer has no inherited conversation and is instructed to read
only four files. This is procedural blinding, not a filesystem security
boundary; the reviewer shares the workspace. Its observed file access is part
of the evidence. Dated findings are retained under `logs/`.
