# 2026-09-28 Supervisor workflow implementation

## Scope

Implemented the supervisor lane end to end on branch `feature/supervisor-workflow`:
the shared permission matrix, the shared request lifecycle and expiry rule,
supervisor password sign-in, the review and authorization service, the three
supervisor screens, and the supervisor test set. Clarification and resubmission
are deliberately excluded. Custodian inventory, checkout, return and maintenance
remain with their owner and were not implemented or simulated.

## Decisions

- A decision reason is required only to reject. `LoanRequest` previously
  required one for every decision; approval now records only the decision owner
  and time, matching the agreed request policy.
- Clarification and resubmission are out of scope for the initial workflow, per
  the request policy, even though the workload overview lists them under the
  supervisor role. No `CLARIFICATION_REQUIRED` status was added.
- `PermissionService` holds one permission matrix for all three roles. The four
  borrower services were migrated from ad-hoc `Session.requireRole` calls to it,
  so the mechanism is exercised by two roles rather than only the supervisor.
  Record ownership, such as a borrower reading only their own requests, stays in
  the owning service.
- `RequestLifecycleService` owns legal transitions and the EXPIRED rule. Expiry
  is applied lazily when shared request data is read and persists only when a
  status actually changed, rather than on a background task or only at startup.
- An earlier attempt added the supervisor account to any database missing one
  during `loadOrSeed`. That was withdrawn: it mutated shared state on a read,
  advanced the database revision, and broke the optimistic-concurrency and
  legacy-database contracts. The supervisor account is seeded on first launch
  only, so a database created before this change has no supervisor and reports
  that the local demonstration data must be reseeded.
- The persistence guard now accepts a supervisor as the canceller of a request.
  It previously required the canceller to be the borrower, which would have
  rejected every supervisor cancellation of an approved booking.
- Approval rechecks availability and eligibility at decision time through the
  same shared calculations the borrower services use. The eligibility rule was
  extracted into `BorrowerEligibilityService.evaluate` so both roles apply one
  implementation rather than two copies.

## Files changed

- `src/main/java/loandesk/application/Permission.java` (new)
- `src/main/java/loandesk/application/PermissionService.java` (new)
- `src/main/java/loandesk/application/RequestLifecycleService.java` (new)
- `src/main/java/loandesk/application/ReviewFilter.java` (new)
- `src/main/java/loandesk/application/SupervisorRequestService.java` (new)
- `src/main/java/loandesk/application/AuthenticationService.java`
- `src/main/java/loandesk/application/BorrowerEligibilityService.java`
- `src/main/java/loandesk/application/BorrowerLoanService.java`
- `src/main/java/loandesk/application/BorrowerRequestService.java`
- `src/main/java/loandesk/application/CatalogueService.java`
- `src/main/java/loandesk/domain/LoanRequest.java`
- `src/main/java/loandesk/persistence/DatabaseDataStore.java`
- `src/main/java/loandesk/LoanDeskApp.java`
- `src/test/java/loandesk/ApprovalReasonInvariantTest.java` (new)
- `src/test/java/loandesk/PermissionServiceTest.java` (new)
- `src/test/java/loandesk/RequestLifecycleServiceTest.java` (new)
- `src/test/java/loandesk/SupervisorAuthenticationTest.java` (new)
- `src/test/java/loandesk/SupervisorRequestServiceTest.java` (new)
- `src/test/java/loandesk/SupervisorBorrowerIntegrationTest.java` (new)
- `src/test/java/loandesk/AuthenticationServiceTest.java`

## Verification

- Pre-change `.\gradlew.bat clean test --no-daemon`: passed.
- Post-change `.\gradlew.bat clean test --no-daemon`: passed, 130 tests, 51 of
  them added by this work.
- `git diff --check` passed on every commit; Git reported only normal LF/CRLF
  conversion warnings.
- Supervisor tests cover overlapping approvals, availability rechecks against
  damaged and on-loan equipment, eligibility rechecks for an overdue borrower,
  unauthorized decisions by borrower, custodian and signed-out sessions,
  invalid transitions out of every terminal status, reservation release on
  supervisor cancellation, queue filtering by status, borrower and date, and a
  failed save leaving the request undecided.
- Integration tests run submission, review, approval, rejection, supervisor
  cancellation and overnight expiry against one real H2 database, including a
  restart.
- No manual GUI test was performed in this session. The project has no JavaFX
  interaction harness, so the three supervisor screens are currently verified
  only by compilation and by the service tests behind them.
- No independent review panel was dispatched for this change.
- The work is committed on `feature/supervisor-workflow` in six increments. No
  push and no pull request have been made.

## Limitations

- The supervisor screens have not been exercised by a person. A manual pass over
  sign-in, queue filtering, approve, reject, cancel and decision history is
  still required before this is claimed as working.
- Availability treats an approved reservation as blocking the item outright
  rather than for a date range, so two non-overlapping future bookings for the
  same item cannot both be approved. This is the existing shared contract, not a
  change made here, and it should be settled with the team.
- Equipment has no `RETIRED` condition, so the "unavailable or retired items"
  case in the workload overview is covered only for DAMAGED, UNDER_MAINTENANCE
  and LOST. Adding RETIRED belongs to the custodian owner.
- The seeded supervisor password is demonstration-only and is stored in
  `DatabaseDataStore` as a public constant so tests and the user guide can refer
  to it. It is hashed with the shared PBKDF2 hasher in the database, never
  stored in clear text.
- A database seeded before this change has no supervisor account and cannot sign
  in until the ignored local `data/loandesk` files are removed.
- Supervisor documentation, the permission-and-workflow agent skill and its
  evaluation, the checklist update, and the CI expansion to build and package
  were excluded from this session's scope and remain outstanding.
