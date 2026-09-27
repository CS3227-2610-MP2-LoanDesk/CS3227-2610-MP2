# Completeness review fixture

Use this controlled fixture with `loandesk-borrower-change-completeness-review`.

## Scenario

A borrower request-edit service and UI were changed. Focused service tests
exist, but the User Guide still says editing is unavailable, the Developer
Guide does not describe the new service operation, and no dated session log or
independent review evidence is recorded.

## Expected review behaviour

The reviewer should report missing or stale documentation and missing session
and independent-review evidence. It should not claim that the feature is
complete based only on passing service tests.
