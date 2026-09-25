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

## Final PR-readiness review — 25 September 2026

A fresh read-only three-reviewer panel inspected committed `HEAD 155dadf`.
The panel confirmed the reciprocal request/loan linkage fix and the
foundation-only scope. It also identified stale handoff/checklist wording,
which was corrected in the follow-up documentation commit. Schema-version
tracking, database-level foreign keys for the reciprocal nullable link, and a
purpose-built mid-write failure-injection test remain broader hardening work;
the current additive H2 boundary validates snapshots before replacement and
has passing rollback coverage for the implemented save paths.

The project-local ` .\gradlew.bat clean test --no-daemon` run after the latest
code fix passed. Reviewer environments could not independently reproduce the
Gradle run because of their separate wrapper-lock/network context; this is
recorded as a reviewer-environment limitation, not a project test failure.

## Borrower request-submission service — 25 September 2026

The next MVP slice adds the borrower application service without adding the
JavaFX form yet. `BorrowerRequestService` requires an active borrower session,
normalizes the equipment ID and purpose, validates today-or-future start dates
and a maximum fourteen-day period, rechecks borrower eligibility and current
equipment availability, rejects duplicate pending requests for the same item,
and saves one new `PENDING` request through the shared `DataStore` boundary.

Focused tests cover successful persistence, same-day and fourteen-day
boundaries, past dates, blank purpose, unknown equipment, duplicate pending
requests, unavailable equipment, logged-out/wrong-role calls and a synthetic
save failure that leaves the loaded snapshot unchanged.

Review status:

- Edge-case/test review: `RUN`; final ` .\gradlew.bat clean test --no-daemon`
  passed.
- Ownership review: `RUN`; borrower identity comes from `Session.requireRole`
  and the service has no caller-supplied owner field.
- UI review: `NOT APPLICABLE`; no JavaFX screen changed in this slice.

The current availability check uses the existing read-only availability
precedence. Date-range conflict resolution, supervisor approval, request
editing/cancellation services and the borrower request form remain later
slices.

## Borrower request form — 25 September 2026

The request-submission service is now connected to the borrower catalogue.
Borrowers can select an equipment item, choose a purpose or enter an `Other`
explanation, choose start and due dates, submit the form, see inline failure
feedback and acknowledge a `PENDING` confirmation before returning to the
dashboard. The form delegates authorization, eligibility, availability,
validation and persistence to `BorrowerRequestService`.

Verification:

- Final ` .\gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL`.
- UI review: `RUN` for source and wiring inspection.
- Actual JavaFX interaction: pending manual verification; no automated GUI
  interaction test exists for this project yet, so no interaction result is
  claimed here.

Remaining limitations for this slice are date-range availability conflicts,
request list/history screens, edit/cancel actions, and supervisor/custodian
workflow screens.

Manual GUI evidence: the user tested the borrower catalogue selection and
request form flow, including successful submission, confirmation acknowledgement
and the listed invalid-input cases. The UI behaved as expected. This is manual
user evidence; no automated JavaFX interaction test was claimed.

## Follow-up request/loan linkage fix — 25 September 2026

The final review found that two different collected requests could reference
the same loan ID while only one loan pointed back to one of those requests.
The persistence validator now requires every collected request to link to a
loan whose `requestId` points back to that exact request.

- Added the reciprocal collected-request/loan linkage validation.
- Added `DatabaseDataStoreTest.rejectsCollectedRequestsSharingOneLoan`.
- Baseline ` .\gradlew.bat clean test --no-daemon`: passed after retrying a
  transient Gradle wrapper download/unzip failure.
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

## Borrower request list and details — 25 September 2026

The borrower dashboard now opens a read-only `My Requests` screen. It loads
requests through `BorrowerRequestService`, filters them by the logged-in
borrower's session identity, places active statuses before terminal history,
and displays equipment, purpose, dates, status and available decision or
cancellation reasons. Empty results and load failures have visible feedback.

Verification:

- Focused `BorrowerRequestServiceTest`: `BUILD SUCCESSFUL`.
- Query tests cover owner filtering, active-before-history ordering, unknown
  and foreign request IDs, logged-out access and wrong-role access.
- Ownership review: `RUN`; query methods require `Session.requireRole(BORROWER)`
  and do not accept a caller-supplied owner.
- Edge-case/test review: `RUN`; valid query, empty result, foreign/unknown ID,
  wrong-role and logged-out cases are covered. Full clean verification remains
  required after this implementation slice.
- UI review: `RUN` for source and wiring inspection. Manual JavaFX verification
  of `My Requests`, including empty state and selected details, is still pending;
  no automated JavaFX interaction result is claimed.

The restart-specific check remains open until a fresh store instance is used to
verify persisted request data. Editing, cancellation, supervisor decisions and
loan history remain later slices.

## Independent review checkpoint update — 25 September 2026

The change-completeness workflow now explicitly requires a fresh, read-only
independent reviewer panel before finalizing each meaningful borrower feature
or milestone commit, not only when checking PR readiness. Routine tests,
small documentation edits and trivial formatting changes remain exempt. Valid
panel findings require affected-test and full-clean-suite reruns; a second
panel is needed only when the fix materially changes behavior or addresses a
major finding.

## Independent panel for My Requests — 25 September 2026

Before committing the My Requests slice, two fresh read-only reviewers were
invoked with the current uncommitted diff and relevant requirements.

- Ownership/workflow reviewer: `RUN`; no authorization defect found. The
  service requires a borrower session, filters by session username, and
  safely rejects foreign and unknown request IDs.
- Behaviour/test/UI reviewer: `RUN`; confirmed the ownership and basic display
  behavior, but identified three findings:
  - the read-only screen should explicitly explain that edit/cancel actions
    are deferred rather than appearing to omit permitted actions;
  - a fresh-store restart test and stronger evidence for empty/stale/UI cases
    are still missing, so the restart milestone checkbox remains open;
  - long purpose or reason text may be clipped in the fixed details area.

The panel was not treated as a pass. No reviewer edited files, committed,
pushed, changed local data or ran JavaFX interaction. The focused and full
Gradle suites had already passed in the main session before this panel; one
reviewer could not rerun Gradle because of its isolated environment's lock or
cached-JavaFX access restrictions. Fixes and verification are pending.

## My Requests review fixes — 25 September 2026

The panel findings were addressed within the current read-only scope:

- The details view now explicitly explains that editing and cancellation are
  deferred, rather than implying that permitted actions were accidentally
  omitted.
- `BorrowerRequestServiceTest.reloadsOwnRequestsFromAFreshStoreInstance`
  recreates the H2 store and verifies the persisted request is returned.
- The details label is displayed inside a scrollable pane so long purpose or
  decision/cancellation text remains accessible.

Verification after the fixes:

- Focused `BorrowerRequestServiceTest`: `BUILD SUCCESSFUL`.
- Full ` .\gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL`.
- The restart-specific milestone checkbox is now complete.
- Manual JavaFX verification of the updated screen remains pending. No second
  panel was needed because the fixes clarified and hardened the existing
  read-only behavior without introducing a new workflow.

Manual GUI evidence: the user logged in as a borrower, opened `My Requests`,
verified the read-only explanation, selected and inspected a request, restarted
the app and confirmed persistence, checked the empty state, and confirmed that
another borrower could not see the first borrower's requests. The updated GUI
behaved as expected. This is manual user evidence; no automated JavaFX
interaction test was claimed.

## Implementation preflight skill — 25 September 2026

Added `loandesk-borrower-implementation-preflight` as a fifth automatically
selectable borrower skill. Before meaningful implementation, it reads the
project policies and `ReviewGapRegistry.md`, turns applicable prior lapses into
a prevention checklist, identifies focused tests and GUI evidence, and flags
unresolved shared decisions. It does not replace the post-implementation
reviews or independent panel, and it does not run from Git hooks.

## Borrower cancellation slice — 25 September 2026

The borrower can now cancel an own future `PENDING` or `APPROVED` request. The
service requires a non-blank reason, rechecks the session owner, current
status and start date, changes the request to `CANCELLED`, preserves approval
metadata where present, records the borrower and timestamp, and saves through
the shared H2 boundary. The UI exposes the action only for eligible selected
requests, uses the agreed reason dropdown with a required `Other` explanation,
requires confirmation and reports that the reservation was released.

Focused tests cover successful pending and approved cancellation, same-day
rejection, terminal/repeated cancellation, foreign and unknown IDs, blank
reasons and failed-save state preservation.

Verification:

- Preflight: `RUN`; the review-gap registry produced the prevention checklist
  for ownership, state/date rechecks, repeated actions and save failure.
- Focused `BorrowerRequestServiceTest`: `BUILD SUCCESSFUL`.
- Full ` .\gradlew.bat clean test --no-daemon`: `BUILD SUCCESSFUL`.
- Ownership, edge-case/test, UI and independent panel reviews: `RUN`; manual
  JavaFX verification was completed by the user on 26 September 2026.

Independent panel evidence:

- Ownership/workflow reviewer: `RUN`; no service authorization defect found,
  but requested direct proof for reservation release, terminal/date boundaries,
  session boundaries and fresh cancellation metadata reload.
- Behaviour/UI reviewer: `RUN`; identified stale reason controls between
  selections, outdated empty-detail wording, inaccurate pending success text,
  and generic ineligible-action feedback. The reviewer environment could not
  run Gradle because its wrapper/network access was unavailable.

Panel findings were fixed by adding the requested service tests, H2 reload and
availability assertions, logged-out/wrong-role cancellation tests, resetting
reason controls on selection, status-aware success feedback and specific
status/date guidance. Focused and full clean suites passed after the fixes.
No second panel was run because these were scoped evidence and UI-feedback
corrections without a new workflow boundary.

Manual GUI evidence — 26 September 2026: the user tested future-request
cancellation, reason selection, confirmation, resulting `CANCELLED` status,
same-day ineligibility feedback, and clearing of `Other` reason text when
changing selection. The UI behaved as expected. This is manual JavaFX evidence;
no automated JavaFX interaction test was claimed.

## Review gap registry — 25 September 2026

Added `docs/ReviewGapRegistry.md` to preserve valid independent-review gaps,
their prevention measures and focused verification. It includes the earlier
ownership, password-save, catalogue restart/UI, request/loan integrity,
documentation, My Requests and pre-commit review-process findings. Broader
hardening recommendations and the lack of an automated JavaFX harness are
recorded separately as deferred limitations rather than defects.
