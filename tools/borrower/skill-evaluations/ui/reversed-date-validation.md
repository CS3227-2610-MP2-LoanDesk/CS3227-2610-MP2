# UI review fixture: reversed dates

Use this controlled fixture with `loandesk-borrower-ui-review`.

## Scenario

The borrower request form accepts a start date after the end date, submits the
request, and displays a success message. The service has no corresponding
date-order validation in this fixture.

## Expected review behaviour

The reviewer should identify the invalid date-range acceptance, distinguish UI
inspection from runtime execution, and recommend a focused rejection test.

The fixture is intentionally descriptive rather than production code. It tests
whether the reviewer notices the acceptance-rule gap, not whether JavaFX can be
automated in this repository.
