# LoanDesk User Guide

The current foundation provides local role selection, borrower identity
selection, and placeholder dashboards. Role features will be documented here
as they are implemented.

## Running locally

Install JDK 25, then run `./gradlew run` from the repository root.

## Login foundation

The first screen provides three role buttons: `Borrower`, `Supervisor`, and
`Custodian`.

- `Borrower` provides `Log in` and `Sign up` actions.
- Borrower usernames have no passwords, are unique, and may contain letters,
	numbers, underscores, and hyphens up to 30 characters.
- The initial seeded borrower usernames are `testBorrower1` and
	`testBorrower2`.
- `Supervisor` and `Custodian` open their fixed local role accounts directly.
- The initial equipment records are `camera1` and `camera2`.
- Local data is stored in `data/loandesk.json` and persists between launches.

The placeholder dashboard buttons do not provide role features yet.
