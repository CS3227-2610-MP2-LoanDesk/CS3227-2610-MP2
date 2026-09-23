# Borrower Milestones

This is Yikbing's sequential plan for completing the borrower role. It
complements `docs/ProjectChecklist.md`; it does not replace team-level
checklists or decide unresolved shared policies by itself.

Each milestone should be completed with focused tests, relevant documentation,
and a dated session log. Do not begin a later milestone while an earlier
milestone's exit criteria are still failing. Work remains limited to borrower
code unless a shared-contract gate below has been agreed with the team.

## Baseline already available

- [x] Borrower login, sign-up, session and logout foundation.
- [x] Borrower password login/sign-up with salted persisted hashes and the
      agreed 8-to-128-character rule.
- [x] H2 loading, first-run schema creation, seeding and transactional save foundation.
- [x] Placeholder borrower dashboard with catalogue, requests and loans actions.
- [x] Borrower UI, ownership, edge-case/test and change-completeness review
      skills.

## Milestone 0 — Authentication gate and session boundaries

- [ ] Keep role selection and borrower log-in/sign-up available while logged
      out.
- [ ] Require an active session before opening the borrower dashboard or any
      borrower operation.
- [ ] Ensure logout clears the session and prevents returning to protected
      screens through stale UI state.
- [ ] Enforce the same session boundary in borrower application services, not
      only in JavaFX event handlers.
- [ ] Test logged-out, logged-in and logged-out-again paths, including direct
      service calls where applicable.
- [ ] Run the ownership, edge-case/test and change-completeness reviews.

Exit criteria: only authentication entry points are available while logged
out; every protected borrower action requires a current session and rejects
direct unauthenticated calls safely.

## Milestone 1 — Confirm shared workflow contracts

- [x] Agree that the first catalogue slice supports name filtering only;
      category, condition and availability are deferred to request/loan work.
- [ ] Agree request and loan boundaries, request statuses and legal
      transitions.
- [ ] Agree date boundaries, collection windows, cancellation,
      clarification/resubmission and overdue-borrower rules.
- [x] Record the agreed catalogue scope and future status vocabulary in
      `docs/ProjectContext.md` before implementing the catalogue.

Exit criteria: the next borrower slice can be implemented without inventing a
shared policy. Shared changes have an agreed owner and review path.

## Milestone 2 — Catalogue and filtering

- [x] Load equipment through the existing persistence boundary.
- [x] Add borrower-owned catalogue application logic.
- [x] Wire the borrower dashboard's Catalogue action to a real screen.
- [x] Display equipment identifiers and names clearly.
- [x] Implement name filtering, including case handling and clearing.
- [x] Show an understandable empty-results state.
- [x] Test all records, matches, no matches and filter reset behaviour.
- [x] Run the UI and change-completeness reviews.

Exit criteria: a logged-in borrower can open the catalogue, filter it, clear
filters and understand an empty result without changing shared workflow state.

## Milestone 3 — Submit a one-item request

- [ ] Agree or confirm the request input fields and validation messages.
- [ ] Add the smallest request domain/application contract needed by the team.
- [ ] Add a borrower request form from the catalogue.
- [ ] Validate required purpose, selected equipment and agreed date rules.
- [ ] Recheck the selected equipment at submission time.
- [ ] Persist a successful request without corrupting existing data.
- [ ] Test success, invalid input, stale/unavailable equipment and save failure.
- [ ] Run ownership, edge-case/test and change-completeness reviews.

Exit criteria: valid requests persist once; invalid or stale submissions fail
clearly and do not create partial records.

## Milestone 4 — View own requests and details

- [ ] Add a borrower request list and detail view.
- [ ] Show status, equipment, dates, purpose and permitted next actions.
- [ ] Enforce borrower ownership in the service layer, not only the UI.
- [ ] Add empty, unknown, stale and wrong-role cases.
- [ ] Verify the list reloads correctly after restart.

Exit criteria: a borrower sees only their own requests and the displayed status
matches persisted state.

## Milestone 5 — Clarification, revision and cancellation

- [ ] Implement only the state transitions approved in Milestone 1.
- [ ] Allow revision/resubmission only in the agreed states.
- [ ] Allow cancellation only where policy permits and require any agreed
      reason.
- [ ] Recheck ownership and current state at every service call.
- [ ] Test repeated actions, foreign IDs, illegal transitions and no-mutation
      rejection behaviour.
- [ ] Run the ownership and edge-case/test reviews.

Exit criteria: every borrower mutation has a legal-state and ownership guard,
with focused evidence for success and safe rejection.

## Milestone 6 — Active loans and history

- [ ] Define the borrower-facing distinction between requests and loans.
- [ ] Add active-loan and history views using persisted shared records.
- [ ] Display due dates, overdue indicators and relevant return state.
- [ ] Keep borrower access read-only unless an agreed borrower action exists.
- [ ] Test empty history, restart persistence, ownership and overdue boundaries.

Exit criteria: the borrower can understand current and past borrowing without
seeing another borrower's records.

## Milestone 7 — Cross-role lifecycle verification

- [ ] Coordinate the borrower interfaces with supervisor and custodian owners.
- [ ] Run submit -> approve -> checkout -> return using synthetic data.
- [ ] Verify borrower request and history state after each transition.
- [ ] Verify restart persistence and wrong-role rejection.
- [ ] Verify overlapping availability decisions cannot create contradictory
      borrower-visible state.
- [ ] Record the integration/system evidence and any owner-assigned defects.

Exit criteria: the borrower path works in the agreed end-to-end scenario and
cross-role defects have explicit owners.

## Milestone 8 — Borrower readiness and handoff

- [ ] Run `.\gradlew.bat clean test --no-daemon` on Windows.
- [ ] Run relevant borrower skills and record findings and limitations.
- [ ] Confirm user and developer documentation matches implemented behaviour.
- [ ] Update `docs/ProjectChecklist.md`, `docs/UserGuide.md` and the final
      dated session log.
- [ ] Review local-data, packaging and clean-install assumptions with the team.
- [ ] Confirm no deliberate fixture defects or personal `data/` database files
      are included.

Exit criteria: borrower functionality, tests, documentation and integration
evidence are complete enough for team review and release planning.

## Working rule

At the start and end of each milestone, use the change-completeness review to
check that the implementation, tests, documentation and evidence agree. If a
milestone exposes a new shared policy question, pause that slice and record the
question rather than silently choosing a production rule.
