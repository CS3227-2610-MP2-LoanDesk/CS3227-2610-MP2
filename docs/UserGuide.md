# LoanDesk User Guide

LoanDesk is a JavaFX desktop application for managing equipment lending. The
application is designed for three roles:

| Role | Current borrower-visible responsibility |
| --- | --- |
| Borrower | Log in, browse equipment, submit and manage requests, and view loans |
| Supervisor | Review and decide requests; implementation is owned by another team member |
| Custodian | Manage collection, checkout, return and physical condition; implementation is owned by another team member |

This guide documents the borrower workflow that is currently implemented. The
supervisor and custodian buttons currently open role dashboards, but their
workflow actions are not yet part of the borrower implementation.

## Prerequisites

- JDK 25 or a compatible Java 25 distribution.
- Internet access for the first Gradle build so Gradle can download JavaFX,
  H2 and test dependencies.
- Windows PowerShell, macOS/Linux Terminal, or another shell that can run the
  Gradle wrapper.

No separate H2 server is required. The application uses H2 in embedded file
mode and creates its local database automatically.

## Run the application locally

From the repository root:

### Windows PowerShell

```powershell
.\gradlew.bat run
```

### macOS/Linux

```bash
./gradlew run
```

The first build may take longer while Gradle downloads dependencies. The
application opens the LoanDesk role-selection screen.

The supported development launch is through Gradle because it supplies the
JavaFX runtime modules and application dependencies. A plain JAR is not
currently a verified standalone release artifact; do not distribute only the
application JAR and assume JavaFX is installed on the user's machine.

## Local data and first launch

The application stores local data under:

```text
data/loandesk
```

The database contains users, password credentials, equipment, equipment
condition, requests and loans. The directory is local and ignored by Git.
When the database is empty, LoanDesk seeds synthetic borrower accounts and
equipment. Existing data is loaded on later launches; it is not overwritten by
the seed process.

The current synthetic seed accounts are:

| Username | Password |
| --- | --- |
| `testBorrower1` | `password1` |
| `testBorrower2` | `password2` |

These credentials are for local demonstrations only. Do not reuse them for a
real account.

Existing JSON files are not imported. To start with a clean demonstration
database, close the application, preserve any data you need, and remove the
local `data/loandesk` database directory before launching again. Never remove
the repository or a shared database directory by accident.

## Authentication

1. Select `Borrower` on the role-selection screen.
2. Select `Log in` for an existing account or `Sign up` for a new account.
3. Enter a username and password.
4. Select the action button.

Borrower usernames are trimmed, case-normalized and limited to 30 characters.
They may contain letters, numbers, underscores and hyphens. Borrower passwords
must contain between 8 and 128 characters.

Passwords are never stored as plaintext. LoanDesk stores a salted
`PBKDF2WithHmacSHA256` password hash with its salt and algorithm metadata in
the H2 database. A failed login does not create a session. Selecting `Log out`
clears the session and returns to role selection.

## Borrower dashboard

After a successful login, the borrower dashboard provides:

- `Catalogue`: browse equipment and begin a request;
- `My Requests`: view request status, edit eligible requests and cancel eligible
  future requests;
- `My Loans`: view active/lost loans and returned history;
- `Log out`: clear the current session.

Borrower service methods also require an active borrower session. Hiding a
button is only a usability measure; the service layer performs the actual role,
ownership and state checks.

## Catalogue

1. Select `Catalogue` from the dashboard.
2. Enter a full or partial equipment name.
3. Select `Filter`.
4. Select `Clear` to restore all catalogue entries.
5. Select an eligible equipment item and choose `Request`.

Filtering ignores case and surrounding whitespace. For example, `camera`,
`CAMERA` and ` cam ` produce the same results. A non-matching search displays
an empty-results message. The borrower catalogue is read-only: borrowers do
not edit equipment names, condition or availability from this screen.

The shared model already contains these condition values:

```text
GOOD, DAMAGED, UNDER_MAINTENANCE, LOST
```

Availability is derived from condition, approved reservations and active/lost
loans as `AVAILABLE`, `RESERVED`, `ON_LOAN` or `UNAVAILABLE`. The borrower UI
does not yet provide the supervisor/custodian controls that change these shared
states.

## Submit a request

From the catalogue, select an equipment item and choose `Request`.

Required fields:

- equipment item;
- non-blank purpose;
- start date;
- due date.

The start date defaults to today. The due date defaults to 14 days after the
start date, but a shorter period is allowed. Same-day borrowing is allowed.
Past start dates, due dates before the start date and periods longer than 14
days are rejected.

On success, the request is saved as `PENDING` and appears in `My Requests`.
The request service also checks that:

- the borrower is logged in;
- the borrower has not reached the maximum of three active loans or approved
  reservations;
- the borrower has no unresolved overdue or lost loan;
- the equipment is currently available;
- the borrower does not already have a pending request for that equipment.

Pending requests do not reserve equipment. An approved request reserves the
equipment until collection. Supervisor approval and custodian checkout are
performed by their respective role workflows when those implementations are
available.

## View, edit and cancel requests

Select `My Requests` to see the current borrower's requests. Active requests
are shown before terminal history. Selecting a request shows its details and
available actions.

### Editing

An owned `PENDING` request may be edited only before its start date. The
borrower may change its purpose and dates; the equipment and request ID remain
unchanged. The service rechecks the current session, ownership, state, date
rules and eligibility when saving.

### Cancellation

A borrower may cancel an owned `PENDING` or `APPROVED` request only when its
start date is later than today. A cancellation reason is required. The UI
provides common reasons and an `Other` option with an explanation field.

Requests cannot be cancelled on or after their start date, after collection,
or after reaching another terminal state. Rejected requests cannot be edited or
resubmitted; create a new request instead.

The agreed request states are:

| State | Meaning |
| --- | --- |
| `PENDING` | Submitted and awaiting supervisor decision |
| `APPROVED` | Approved and reserving equipment until collection |
| `COLLECTED` | Collected by the custodian and linked to a loan |
| `REJECTED` | Declined by the supervisor |
| `CANCELLED` | Cancelled by the borrower under the future-request rule |
| `EXPIRED` | Approved request not collected within its collection window |

## My Loans

Select `My Loans` from the dashboard. The page separates:

- active and lost loans at the top;
- returned loans in history below.

Each entry shows the equipment name and ID, checkout date, due date, return
date when available, and a displayed status. `OVERDUE` is derived when an
active loan is past its due date; `LOST` is shown when the shared loan status
is lost. The page is read-only for borrowers. Checkout, return, physical
condition and maintenance actions belong to the custodian workflow.

The page may initially be empty because a loan is created only when a
custodian checks out approved equipment. Returned history appears after a
custodian records a return.

## Suggested borrower demonstration

1. Start the application and sign in as `testBorrower1`.
2. Open `Catalogue`, filter with `CAMERA`, clear the filter, and try a
   non-matching search.
3. Select an equipment item and submit a same-day request with a purpose.
4. Open `My Requests`, confirm the request is `PENDING`, edit its purpose or
   dates, and verify the confirmation message.
5. Cancel a future request with a reason and verify that it becomes
   `CANCELLED`.
6. Open `My Loans` and verify the empty-state message if no custodian loan
   exists.
7. Log out and verify that protected borrower pages require a new login.

For final submission documentation, add screenshots of the role selection,
borrower login, catalogue filtering, request form, My Requests and My Loans
screens to this guide or the project submission materials.

## Troubleshooting

| Symptom | Action |
| --- | --- |
| JavaFX runtime components are missing | Run through `gradlew.bat run` or `./gradlew run`; do not run only a plain JAR |
| Login fails | Check username spelling, password length and whether the local database contains the account |
| Catalogue is empty | Confirm that the database was initialized and that the search filter is cleared |
| Request is rejected | Read the displayed eligibility, date, availability or duplicate-request message |
| Data appears to reset | Check that the application is launched from the repository root so `data/loandesk` resolves consistently |

## Current limitations

- Supervisor password login and supervisor request decisions are not part of the
  current borrower implementation.
- Custodian checkout, return and maintenance screens are not part of the
  current borrower implementation.
- Automated JavaFX interaction testing is not configured; GUI checks are
  manual.
- Cross-platform packaged release verification is still pending. The Gradle
  development launch is the currently verified way to run the application.
