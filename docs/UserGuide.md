# LoanDesk User Guide

The current foundation provides local role selection, borrower password
authentication, and placeholder dashboards. Role features will be documented
here as they are implemented.

## Running locally

Install JDK 25, then run `./gradlew run` from the repository root.

## Login foundation

The first screen provides three role buttons: `Borrower`, `Supervisor`, and
`Custodian`.

- `Borrower` provides `Log in` and `Sign up` actions with a password field.
- Borrower usernames are unique and may contain letters, numbers, underscores,
  and hyphens up to 30 characters.
- Borrower passwords must be 8 to 128 characters. The application stores
  salted password hashes rather than plaintext passwords.
- The initial seeded borrower usernames are `testBorrower1` and `testBorrower2`.
  For local synthetic demonstrations only, their passwords
  are `password1` and `password2` respectively. Do not reuse these passwords.
- The current foundation opens the fixed local supervisor and custodian role
  accounts directly. Password login for those roles is still pending their
  owners' implementation.
- The initial equipment records are `camera1` and `camera2`.
- Local data is stored in `data/loandesk.json` and persists between launches.

If `data/loandesk.json` was created before password support, its existing users
do not have credentials. The application does not invent passwords or silently
overwrite that file; use a fresh local demonstration file or an agreed
migration process.

The placeholder dashboard buttons do not provide role features yet.
