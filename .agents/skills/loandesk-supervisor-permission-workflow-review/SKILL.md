---
name: loandesk-supervisor-permission-workflow-review
description: Review LoanDesk permission and workflow enforcement when adding or changing supervisor review, approval, rejection, booking cancellation, request lifecycle, expiry or role-permission functionality, or auditing who may perform which operation. Check authorization and legal status transitions in services even when UI controls are hidden or bypassed.
---

# Supervisor permission and workflow review

Read docs/DeveloperGuide.md for the permission matrix and the request lifecycle,
then trace each affected entry point from its screen through its service to
persistence. Resolve paths from the repository root. Treat session identity as
authoritative; a caller-supplied role, username or actor field is never evidence
of authorization.

For each affected operation, identify the required permission, the legal source
statuses, the resulting status and the metadata the transition must record.
Review direct service calls as well as screen wiring:

- Logged-out and wrong-role callers are refused before any mutation, including
  when the screen would never offer the action.
- Every status change is checked against the shared lifecycle rather than a
  status comparison written inline at the call site.
- Terminal statuses cannot be re-decided, reopened or decided twice.
- Rules that depend on other records, such as availability and borrower
  eligibility, are rechecked at decision time rather than trusted from the
  state captured when the request was submitted.
- A rejected operation leaves in-memory and persisted state unchanged, and a
  failed save leaves no partially applied decision.
- Required decision metadata, the actor, the time and the reason where the
  policy requires one, is recorded and preserved by later transitions.

Use only agreed rules. The recorded request policy and the permission matrix
are authoritative; flag anything unsettled as a question rather than an asserted
defect. Keep enforcement in the shared permission and lifecycle services; do not
copy a role check into a controller or restate transition rules per role. Ask
before shared-contract changes and avoid editing another role's code.

Report an operation/permission/source-status/target-status table with evidence,
then findings with severity, file/symbol, a direct-call reproduction that
bypasses the UI, and the missing test. Distinguish what you inspected from what
you executed. A permission matrix constrains this application's own services; it
is not a defence against direct database or file tampering.

For a review-only request, report without editing. In an authorized
implementation task, fix and test only within its scope. Tests use synthetic
users and temporary storage, never the local data/loandesk database. Log
meaningful evaluation results and limitations under logs/Supervisor-logs/; do
not claim complete authorization or lifecycle coverage from a few tests.
