---
name: loandesk-borrower-implementation-preflight
description: Prepare a meaningful LoanDesk borrower feature before implementation by checking project policy, the review-gap registry, ownership, persistence, tests and UI evidence needs.
---

# Borrower implementation preflight

Use this skill before implementing a meaningful borrower feature, milestone
slice, service operation, persistence change or borrower screen. Do not use it
for typo fixes, formatting-only changes, routine documentation edits or an
ordinary test rerun.

Read these from the repository root:

- `docs/ProjectContext.md`
- `docs/ProjectChecklist.md`
- `docs/BorrowerMilestones.md`
- `docs/ReviewGapRegistry.md`
- the relevant detailed borrower review skills when their areas apply

## Preflight steps

1. State the intended feature boundary and the milestone exit criteria.
2. Identify whether the change affects UI, borrower services, shared domain,
   permissions, availability, persistence, tests or documentation.
3. Match the planned change against the registry's resolved gaps and deferred
   limitations. Convert applicable patterns into prevention checks before
   coding; do not silently treat deferred recommendations as requirements.
4. Confirm the owner identity comes from the active session and list the
   service-layer checks needed for logged-out, wrong-role, foreign-owner,
   unknown-ID and stale-state calls.
5. List the focused success, rejection, boundary, failed-save and restart
   tests needed for the feature. Identify which UI checks must be manual
   because no automated JavaFX interaction harness exists.
6. Flag any unresolved product policy, shared contract or architecture choice
   before implementation. Do not invent a cross-role rule or edit another
   role's feature to complete the preflight.

## Output

Produce a short preflight record containing:

- scope and assumptions;
- applicable registry gaps and prevention checklist;
- planned tests and GUI evidence;
- shared-contract coordination needed;
- documentation and session-log updates required;
- explicit blockers or confirmation that implementation may begin.

The preflight is planning evidence, not proof that the implementation works.
After implementation, use the focused review skills and the
`loandesk-borrower-change-completeness-review` skill. That later review still
requires an independent panel before a meaningful feature commit.

Keep the skill advisory and local. It must not launch agents, modify Git hooks,
make network calls, commit, push, change local data or replace user approval.
