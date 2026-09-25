# Shared Request Foundation Scope — 25 September 2026

## Baseline

- Branch: `yikbing`, synchronized with merged `origin/main` at `a18f29f`.
- Required baseline command: `.\gradlew.bat clean test --no-daemon`.
- Baseline result: `BUILD SUCCESSFUL`.
- No application code was changed before recording this scope.

## Decisions recorded

- Request records use a generated request ID, existing borrower username and
  equipment ID, purpose, requested dates, status, timestamps and nullable
  `loanId`.
- Request statuses include `PENDING`, `APPROVED`, `COLLECTED`, `REJECTED`,
  `CANCELLED` and `EXPIRED`.
- `EXPIRED` applies only to an approved request not collected by the end of its
  start date. `OVERDUE` applies to an active loan after its due date and is
  derived rather than manually assigned.
- Approval/rejection and cancellation retain their actor, time and reason.
- A lost loan blocks new borrower requests until resolved.
- Availability precedence is `UNAVAILABLE`, `ON_LOAN`, `RESERVED`, then
  `AVAILABLE`.
- Request and future loan data use the shared `LoanDeskData`/H2 boundary. The
  schema change must be additive and preserve existing local data.

## Implementation boundary

The first code slice will add the shared domain vocabulary, additive H2
request/loan persistence and read-only eligibility/availability queries. It
will not add supervisor approval UI, custodian checkout/return UI or borrower
request screens. Those will be implemented and reviewed in later focused
slices.

## Coordination and evidence

The scope was circulated to the groupmates before implementation. Their role
owners remain responsible for supervisor approval and custodian checkout,
return, condition and maintenance operations. This log will be extended with
the changed files, focused tests, review findings and limitations after the
implementation slice.

## Implementation evidence

Changed production areas:

- Added shared equipment condition, request status, loan status and availability
  vocabulary under `src/main/java/loandesk/domain/`.
- Extended `Equipment` with a centrally persisted condition while preserving
  the existing two-argument constructor as a `GOOD` default.
- Added `LoanRequest` and `Loan` records with required-field/date and
  state-specific metadata validation. `Loan.isOverdue` derives overdue state
  from an active loan and the current date.
- Extended `LoanDeskData` and `DatabaseDataStore` with additive request/loan
  tables, timestamps, audit fields, equipment condition and rollback-safe
  snapshot persistence.
- Added `AvailabilityService` and `BorrowerEligibilityService`. Both enforce
  the agreed precedence/eligibility rules at the shared application boundary;
  no UI-only enforcement was added.

Focused tests added:

- request/loan persistence and equipment-condition round trip;
- additive workflow save rollback;
- inclusive due-date and returned-loan overdue boundaries;
- whitespace-purpose rejection;
- availability precedence, pending requests, expired reservations and lost
  loans;
- eligible borrower, all-blocker reporting, logged-out/wrong-role rejection and
  foreign-borrower isolation.

Verification:

- Baseline ` .\gradlew.bat clean test --no-daemon`: passed before implementation.
- Intermediate ` .\gradlew.bat test --no-daemon`: one synthetic foreign-key
  fixture failure was found and corrected; the rerun passed.
- Final ` .\gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL`.
- `git diff --check`: passed, with only normal Git line-ending warnings.

Skill reviews:

- Edge-case/test review: `RUN`. The focused test command passed after the
  fixture correction. It covered persistence rollback, state boundaries,
  derived availability, eligibility blockers, direct role rejection and
  foreign-owner isolation. Request submission UI, duplicate-pending mutation,
  cancellation and stale form submission remain intentionally uncovered.
- Ownership review: `RUN`. The eligibility service obtains the borrower from
  the active session, rejects logged-out/wrong-role calls before loading data,
  and filters loans/requests by that session identity. No borrower mutation
  service exists yet, so edit/cancel ownership remains future work.

## Independent review panel evidence

A fresh three-reviewer panel was run before this commit. Each reviewer was
given a read-only, role-specific prompt and was instructed to inspect the
current working tree, tests and documentation without editing or committing.

- Tesla (`RUN`, ownership/workflow focus) identified that past-start approved
  reservations were still counted toward the borrower limit, and that direct
  records could express inconsistent collected/loan state. The reservation
  filter, domain invariants and persistence cross-record validation were added.
- Hubble (`RUN`, H2/persistence focus) confirmed transactional rollback and
  concurrency handling, while flagging the same cross-record integrity gap and
  the lack of a legacy-schema migration test. The migration test was added and
  the save validator now checks request/loan references, ownership and required
  collected links before any replacement deletes.
- Zeno (`RUN`, completeness/shared-contract focus) found the foundation scope
  coherent but not yet a complete workflow or PR-readiness claim. It also
  noted that groupmate acknowledgement is coordination evidence rather than a
  completed implementation requirement, and that one reviewer could not
  reproduce the clean test because of a temporary build-output lock. Those
  limitations are recorded rather than overstated.

The panel’s valid findings were corrected, and the synthetic fixtures were
updated to use matching collected request/loan IDs. The final clean test run
then passed. This panel is evidence for this focused foundation commit; a
fresh panel remains appropriate when the complete borrower request feature is
ready for PR review.

This is an implementation checkpoint, not a PR-readiness claim. The next
slice is the borrower request service/form and should be started only after
this foundation is reviewed and committed separately.

## Follow-up invariant fix — 25 September 2026

The final PR-readiness review identified one valid small domain gap: a returned
loan could contain a return date earlier than its checkout date if a caller
constructed the record directly. The intended checkout-before-return workflow
was already clear, but the shared domain record did not enforce that rule.

- Added a `Loan` invariant rejecting `returnedDate` before `checkoutDate`.
- Added `WorkflowDomainTest.loanRejectsReturnDateBeforeCheckoutDate`.
- Baseline ` .\gradlew.bat clean test --no-daemon`: passed before the fix.
- Final ` .\gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL` after the
  fix.

## Follow-up cancellation-ownership fix — 25 September 2026

The remaining known contract gap was that persistence accepted a cancelled
request whose cancellation actor differed from the borrower owner. The shared
validation now rejects that mismatch; automated missed collection remains the
separate `EXPIRED` status.

- Added persistence validation requiring `cancelledBy` to equal the request
  borrower.
- Added `DatabaseDataStoreTest.rejectsCancellationByAnotherBorrower`.
- Baseline ` .\gradlew.bat clean test --no-daemon`: passed before the fix.
- Final ` .\gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL` after the
  fix.

This is a small follow-up commit to the foundation. The remaining review items
about request lifecycle metadata and broader persistence coverage will be
handled before the corresponding mutation workflow is implemented.

## Follow-up request-state fix — 25 September 2026

The next review finding was that a `PENDING` request could be constructed with
approval or rejection metadata. This contradicted the request lifecycle policy:
decision metadata belongs only after a supervisor decision.

- Added a `LoanRequest` invariant rejecting decision metadata on `PENDING`.
- Added `WorkflowDomainTest.pendingRequestRejectsDecisionMetadata`.
- Baseline ` .\gradlew.bat clean test --no-daemon`: passed before the fix.
- Final ` .\gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL` after the
  fix.
