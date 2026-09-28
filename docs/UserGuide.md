# LoanDesk User Guide

The current workflow provides local role selection, password authentication,
catalogue browsing, request submission, request history, request cancellation
and a read-only loans/history view for borrowers, and request review,
approval, rejection, booking cancellation and decision history for
supervisors. Custodians can manage collections, active loans, physical
condition and the minimal equipment inventory.

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
- `Supervisor` requires the fixed supervisor account password. For local
  synthetic demonstrations only, the seeded password is `supervisor1`. Do not
  reuse this password.
- `Custodian` requires the fixed custodian account password. For local
  synthetic demonstrations only, the seeded password is `custodian1`. Do not
  reuse this password.
- The initial equipment records are `camera1` and `camera2`.
- Local data is stored in an embedded H2 database rooted at `data/loandesk` and
  persists between launches. Users do not need to install a separate database
  server.
- The supervisor account is created when the database is first seeded. A
  database created before supervisor login existed has no supervisor account;
  sign-in then reports that the ignored local `data/loandesk` files must be
  removed so the demonstration accounts are seeded again. A database that
  predates custodian login is upgraded with the missing custodian account when
  it is next opened.

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
metadata will be added with later workflow milestones, while eligible borrowers
can already start a request from the catalogue.

## Borrower requests

From the catalogue, select one available equipment item and choose `Request`.
The request form requires a non-blank purpose and a requested start date. The
start date defaults to today and the due date defaults to fourteen days later;
shorter borrowing periods are allowed. Past start dates and due dates before
the start date are rejected.

After submission, select `My Requests` from the dashboard to view your own
requests. Active requests appear before terminal history. Borrowers may edit
the purpose and dates of an eligible `PENDING` request before its start date;
the equipment and request ID remain unchanged. Borrowers may also cancel
eligible future `PENDING` or `APPROVED` requests by selecting a cancellation
reason and confirming the action. Rejected requests are read-only and must be
replaced by a new request. Clarification and revision/resubmission are not part
of the current borrower workflow.

Borrowing is blocked when the service detects an unresolved overdue or lost
loan, or when the borrower has reached the agreed active-loan/reservation
limit. The service rechecks ownership, current state, date rules and
availability when a request is submitted.

## Supervisor review

Select `Supervisor` from role selection, enter the supervisor password, and the
supervisor dashboard offers `Review Queue` and `Decision History`.

`Review Queue` lists borrower requests with those still awaiting a decision
first and the longest-waiting request at the top. Filter by status, by borrower
username, and by the requested start date falling on or after and on or before
chosen dates. The queue opens filtered to `PENDING`; select `Clear` to see every
status. Select a request and choose `Review selected request`.

`Review Details` shows the borrower, equipment, purpose and requested dates,
together with the decision context: the equipment's current availability, the
borrowing rules the borrower currently fails, if any, and that borrower's
outstanding loans with overdue marked. Any recorded decision or cancellation is
shown with its owner, time and reason.

From that screen:

- `Approve` reserves the item until collection. A reason is optional.
- `Reject` requires a reason, which the borrower sees on their own request.
- `Cancel booking` appears for an approved request that has not been collected.
  A reason is required and the reservation is released.

Approval is refused when the item is no longer available or the borrower is no
longer eligible, both of which are rechecked at the moment of the decision
rather than taken from the state at submission. A request that is already
decided, cancelled, expired or collected can no longer be decided.

An approved request is collectable on its requested start date and the next
three calendar days. It becomes `EXPIRED` and releases its reservation the day
after that window ends. This is applied when shared request data is next opened.

Clarification and resubmission are not part of the current workflow. Supervisors
do not edit physical equipment condition; that remains with the custodian.

`Decision History` lists every recorded decision and cancellation, most recent
first, showing who decided, when and why.

## Custodian workflow

Select `Custodian` from role selection and enter the custodian password. The
dashboard provides `Collections`, `Active Loans`, and `Inventory`.

`Collections` lists approved requests that are within the inclusive collection
window: the requested start date through three additional calendar days. Select
a request and choose `Check out selected request` to create its one linked
active loan. Checkout is refused if the request is no longer approved or
collectable, or if the equipment is unavailable or not in `GOOD` condition.

`Active Loans` lists active and lost loans, with overdue active loans first.
For an active loan, select a return condition (`GOOD`, `DAMAGED`, or
`UNDER_MAINTENANCE`) and record its return. Marking an active item lost changes
the loan and equipment to `LOST`. Recovering a lost item deliberately restores
it to an `ACTIVE` loan with `GOOD` equipment; it remains on loan until the
custodian selects and records the observed return condition. This two-step flow
ensures the condition is chosen at physical return rather than assumed during
recovery.

`Inventory` lets custodians add equipment and edit its name or physical
condition. Its availability is read-only and derived from the shared requests,
loans and condition. Categories, notes, deletion, retirement, bulk import and
maintenance records are outside this MVP.

## My loans and history

Select `My Loans` from the borrower dashboard to view persisted loans belonging
to the logged-in borrower. Active and lost loans appear above returned history.
Each entry shows the equipment, checkout date, due date, return date when
available, and a displayed status such as `ACTIVE`, `OVERDUE`, `LOST` or
`RETURNED`. Overdue is derived when an active loan is past its due date.

This screen is read-only. It may be empty until a custodian checks out or
returns equipment. Borrower checkout, return and physical-condition updates
are not performed from this screen.
