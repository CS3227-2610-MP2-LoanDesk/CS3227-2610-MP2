# Borrower Request UX Design — 24 September 2026

## Scope

Recorded the borrower-facing design discussion for the future request, request
history and loan-summary screens. No application code, database schema or tests
were changed in this session.

## Agreed decisions

- The request form contains one equipment selection, a purpose and date-only
  start/due fields. Start defaults to today; due defaults to fourteen days
  later; shorter periods are allowed.
- Purpose uses common dropdown options plus `Other`, which requires an
  explanation. The request stores the resulting purpose text for the MVP.
- The catalogue remains browseable, but only `AVAILABLE` equipment is
  selectable. The service reloads and rechecks eligibility and availability at
  submission time. One borrower may have only one pending request for an item.
- Successful login opens the dashboard. Overdue blockers are most prominent,
  followed by approved collection reminders, active loans and requests. All
  active eligibility blockers are listed in a text-and-colour warning banner;
  request actions are disabled while catalogue browsing remains available.
- Active loans and loan history use separate tables. Requests show active
  records before terminal history. Empty states are explicit.
- Borrowers may edit only purpose and dates on eligible pending requests.
  Rejected requests remain read-only history and require a new request.
  Cancellation requires confirmation, a reason dropdown and an explanation
  for `Other`; the reason is persisted.
- Invalid input stays on the form with inline messages. Successful submission
  requires an `OK` confirmation dialog before returning to the dashboard.
- A changed start date updates the default due date; an intentionally shortened
  duration is preserved when possible, with validation for invalid dates.

## Evidence and next step

This was a design-only session. Milestone 3's input/UX agreement is now checked
in `docs/BorrowerMilestones.md`, but its implementation and test items remain
unchecked. The next step is to agree the smallest shared request domain and H2
repository contract with the group before coding.
