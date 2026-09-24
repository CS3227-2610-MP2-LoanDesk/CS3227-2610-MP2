# 2026-09-24 Request and loan policy

## Purpose

Recorded the borrower-side workflow decisions shared with and accepted by the
supervisor and custodian owners before implementing shared request and loan
contracts.

## Draft decisions

- Requests contain one equipment item. A borrower may have at most three
  active loans or approved reservations.
- Equipment, a non-blank purpose and a start date are required. The start date
  defaults to today, and the due date defaults to fourteen days later while
  allowing shorter periods.
- Borrowers may edit only pending requests before their start date.
- Request statuses are `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` and
  `EXPIRED`; clarification and revision are excluded from the initial workflow.
- Borrowers create requests, supervisors approve or reject them, and borrowers
  may cancel eligible future requests with a required reason.
- Requests and loans are separate records. The custodian creates a loan at
  physical checkout and records return condition; rejected, cancelled and
  expired requests do not create loans.
- Dates are date-only, inclusive, allow same-day borrowing and reject past
  starts or due dates before the start. The collection window ends at the end
  of the requested start date; missed collection causes expiry and releases the
  reservation.
- Borrowers with unresolved overdue or lost loans cannot submit new requests.
  The request service enforces this rule; fine settlement and supervisor
  overrides are outside the current scope.
- Pending requests do not reserve equipment. Approved requests reserve it;
  active loans and damaged, under-maintenance or lost equipment are unavailable.
  Availability is proposed as `AVAILABLE`, `RESERVED`, `ON_LOAN` and
  `UNAVAILABLE`.

## Ownership clarification

The custodian is responsible for recording physical condition, maintenance
status, loss confirmation and return condition. The supervisor uses that state
when approving requests but does not directly edit physical equipment status.

## Status

These shared decisions are agreed for implementation. They have not yet been
implemented in application code, and no local database was changed in this
policy-recording step.
