# Edge-case review fixture

Use this controlled fixture with `loandesk-borrower-edge-case-test-review`.

## Scenario

A borrower request test covers a valid edit and one invalid blank purpose, but
does not cover the maximum allowed purpose length, reversed dates, a failed
database save, or reload after restart. The UI displays success before the save
result is known.

## Expected review behaviour

The reviewer should identify the missing boundary, failed-save and restart
coverage, and the possibility of a false success message. It should recommend
observable tests rather than tests of private helper methods.
