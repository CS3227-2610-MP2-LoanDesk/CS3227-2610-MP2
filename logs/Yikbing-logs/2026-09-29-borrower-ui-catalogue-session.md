# Borrower UI and catalogue update — 29 September 2026

## Scope

Updated borrower authentication, dashboard, catalogue, request form, loan
history and request-history presentation. The catalogue now exposes shared,
derived availability to borrower views; it remains read-only. No persistence,
workflow state, permission matrix or custodian behaviour changed.

## Verification

- `./gradlew --no-daemon test` was run before the review fixes; it compiled the
  updated JavaFX code but exposed a new catalogue fixture failure.
- `./gradlew --no-daemon test --tests loandesk.CatalogueServiceTest` passed
  after the fixture was corrected. The test now covers all four derived
  availability statuses, logged-out/wrong-role rejection and ordering.
- `git diff --check` passed before staging. JavaFX interaction was not executed;
  the repository has no JavaFX interaction harness.

## Independent review panel

### UI/interaction reviewer — RUN

Prompt: review the staged borrower UI and catalogue diff, without edits,
commits, pushes or local-data changes; assess the visual-system flow, tables,
availability selection and date controls.

Response: found two P2 issues. Lost loans appeared in the active-loans table,
and the due-date picker allowed a date beyond the agreed fourteen-day limit.
Confirmed that catalogue availability sorting, unavailable-item disabling,
borrower-visible names and past-date disabling were otherwise correctly wired.

Resolution: dashboard filtering now selects only `ACTIVE` loans. The due-date
calendar disables dates before the selected start date and after fourteen days.

### Behaviour/test reviewer — RUN

Prompt: review the staged catalogue/service and borrower behaviour/tests
without edits, commits, pushes or local-data changes; assess authorization,
availability derivation/sorting and existing request-service boundaries.

Response: no blocking behaviour defect. Found P2 coverage gaps for the new
availability query: absent non-available status and authorization coverage,
and only one unavailable item in the ordering test. Noted the expected manual
JavaFX rendering limitation.

Resolution: `CatalogueServiceTest` now covers AVAILABLE, RESERVED, ON_LOAN and
UNAVAILABLE rows; logged-out and supervisor rejection; and alphabetical order
within unavailable rows.

## Documentation review

Updated the User Guide to describe the availability-led catalogue, borrower
request table and active/past-loan destinations. Updated the Developer Guide
to document `CatalogueService` availability derivation and ordering. The
review-gap registry records the three valid panel findings above.
