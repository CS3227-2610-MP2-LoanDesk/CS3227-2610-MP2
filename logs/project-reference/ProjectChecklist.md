# LoanDesk Project Checklist

**Status date:** 28 September 2026

This is the end-to-end checklist for the project. Check items only when they
are implemented and verified. Keep decisions and evidence truthful.

## 1. Repository and foundation

- [x] Clone the team repository.
- [x] Establish `main` as the shared integration branch.
- [x] Add Java 25 Gradle configuration.
- [x] Add and verify the Gradle 9.1.0 wrapper.
- [x] Add JavaFX application entry point.
- [x] Add JUnit test setup and a passing baseline test.
- [x] Add JavaFX/Gradle README instructions.
- [x] Add `.gitignore` entries for build, IDE, and local data files.
- [x] Add initial CI workflow.
- [x] Review foundation with the team.
- [ ] Push the committed foundation branch and open its pull request.
- [ ] Tag the shared baseline, for example `foundation-0.1`.
- [ ] Protect `main` and require pull-request review.

## 2. Shared decisions before feature branches

- [x] Choose embedded H2 persistence for the shared local database.
- [x] Choose `data/loandesk` as the local database path; keep generated files ignored.
- [x] Create the database schema and seed records on first launch when it is empty.
- [x] Preserve existing database records and do not reseed on later launches.
- [x] Use JDBC transactions and prepared statements for database writes.
- [x] Keep supervisor and custodian as fixed singleton roles.
- [x] Set borrower username maximum length to 30 characters.
- [x] Set username rules: trim, case-insensitive uniqueness, no internal spaces,
  letters/numbers/underscores/hyphens only, and no blank values.
- [x] Decide that coherent demo data is seeded only on first launch.
- [x] Choose initial demo accounts and a ten-item, booking-free equipment catalogue.
- [x] Defer complex request, damage, and maintenance demo scenarios until after
  feature branches are created.
- [ ] Decide local data location, reset, recovery, and schema version behaviour.
- [x] Finalize request statuses and legal transitions.
- [x] Decide whether requests and loans are separate records.
- [x] Finalize date boundary and collection-window rules.
- [x] Finalize cancellation, clarification, and resubmission rules.
- [x] Decide how overdue borrowers affect approval.
- [x] Define initial seeded accounts and equipment.
- [ ] Define cross-role demonstration data after workflow features exist.
- [ ] Agree on shared service/repository interfaces before implementation.

## 3. Shared application foundation

- [x] Implement shared role selection, borrower login/sign-up, session identity,
      logout, and placeholder dashboards.
- [x] Add borrower password sign-up/login with salted persisted hashes.
- [x] Add supervisor password login; custodian password login remains with
      its owner.
- [ ] Enforce an active-session gate before protected dashboards and role
      operations, including direct service calls and logout re-entry.
- [x] Implement first-launch H2 schema creation and seeding for the agreed initial records.
- [x] Implement username validation and duplicate prevention.
- [x] Add tests for authentication, persistence, and username rules.
- [x] Implement the first shared request/loan domain models and basic validation.
- [x] Implement permission checks in services, not only in the UI.
- [x] Implement request lifecycle rules in one shared place.
- [x] Implement the first shared availability precedence calculation.
- [ ] Implement repository interfaces and consistent error handling.
- [x] Add the coordinated shared request/loan domain and additive H2 tables.
- [x] Add read-only eligibility and availability queries for borrower services.
- [ ] Add logging that excludes passwords and sensitive credentials.
- [x] Add meaningful unit tests for shared rules.

## 4. Borrower workflow

See the sequential [borrower milestone plan](BorrowerMilestones.md) for
implementation order and exit criteria.

- [x] Catalogue and equipment name filtering.
- [ ] Availability search by name, category, and dates.
- [x] Submit one-item request with purpose and date validation.
- [x] View own requests and request details.
- [x] Edit purpose and dates on eligible pending requests.
- [ ] Read rejection reasons and create a new request after rejection.
- [x] Cancel only where policy permits.
- [x] View active loans, due dates, overdue indicators, and history.
- [ ] Test ownership, invalid input, prohibited edits, filters, duplicate
  pending requests, eligibility blockers, stale availability and state rules.

## 5. Supervisor workflow

- [x] Review queue with status/date/borrower filters.
- [x] Inspect request, purpose, item, dates, and relevant loans.
- [x] Descope clarification and resubmission for the initial workflow, per the
      agreed request policy.
- [x] Approve only after availability and eligibility recheck.
- [x] Reject with a required reason.
- [x] Preserve decision history, actor, time, and reason.
- [x] Cancel an approved uncollected booking with a reason.
- [x] Expire an approved request that was not collected by the end of its
      start date, releasing the reservation.
- [x] Test conflicts, unavailable items, wrong-role calls, and invalid transitions.
- [ ] Manually verify the supervisor screens; the project has no JavaFX
      interaction harness.

## 6. Custodian workflow

- [ ] Add and edit uniquely identified equipment.
- [ ] Manage service status and preserve retired-item history.
- [ ] View approved collections.
- [ ] Check out only approved and issuable equipment.
- [ ] Prevent duplicate issuance.
- [ ] View active and overdue loans.
- [ ] Record return time, condition, and notes.
- [ ] Create, update, and resolve basic maintenance records.
- [ ] Block damaged/unavailable equipment until restored.
- [ ] Test save failure and multi-record consistency.

## 7. Agentic SE implementation and evidence

- [x] Create five borrower review `SKILL.md` files under `.agents/skills/`.
- [x] Add a skill-evaluation manifest, controlled case for each skill and
      deterministic repository-side contract tests (27 September 2026).
- [ ] Evaluate the UI acceptance reviewer with a controlled fixture.
- [x] Evaluate borrower ownership review on one controlled defective/correct pair
      with direct foreign-owner calls (21 September 2026).
- [x] Create the supervisor permission/workflow `SKILL.md`, its controlled
      defective/correct case pair and repository-side contract tests
      (28 September 2026).
- [x] Run a fresh reviewer against the supervisor permission/workflow case and
      record the prompt, response and assessment (28 September 2026): one
      defect planted, one detected, no misses, no unsupported findings.
- [ ] Broaden permission/workflow evaluation to integration and real lifecycle rules.
- [ ] Evaluate the persistence-failure tester with injected save failure.
- [ ] Evaluate the cross-role scenario tester on the acceptance journey.
- [ ] Record prompts, outputs, verification, corrections, and limitations.
- [ ] Run fresh reviewer evaluations for the remaining controlled cases and
      separately verify automatic skill selection.
- [x] Create `docs/Reflections.md` with at least three detailed skill reflections.
- [ ] Add a dated summary under `logs/Yikbing-logs/` for each meaningful session.

## 8. Hooks, quality, and delivery automation

- [x] Choose versioned personal hooks under `tools/borrower/hooks/`, activated per checkout.
- [x] Add and fixture-test pre-commit data, conflict-marker and whitespace checks with scope warnings.
- [ ] Add fast pre-commit formatting and unit-test checks if agreed.
- [ ] Add commit-message convention if agreed.
- [x] Add pre-push full clean-test check and verify success/failure propagation.
- [x] Activate personal hooks in Yikbing's checkout and run the real pre-push check.
- [x] Expand pull-request CI to build, test and package, and to run the skill
      contract checks and the controlled fixture pair.
- [ ] Add a formatting check to CI; no formatter is configured in `build.gradle`
      yet, so the team must agree on one first.
- [ ] Add dependency/security scanning where supported.
- [ ] Add documentation-path/completeness checks.
- [x] Add release workflow for the Gradle-generated platform JAR artifacts.

## 9. Integration and final verification

- [ ] Run borrower submit -> supervisor approve -> custodian checkout -> return.
- [ ] Restart and verify borrower history and shared state persist.
- [x] Verify wrong-role operations are denied for supervisor operations;
      custodian operations do not exist yet.
- [x] Verify overlapping approvals cannot both succeed.
- [ ] Verify damaged returns block lending until maintenance resolves them.
- [ ] Verify demonstration data remains coherent after role switching.
- [ ] Test clean installation and packaging on relevant operating systems.
- [ ] Record what cross-platform checks were actually performed.
- [x] Keep `docs/UserGuide.md` aligned with released behaviour.
- [ ] Complete `docs/DeveloperGuide.md` and architecture diagrams if needed.
- [ ] Publish the product website on GitHub Pages.
- [ ] Review acknowledgements for reused code, ideas, and documentation.
- [ ] Review all logs and reflections for accuracy.
- [ ] Create the formal GitHub release.
- [ ] Freeze the submitted repository and do not modify it after submission.

## Current artifact inventory

- `README.md`: project overview and local commands
- `build.gradle`, `settings.gradle`: Gradle project configuration
- `gradlew`, `gradlew.bat`, `gradle/wrapper/`: reproducible Gradle wrapper
- `.github/workflows/ci.yml`: pull-request/push workflow building, testing and
  packaging the application, and checking the skill evaluation contracts
- `src/main/java/...`: JavaFX launcher, shared domain, application, and H2
  persistence foundation
- `src/test/java/...`: baseline, authentication, persistence, and username tests
- `ProjectContext.md`: durable context snapshot
- `BorrowerMilestones.md`: sequential borrower implementation milestones
- `.agents/skills/`: five borrower review skills and the supervisor
  permission/workflow review skill, eligible for automatic selection
- `tools/borrower/`: personal hooks and disposable-repository test harness
- `tools/supervisor/`: supervisor skill contract tests and the controlled
  permission/workflow evaluation fixture
- `../../docs/AgenticSE.md`: implemented borrower tooling and future team proposals
- `../../docs/DeveloperGuide.md`: architecture and contribution guidance
- `../../docs/UserGuide.md`: current borrower and supervisor setup and workflow guide
- `logs/Yikbing-logs/`: personal dated AI-session summaries
- `logs/Supervisor-logs/`: dated supervisor-lane AI-session summaries

## Verified current progress

- Working branch `yikbing` is based on the merged `origin/main` and contains
  the borrower request-editing, responsive-UI and reflection changes.
- The H2 migration, catalogue, request submission, request cancellation,
  request editing and borrower loan-history work are implemented for the
  borrower scope; supervisor approval and custodian checkout/return features
  remain dependent on their role owners.
- Java 25.0.4.1 is installed.
- `gradlew.bat clean test --no-daemon` passes after the shared request-foundation
  changes.
- Shared role selection, borrower login/sign-up, staff entry, logout, and
  placeholder dashboards are implemented.
- H2 persistence currently stores users, password credentials and equipment in
  one role-aware local database, now extended with request/loan tables and
  equipment condition. Existing JSON files are not imported.
- Supervisor approval, rejection, booking cancellation, decision history and
  request expiry are implemented and tested; the catalogue and borrower
  request/loan views are implemented. Custodian checkout, return, inventory and
  maintenance features do not exist yet.
- The shared permission matrix and shared request lifecycle now exist and are
  used by both the borrower and supervisor services. The custodian owner is
  expected to adopt the same mechanism.
- The supervisor screens have not been exercised manually; they are covered
  only by compilation and by the service tests behind them.
- Five borrower skills and two hook scripts now exist. Hook fixture tests pass.
- Repository-side contract tests now cover all five skills and their controlled
  evaluation cases. The first ownership skill fixture evaluation is complete;
  fresh reviewer runs for the other borrower cases, automatic-selection checks
  and integration/system evaluations remain unfinished. The supervisor
  permission/workflow skill has one completed fresh-reviewer evaluation.
- Release automation now creates four platform-specific JAR assets. Actual
  release tagging and clean-machine verification on each platform remain
  outstanding; `Reflections.md` is complete and committed.
