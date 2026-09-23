# First ownership skill evaluation

This is a small, controlled evaluation of the ownership review skill. It is
not an implementation of LoanDesk request cancellation. All identities and
records are synthetic and live only in memory.

## Walkthrough

1. Read `requirements.md`. It defines what correct behaviour means for this
   exercise. Real project cancellation policy remains undecided.
2. Inspect `case-a` and `case-b`. They expose the same service API. Neutral
   names avoid announcing an answer to the reviewer.
3. Run the seven shared JUnit tests against both implementations:

   ```powershell
   .\gradlew.bat -p tools/borrower/skill-evaluations/ownership verifyFixtures --no-daemon
   ```

   Run this from the LoanDesk repository root. The standalone build reuses the
   committed wrapper and JUnit version without modifying the application build.
4. Read the terminal output carefully: one example intentionally fails one
   ownership test. `verifyFixtures` succeeds only if case A has exactly that
   failure and case B has seven passes, with no skipped tests or errors.
   Never interpret the underlying `test` task's exit status by itself: this
   evaluation defers expected-failure checking to `verifyFixtures`.
5. Give a fresh reviewer only the skill file, `requirements.md`, and the two
   Java source files, using `review-prompt.txt`. Do not supply tests, build files,
   previous findings or `expected-results.md`. Explicit invocation tests review
   ability; it does not test automatic skill selection.
6. Preserve the original response before revealing the answer sheet. Compare
   it with `expected-results.md`: count detected defects, misses and unsupported
   findings. Different wording is fine; correctness matters.
7. Record your own assessment, any correction to the skill and any rerun as a
   separate result. Do not overwrite an earlier unsuccessful attempt.

## How the executable tests work

Every test starts with the same fresh map of three requests. The success test
checks the entire resulting map, including unrelated records. Rejection tests
check both the false return value and equality of all records before/after the
call. `assertAll` reports both failures instead of stopping after the first.

Covered: valid owner, foreign owner, logged out, wrong role, unknown request,
already cancelled, and repeated cancellation. The missing owner check is the
only deliberate defect. Concurrency, UI, disk saving and malformed seed data
are outside the written exercise requirements.

JUnit supplies evidence about the code. The separate reviewer response supplies
evidence about the skill. Neither substitutes for the other.

## Reports and isolation

Generated reports are under ignored `build/ownership-evaluation/case-a/` and
`case-b/` at the repository root. Gradle clean removes these generated reports;
rerun the fixture command to regenerate them. Dated findings are retained under
`logs/Yikbing-logs/`.

Normal `.\gradlew.bat clean test --no-daemon` does not include this standalone
project. Never move the defective case into `src/main/java` or normal application
tests. This is one unit-level skill evaluation, not an integration/system test
or evidence that all ownership bugs will be detected.

The separate reviewer has no inherited conversation and is instructed to read
only four files. This is procedural blinding, not a filesystem security boundary;
the reviewer shares the workspace. Its observed file access is part of the evidence.
