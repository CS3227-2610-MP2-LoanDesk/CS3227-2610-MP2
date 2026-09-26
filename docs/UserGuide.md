# LoanDesk User Guide

The current borrower workflow provides local role selection, password
authentication, catalogue browsing, request submission, request history,
request cancellation and a read-only loans/history view. Supervisor and
custodian workflow actions are still being implemented by their owners.

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

## Borrower requests

From the catalogue, select one available equipment item and choose `Request`.
The request form requires a non-blank purpose and a requested start date. The
start date defaults to today and the due date defaults to fourteen days later;
shorter borrowing periods are allowed. Past start dates and due dates before
the start date are rejected.

After submission, select `My Requests` from the dashboard to view your own
requests. Active requests appear before terminal history. Borrowers may cancel
eligible future `PENDING` or `APPROVED` requests by selecting a cancellation
reason and confirming the action. Rejected requests are read-only and must be
replaced by a new request. Clarification and revision are not part of the
current borrower workflow.

Borrowing is blocked when the service detects an unresolved overdue or lost
loan, or when the borrower has reached the agreed active-loan/reservation
limit. The service rechecks ownership, current state, date rules and
availability when a request is submitted.

## My loans and history

Select `My Loans` from the borrower dashboard to view persisted loans belonging
to the logged-in borrower. Active and lost loans appear above returned history.
Each entry shows the equipment, checkout date, due date, return date when
available, and a displayed status such as `ACTIVE`, `OVERDUE`, `LOST` or
`RETURNED`. Overdue is derived when an active loan is past its due date.

This screen is read-only. It may be empty until a custodian checks out or
returns equipment. Borrower checkout, return and physical-condition updates
are not performed from this screen.
