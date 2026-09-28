---
name: loandesk-persistence-failure-test-review
description: Review LoanDesk persistence and save-failure handling when changing storage, repositories, multi-record writes, checkout, return, damage or maintenance flows, or when auditing whether tests detect inconsistent loan and equipment records. Check that a failed or partial save leaves a consistent prior state and that tests actually inject failure.
---

# Persistence-failure test review

Read docs/DeveloperGuide.md for the storage design, then identify every
operation that writes more than one related record. Resolve paths from the
repository root. The question this review answers is not whether a write works,
but what the stored and in-memory state looks like when a write does not.

For each multi-record operation, establish which records must change together,
then check both the production path and its tests:

- Related updates are committed together. A damaged return changes the loan and
  the equipment; either both apply or neither does.
- A failed save leaves the prior state intact, in persistence and in memory.
  In-memory state must not be mutated before the write that is supposed to
  justify it succeeds.
- A rejected or failed operation reports the failure rather than returning
  success with unsaved changes.
- Reload after restart observes exactly what the successful writes claimed, and
  a failed write left nothing behind.
- Concurrent or stale writers are refused rather than silently overwriting a
  newer state.
- Errors surface enough to diagnose without leaking credentials or password
  material.

Then judge the tests specifically. A suite that only exercises the happy path
does not cover this area regardless of how many assertions it contains:

- A test injects a real save failure rather than asserting a thrown exception
  from validation that never reaches storage.
- The failure test asserts the whole stored state is unchanged, not only that
  the call failed.
- Restart persistence is proved with records the test itself wrote, not with
  seeded data that would exist anyway.

Use only agreed rules. Flag unsettled recovery, reset or schema-version
behaviour as a question rather than a defect. Keep storage concerns behind the
repository boundary; do not push persistence handling into controllers.

Report a table of multi-record operations with the records each writes and the
failure behaviour observed, then findings with severity, file/symbol, the
injected-failure scenario that reproduces the inconsistency, and the missing
test. State clearly whether you executed any test or only inspected code.

For a review-only request, report without editing. In an authorized
implementation task, fix and test only within its scope. Tests use synthetic
records and temporary storage, never the local data/loandesk database. Log
meaningful evaluation results and limitations under logs/. Passing failure
tests for one operation says nothing about the others.
