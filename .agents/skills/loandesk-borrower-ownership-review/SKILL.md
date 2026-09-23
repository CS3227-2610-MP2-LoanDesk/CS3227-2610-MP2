---
name: loandesk-borrower-ownership-review
description: Review LoanDesk borrower record access and service operations when adding or changing request, loan, history, edit or cancellation functionality, or auditing borrower permissions. Check service authorization and agreed state rules even when UI checks are bypassed.
---

# Borrower ownership review

Read docs/ProjectContext.md and the relevant requirements, then trace borrower
entry points through services to repositories. Resolve paths from the repository
root. Treat session identity as authoritative rather than caller-supplied owner IDs.

For each affected operation, identify required role, owner, legal states and
expected mutation. Review direct service calls as well as screen wiring:
- Logged-out and wrong-role access is denied before mutation.
- Lists, details, edits and cancellation cannot expose or alter another borrower's records.
- Unknown IDs and stale state have defined, non-leaking failure behaviour.
- Ownership and state are rechecked at the operation, not just on screen load.
- Rejected operations preserve in-memory and persisted state.

Use only agreed rules. Flag unsettled cancellation, clarification, dates or
request/loan boundaries as questions, not asserted defects. Keep enforcement
centralized; do not copy shared rules into controllers or a borrower-only store.
Ask before shared-contract or architecture changes and avoid other-role edits.

Output an operation/role/owner/state table with evidence, then findings with
severity, file/symbol, a direct-call reproduction and missing tests. Distinguish
observed results from hypotheses. File tampering is outside the guarantees of
this local application's service permission checks.

For a review-only request, report without editing. In an authorized implementation
task, fix and test only within its scope. Tests use synthetic users and temporary
storage, never data/loandesk.json. Log meaningful evaluation results and limitations
under logs/Yikbing-logs/; do not claim complete permission coverage from a few tests.
