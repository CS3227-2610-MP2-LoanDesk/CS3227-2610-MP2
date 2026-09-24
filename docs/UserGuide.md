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
- Local data is stored in an embedded H2 database rooted at `data/loandesk` and
  persists between launches. Users do not need to install a separate database
  server.

Existing JSON files are not imported. Use borrower sign-up to create accounts
in the new local database. The generated database files remain local and are
ignored by Git.

After a successful login or sign-up, the borrower dashboard and its protected
catalogue actions become available. Select `Log out` to clear the active
session and return to role selection; protected borrower actions require
logging in again.

## Borrower catalogue

After logging in as a borrower, select `Catalogue` to view the seeded equipment
identifiers and names. Enter part of an equipment name and select `Filter` to
perform a case-insensitive search. Select `Clear` to restore the full catalogue.
If no item matches, the screen displays an empty-results message. The current
catalogue slice is read-only; category, condition, availability and borrowing
actions will be added with later workflow milestones.
