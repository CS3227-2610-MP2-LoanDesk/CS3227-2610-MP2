---
name: loandesk-cross-role-scenario-review
description: Review LoanDesk state that crosses role boundaries when changing the borrower request, supervisor decision or custodian fulfilment path, or when checking the submit-approve-checkout-return acceptance journey. Check that records the three roles share stay consistent with each other, not merely valid on their own.
---

# Cross-role scenario review

Read docs/DeveloperGuide.md for the request lifecycle and the permission
matrix, then follow one record through every role that touches it rather than
reviewing each role's code in isolation. Resolve paths from the repository
root. Most defects of this kind are invisible inside a single role: each
operation looks correct, and the pair of records they leave behind disagrees.

Trace the journey end to end: a borrower submits, a supervisor approves or
rejects, a custodian checks out and returns. For each handover, identify what
the receiving role must be able to rely on, and check:

- A request and the loan created from it agree on borrower, equipment, dates
  and status, and each links to the other where the contract says it must.
- A status change that should release or claim a shared resource actually
  changes what the other roles observe, including derived availability.
- Rules evaluated in one role are rechecked at the moment another role acts on
  them, rather than trusted from when the record was first created.
- A record reachable from two roles cannot be left in a state only one of them
  considers legal.
- State survives a restart in the same shape every role expects, not only in
  the shape the writing role expects.
- Terminal states stay terminal when a different role acts on them.

Use only agreed rules. Where two roles' expectations genuinely conflict, report
it as an unsettled shared contract needing the owners' agreement, not as one
role's defect. Do not resolve a cross-role disagreement by changing another
owner's code; name the owners who must agree.

Report a journey table with one row per handover, giving the acting role, the
records written, and what the next role then observes. Follow it with findings
carrying severity, file/symbol, the specific two-role sequence that produces
the inconsistency, and the missing test. State which sequences you executed and
which you only inspected.

For a review-only request, report without editing. In an authorized
implementation task, fix and test only within its scope, and coordinate any
shared-contract change with its owner first. Tests use synthetic records and
temporary storage, never the local data/loandesk database. Log meaningful
evaluation results and limitations under logs/. Two roles agreeing in one
scenario does not establish that the whole journey is consistent.
