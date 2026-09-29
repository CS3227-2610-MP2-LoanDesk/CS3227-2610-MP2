---
name: loandesk-custodian-persistence-condition-review
description: Review LoanDesk equipment condition, return/lost/recovery, H2 save, schema, or persistence changes. Check atomic multi-record saves, rollback, stale writes, restart persistence, and condition-to-availability and eligibility effects.
---

# Custodian persistence and condition review

Read the applicable approved custodian requirements, persistence contract,
`DatabaseDataStore`, and the affected condition and availability services.
Preserve the agreed transactional save and stale-revision behavior unless the
team explicitly changes that contract. Inspect service behavior, persistence
mapping and schema updates together; a UI indication is not evidence that a
state change saved.

For each changed operation, check that request, loan and equipment changes are
saved as one complete snapshot and either all persist or all roll back. Verify
that stale snapshots are rejected without overwriting newer data, and that a
fresh store reload preserves the changed condition, linked records and status.
Test database failures with a temporary H2 store rather than `data/loandesk`.

Check the agreed mappings: good return permits availability; damaged,
under-maintenance and lost equipment remain unavailable; recovery restores the
loan to ACTIVE and sets equipment to GOOD so the custodian can record the
observed return condition separately. Recovery has no return date and remains
ON_LOAN until that return. Check that availability and borrower eligibility
consume the shared condition/availability result rather than a custodian-only
copy of the rule.

Report a table with operation, records changed atomically, condition/status
effect, and evidence. Then report findings with severity, file/symbol, a
failure or restart reproduction, and the missing test. Distinguish inspected
code from executed tests. Flag unagreed schema or condition policy as a question
and do not add maintenance notes or records to this MVP.
