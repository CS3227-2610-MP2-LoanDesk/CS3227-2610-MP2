---
name: loandesk-borrower-edge-case-test-review
description: Review edge cases and tests whenever LoanDesk borrower behaviour or tests are added or changed, including borrower authentication, password hashing/sign-up/login, credential persistence, database saves, catalogue filtering, requests or loans, or when a borrower milestone audit is requested. Run relevant tests, investigate failures and check successful operations, safe rejection, boundaries and missing coverage. Excludes unrelated changes.
---

# Borrower edge-case and test review

## Invocation trigger

Invoke this skill for any borrower change that touches authentication, password
validation or hashing, sign-up/login persistence, a `DataStore` or repository,
borrower UI behavior, filtering, request/loan behavior, or tests for those
areas. This applies even when the change is described as a small storage,
refactoring or documentation update if the changed tests or code exercise the
behavior. It also applies when the user asks whether a borrower milestone is
complete.

This skill is automatically discoverable, but it is not a background file
watcher or a Git hook. When the trigger applies, the active Codex turn must
explicitly run the review or report why it was unavailable; passing tests alone
does not count as an edge-case review.

Read the applicable requirements and agreed rules in docs/ProjectContext.md and
docs/ProjectChecklist.md, then inspect changed borrower functions, affected
callers and tests. Resolve paths from the repository root. For an explicitly
requested milestone audit, cover all implemented borrower behaviours.

Build a compact behaviour/case/expected-result/test/evidence table. Consider
only applicable categories:
- Valid operations succeed and persist as promised.
- Empty, whitespace, length boundaries and unexpected input.
- Reversed, past, same-day and exact-boundary dates under agreed policy.
- Combined filters, case handling, clearing filters and no matches.
- Unknown IDs, wrong owner, logged-out/wrong-role direct service calls.
- Illegal states, repeated cancellation, resubmission and double submission.
- Stale availability or request state between display and action.
- Failed saves: useful error, no false success or partial state, input retained.
- Restart persistence where the affected behaviour requires it.

For borrower authentication and credential changes, also check when applicable:
- Correct login, wrong password, unknown username, null/blank input and
  case-insensitive username handling.
- Minimum and maximum password lengths, duplicate sign-up and repeated submit.
- Hash-only storage, salt/algorithm metadata, and absence of plaintext secrets.
- Failed credential saves: no in-memory account before persistence succeeds,
  no partial database state, useful retry behavior and restart consistency.

For persistence or snapshot changes, use controlled failure injection where
possible and check rollback, stale snapshots and concurrent writers when the
store can be shared by more than one service instance.

Retain ordinary test review: run relevant existing tests, inspect failures and
assertion quality, and check that valid cases still pass. Do not merely add
negative tests. Prefer observable behaviour over tests mirroring private helpers
or getters. Use synthetic data, temporary folders and controlled failure injection.
Never alter personal data or leave deliberate defects in production code.

Flag unresolved policy rather than choosing it. Reuse centralized rules and flag
shared issues for coordination; ask before changing shared contracts or architecture.
Do not modify other-role features to make a borrower review pass.

A review-only task reports findings without edits. When implementation/testing
is authorized, add focused missing tests, fix failures within scope and rerun the
affected checks. Follow the project's required clean test command before and after
changes: .\gradlew.bat clean test --no-daemon on Windows.

Report executed commands and results, failures, uncovered cases and limitations.
Mark proposed tests as proposed, not passed. No claim of exhaustive coverage.
Record meaningful skill evaluations under logs/Yikbing-logs/ with prompt, fixture,
findings, verification, missed defects and false positives when known.

Every triggered review must state one result explicitly: `RUN` with commands and
findings, `UNAVAILABLE` with the blocking reason, or `NOT APPLICABLE` with the
scope reason. Do not describe an unavailable or skipped review as passed.
