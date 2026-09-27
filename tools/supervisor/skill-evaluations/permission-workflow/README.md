# `loandesk-supervisor-permission-workflow-review` skill evaluation

This is a small, controlled evaluation of the supervisor permission and
workflow review skill. It is not an implementation of LoanDesk request
approval. All identities and records are synthetic and live only in memory.

## Walkthrough

1. Read `requirements.md`. It defines what correct behaviour means for this
   exercise. It is deliberately narrower than the real request policy.
2. Inspect `case-a` and `case-b`. They expose the same service API. Neutral
   names avoid announcing an answer to the reviewer.
3. Run the eight shared JUnit tests against both implementations:

   ```powershell
   .\gradlew.bat -p tools/supervisor/skill-evaluations/permission-workflow verifyFixtures --no-daemon
   ```

   Run this from the LoanDesk repository root. The standalone build reuses the
   committed wrapper and JUnit version without modifying the application build.
4. Read the terminal output carefully: one example intentionally fails one
   lifecycle test. `verifyFixtures` succeeds only if case A has exactly that
   failure and case B has eight passes, with no skipped tests or errors.
   Never interpret the underlying `test` task's exit status by itself: this
   evaluation defers expected-failure checking to `verifyFixtures`.
5. Give a fresh reviewer only the skill file, `requirements.md`, and the two
   Java source files, using `review-prompt.txt`. Do not supply tests, build
   files, previous findings or `expected-results.md`. Explicit invocation tests
   review ability; it does not test automatic skill selection.
6. Preserve the original response before revealing the answer sheet. Compare
   it with `expected-results.md`: count detected defects, misses and
   unsupported findings. Different wording is fine; correctness matters.
7. Record your own assessment, any correction to the skill and any rerun as a
   separate result. Do not overwrite an earlier unsuccessful attempt.

## How the executable tests work

Every test starts with the same fresh map of three requests: one pending, one
already approved and one collected. The success tests check the decided record
and that unrelated records are untouched. Refusal tests check both the false
return value and equality of all records before and after the call. `assertAll`
reports every failure instead of stopping after the first.

Covered: supervisor approval, supervisor rejection with a reason, rejection
without a reason, logged-out caller, borrower caller, unknown request,
re-approving a terminal request, and re-rejecting a terminal request. The
missing source-status check in `approve` is the only deliberate defect, and it
is planted in one operation rather than both so an overstated finding is
visible. Availability, eligibility, expiry, persistence, concurrency and UI are
outside the written exercise requirements.

JUnit supplies evidence about the code. The separate reviewer response supplies
evidence about the skill. Neither substitutes for the other.

## Reports and isolation

Generated reports are under ignored `build/permission-workflow-evaluation/case-a/`
and `case-b/` at the repository root. Gradle clean removes these generated
reports; rerun the fixture command to regenerate them. Dated findings are
retained under `logs/Supervisor-logs/`.

Normal `.\gradlew.bat clean test --no-daemon` does not include this standalone
project. Never move the defective case into `src/main/java` or normal
application tests. This is one unit-level skill evaluation, not an
integration/system test or evidence that all permission and lifecycle bugs will
be detected.

The separate reviewer has no inherited conversation and is instructed to read
only four files. This is procedural blinding, not a filesystem security
boundary; the reviewer shares the workspace. Its observed file access is part
of the evidence.
