# `loandesk-cross-role-scenario-review` skill evaluation

A small, controlled evaluation of the cross-role scenario review skill. It is not an
implementation of LoanDesk request fulfilment. All identities and records are synthetic
and live only in memory.

## Walkthrough

1. Read `requirements.md`. It defines what correct behaviour means for this
   exercise and is deliberately narrower than the real project policy.
2. Inspect `case-a` and `case-b`. They expose the same service API. Neutral
   names avoid announcing an answer to the reviewer.
3. Run the 8 shared JUnit tests against both implementations:

   ```powershell
   .\gradlew.bat -p tools/team/skill-evaluations/cross-role verifyFixtures --no-daemon
   ```

   Run this from the LoanDesk repository root. The standalone build reuses the
   committed wrapper and JUnit version without modifying the application build.
4. One example intentionally fails one test. `verifyFixtures` succeeds only if
   case A fails exactly `checkoutLeavesTheRequestAndItsLoanAgreeing` and case B passes all 8, with no
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

The seeded defect is only visible across a role boundary: every operation is
internally valid, and the pair of records two roles share disagrees. Exactly
one test follows the request into the borrower view after checkout, so a
reviewer that inspects `checkout` alone can plausibly miss it.

Covered: approval, approval role gate, loan creation and its links, the
request/loan agreement after checkout, the approved-request precondition, the
custodian gate on checkout, return closing only its own loan, and return
preconditions. Persistence, expiry, availability and concurrency are outside
the written requirements.

JUnit supplies evidence about the code. The separate reviewer response
supplies evidence about the skill. Neither substitutes for the other.

## Reports and isolation

Generated reports are under ignored `build/cross-role-evaluation/case-a/` and `case-b/`
at the repository root. Normal `.\gradlew.bat clean test --no-daemon` does not
include this standalone project. Never move the defective case into
`src/main/java` or the application tests. This is one unit-level skill
evaluation, not an integration or system test, and not evidence that all
defects of this class will be detected.

The separate reviewer has no inherited conversation and is instructed to read
only four files. This is procedural blinding, not a filesystem security
boundary; the reviewer shares the workspace. Its observed file access is part
of the evidence. Dated findings are retained under `logs/`.
