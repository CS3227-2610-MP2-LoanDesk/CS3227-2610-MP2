# LoanDesk User Guide

LoanDesk is a desktop application for managing shared equipment loans. It
supports borrowers, supervisors, and custodians throughout the lending process.

## Starting LoanDesk

### From a GitHub Release

Download the JAR that matches both your operating system and processor from
the latest GitHub Release. The release JAR already includes JavaFX and H2, so
you do not need to install JavaFX separately. Install a Java 25 runtime, open
a terminal in the folder containing the downloaded JAR, then run the matching
command:

| Computer | Release asset | Command |
| --- | --- | --- |
| Windows | `loandesk-windows.jar` | `java -jar .\loandesk-windows.jar` |
| Linux | `loandesk-linux.jar` | `java -jar ./loandesk-linux.jar` |
| Apple-silicon Mac (M-series) | `loandesk-macos-arm64.jar` | `java -jar ./loandesk-macos-arm64.jar` |

Each release JAR is built with the JavaFX native libraries for its named
platform. Do not use a JAR intended for another operating system or processor.
If `java` is not recognised, install Java 25 and reopen the terminal. On macOS,
you may need to approve the app in **System Settings > Privacy & Security** if
the operating system blocks software downloaded from the internet.

### From the project source

To run a checked-out copy of the project, install JDK 25. JavaFX is downloaded
by Gradle automatically on the first run; internet access is therefore needed
once. From the repository root, run:

| Operating system | Command |
| --- | --- |
| Windows | `.\gradlew.bat run` |
| macOS or Linux | `./gradlew run` |

To make a runnable JAR for the current platform from source, replace `run` with
`fatJar`. The output is `build/libs/loandesk-0.1.0-all.jar`, which can be
started with `java -jar build/libs/loandesk-0.1.0-all.jar`.

## Logging In

The first screen provides three role cards: `Borrower`, `Supervisor`, and
`Custodian`. Selecting a role opens its sign-in form directly.

The following testing accounts are provided for local demonstrations:

- The seeded borrower usernames are `testBorrower1` and `testBorrower2`.
  For local synthetic demonstrations only, their passwords
  are `password1` and `password2` respectively.
- `Supervisor` requires the fixed supervisor account password. The seeded password is `supervisor1`.
- `Custodian` requires the fixed custodian account password. The seeded password is `custodian1`.

The initial catalogue has ten available items and no bookings.

After a successful login or sign-up, the role dashboard and its protected actions become available.
Select `Log out` to clear the active session and return to role selection.


## Borrower Features

### Catalogue

After logging in as a borrower, select `View Catalogue` to view equipment names
and their current availability. Available equipment appears first; each group is
then ordered alphabetically by name. Enter part of an equipment name and select
`Filter` to perform a case-insensitive search. Select `Clear` to restore the full
catalogue. If no item matches, the screen displays an empty-results message.
Only equipment marked `AVAILABLE` can be selected for a request. The catalogue
is read-only for borrowers.

### Borrower requests

From the catalogue, select one available equipment item and choose `Request`.
The request form requires a non-blank purpose and a requested start date. The
start date defaults to today and the due date defaults to fourteen days later;
shorter borrowing periods are allowed. Past start dates and due dates before
the start date are rejected.

After submission, select `My Requests` from the dashboard to view your own
requests. The table shows the equipment name, status, requested start and due
dates, and submission date. Active requests appear before terminal history.
Borrowers may edit the purpose and dates of an eligible `PENDING` request
before its start date; the equipment and request ID remain unchanged. Borrowers may also cancel
eligible future `PENDING` or `APPROVED` requests by selecting a cancellation
reason and confirming the action. Rejected requests are read-only and must be
replaced by a new request. Clarification and revision/resubmission are not part
of the current borrower workflow.

Borrowing is blocked when the service detects an unresolved overdue or lost
loan, or when the borrower has reached the active-loan/reservation
limit.

## Supervisor Features

Select `Supervisor` from role selection, enter the supervisor password, and the
supervisor dashboard offers `Review Queue` and `Decision History`.

`Review Queue` lists borrower requests with those still awaiting a decision
first and the longest-waiting request at the top. Use the filters to narrow the
list, then select a request and choose `Review selected request` to approve or
reject it.

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
rather than taken from the state at submission.

An approved request is collectable on its requested start date and the next
three calendar days. It becomes `EXPIRED` and releases its reservation the day
after that window ends. This is applied when shared request data is next opened.

Clarification and resubmission are not part of the current workflow. Supervisors
do not edit physical equipment condition; that remains with the custodian.

`Decision History` lists every recorded decision and cancellation, most recent
first, showing who decided, when and why.


## Custodian Features

Select `Custodian` from role selection and enter the custodian password. The
dashboard shows non-clickable collection, loan, inventory and condition-alert
statistics, followed by filterable `Loan Requests` and `Active Loans` tables.

Use `Manage inventory` in the top-right corner to open the separate inventory
table.

`Loan Requests` lists approved requests that are within the inclusive
collection window: the requested start date through three additional calendar
days. Choose `Check out` once a user has collected their item. Checkout is
refused if the request is no longer approved or collectable, or if the
equipment is unavailable or not in `GOOD` condition.

Use the request search field to filter by equipment name or ID, borrower, or
request ID.

`Active Loans` lists active and lost loans, with overdue active loans first.
Choose `Return / update` to open the loan-details overlay.

- For an active loan, select a return condition (`GOOD`, `DAMAGED`,
or `UNDER_MAINTENANCE`) before marking it returned.
- If the borrower has misplaced an item, mark it as `LOST`.
- If a `LOST` item has been returned, you can recover it on this page.
This restores it to an `ACTIVE` loan with `GOOD` equipment; it remains on loan
until you select and record the observed return condition. This two-step flow
ensures the condition is chosen at physical return rather than assumed during
recovery.

`Manage Inventory` opens a table of all equipment, its physical condition and
read-only availability derived from shared requests, loans and condition. From
there, custodians can add equipment, or change an item's condition directly in
its table row. Use the pen beside an item's name to edit it in place, then the
tick to save. The `Add equipment` button opens a panel for the required name
and initial condition. Condition changes are disabled while an item is
reserved; other condition changes are saved immediately. The eye action opens
item details, including condition, availability and any current borrower's
collection and due dates. Dashboard tables can be filtered by item, borrower
or record ID and by `ACTIVE`, `OVERDUE` or `LOST` status. Categories, notes,
deletion, retirement, bulk import and maintenance records are outside this
MVP.
