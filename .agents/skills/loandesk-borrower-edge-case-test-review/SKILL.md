---
name: loandesk-borrower-edge-case-test-review
description: Review edge cases and tests whenever LoanDesk borrower behaviour or tests are added or changed, or a borrower milestone audit is requested. Run relevant tests, investigate failures and check successful operations, safe rejection, boundaries and missing coverage. Excludes unrelated changes.
---

# Borrower edge-case and test review

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
