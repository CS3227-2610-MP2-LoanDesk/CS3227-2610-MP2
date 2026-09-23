---
name: loandesk-borrower-ui-review
description: Review LoanDesk borrower JavaFX UI after implementing or substantially changing a borrower screen, or when assessing borrower UI readiness. Check filters, validation, feedback, status and actions. Does not apply to unrelated role screens or documentation-only work.
---

# Borrower UI review

Read the relevant requirements in docs/ProjectChecklist.md and agreed policies
in docs/ProjectContext.md, plus the changed borrower UI and its service calls.
Use the repository root to resolve these paths. Preserve existing work.

Review changed flows and affected screens for:
- Search and combined filters, clearing filters, empty catalogue and no matches.
- Required inputs and date validation according to agreed rules.
- Understandable errors, accurate status labels and appropriate available actions.
- Failed operations retaining useful input and not displaying success.
- Repeated clicks, stale results, session/logout changes and visible record ownership.

Trace UI actions to services; hiding controls is not proof of authorization.
Keep domain, permission, availability and persistence logic centralized. Do not
invent unresolved policies. Identify any necessary shared-contract change and
ask before implementing it. Avoid supervisor/custodian edits.

Report concrete findings with severity, file/symbol, scenario, expected versus
observed behaviour, and focused verification. Separate code inspection from
executed tests and actual JavaFX interaction; never claim UI execution without
evidence. If no defect is found, state scope and remaining limitations.

A review-only request produces findings without edits. During an authorized
implementation, fix findings within that scope and verify them; this skill does
not independently authorize new features or unrelated changes. Record meaningful
evaluation evidence in logs/Yikbing-logs/ without copying local borrower data.
