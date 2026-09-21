# LoanDesk Project Checklist

**Status date:** 21 September 2026

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
- [x] Commit and push the foundation to `main`.
- [ ] Tag the shared baseline, for example `foundation-0.1`.
- [ ] Protect `main` and require pull-request review.

## 2. Shared decisions before feature branches

- [x] Choose JSON persistence.
- [x] Choose `data/loandesk.json` as the local data path.
- [x] Create the data file on first launch when it is absent.
- [x] Preserve existing data and do not reseed on later launches.
- [x] Reject malformed JSON without overwriting the original file.
- [x] Use temporary-file replacement for normal saves.
- [x] Keep supervisor and custodian as fixed singleton roles.
- [x] Set borrower username maximum length to 30 characters.
- [x] Set username rules: trim, case-insensitive uniqueness, no internal spaces,
  letters/numbers/underscores/hyphens only, and no blank values.
- [x] Decide that coherent demo data is seeded only on first launch.
- [x] Choose initial demo names `testBorrower1`, `testBorrower2`, `camera1`,
  and `camera2`.
- [x] Defer complex request, damage, and maintenance demo scenarios until after
  feature branches are created.
- [ ] Decide local data location, reset, recovery, and schema version behaviour.
- [ ] Finalize request statuses and legal transitions.
- [ ] Decide whether requests and loans are separate records.
- [ ] Finalize date boundary and collection-window rules.
- [ ] Finalize cancellation, clarification, and resubmission rules.
- [ ] Decide how overdue borrowers affect approval.
- [x] Define initial seeded accounts and equipment.
- [ ] Define cross-role demonstration data after workflow features exist.
- [ ] Agree on shared service/repository interfaces before implementation.

## 3. Shared application foundation

- [x] Implement shared role selection, borrower login/sign-up, session identity,
      logout, and placeholder dashboards.
- [x] Add borrower password sign-up/login with salted persisted hashes.
- [ ] Add supervisor and custodian password login through their role owners.
- [ ] Enforce an active-session gate before protected dashboards and role
      operations, including direct service calls and logout re-entry.
- [x] Implement first-launch JSON seeding for the agreed initial records.
- [x] Implement username validation and duplicate prevention.
- [x] Add tests for authentication, persistence, and username rules.
- [ ] Implement domain models and validation rules.
- [ ] Implement permission checks in services, not only in the UI.
- [ ] Implement request lifecycle rules in one shared place.
- [ ] Implement shared availability/conflict calculation.
- [ ] Implement repository interfaces and consistent error handling.
- [ ] Add logging that excludes passwords and sensitive credentials.
- [ ] Add meaningful unit tests for shared rules.

## 4. Borrower workflow

See the sequential [borrower milestone plan](BorrowerMilestones.md) for
implementation order and exit criteria.

- [ ] Catalogue and equipment filtering.
- [ ] Availability search by name, category, and dates.
- [ ] Submit one-item request with purpose and date validation.
- [ ] View own requests and request details.
- [ ] Read clarification, revise, and resubmit.
- [ ] Cancel only where policy permits.
- [ ] View active loans, due dates, overdue indicators, and history.
- [ ] Test ownership, invalid input, prohibited edits, filters, and state rules.

## 5. Supervisor workflow

- [ ] Review queue with status/date/borrower filters.
- [ ] Inspect request, purpose, item, dates, and relevant loans.
- [ ] Ask for clarification.
- [ ] Approve only after availability and eligibility recheck.
- [ ] Reject with a required reason.
- [ ] Preserve decision history, actor, time, and reason.
- [ ] Cancel an approved uncollected booking with a reason.
- [ ] Test conflicts, unavailable items, wrong-role calls, and invalid transitions.

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

- [x] Create four borrower review `SKILL.md` files under `.agents/skills/`.
- [ ] Evaluate the UI acceptance reviewer with a controlled fixture.
- [x] Evaluate borrower ownership review on one controlled defective/correct pair
      with direct foreign-owner calls (21 September 2026).
- [ ] Broaden permission/workflow evaluation to integration and real lifecycle rules.
- [ ] Evaluate the persistence-failure tester with injected save failure.
- [ ] Evaluate the cross-role scenario tester on the acceptance journey.
- [ ] Record prompts, outputs, verification, corrections, and limitations.
- [ ] Create `docs/Reflections.md` with at least three detailed skill reflections.
- [ ] Add a dated summary under `logs/Yikbing-logs/` for each meaningful session.

## 8. Hooks, quality, and delivery automation

- [x] Choose versioned personal hooks under `tools/borrower/hooks/`, activated per checkout.
- [x] Add and fixture-test pre-commit data, conflict-marker and whitespace checks with scope warnings.
- [ ] Add fast pre-commit formatting and unit-test checks if agreed.
- [ ] Add commit-message convention if agreed.
- [x] Add pre-push full clean-test check and verify success/failure propagation.
- [x] Activate personal hooks in Yikbing's checkout and run the real pre-push check.
- [ ] Expand pull-request CI to build, test, format, and package.
- [ ] Add dependency/security scanning where supported.
- [ ] Add documentation-path/completeness checks.
- [ ] Add release workflow for the Gradle-generated distributable artifact.

## 9. Integration and final verification

- [ ] Run borrower submit -> supervisor approve -> custodian checkout -> return.
- [ ] Restart and verify borrower history and shared state persist.
- [ ] Verify wrong-role operations are denied.
- [ ] Verify overlapping approvals cannot both succeed.
- [ ] Verify damaged returns block lending until maintenance resolves them.
- [ ] Verify demonstration data remains coherent after role switching.
- [ ] Test clean installation and packaging on relevant operating systems.
- [ ] Record what cross-platform checks were actually performed.
- [ ] Keep `docs/UserGuide.md` aligned with released behaviour.
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
- `.github/workflows/ci.yml`: initial pull-request/push test workflow
- `src/main/java/...`: JavaFX launcher, shared domain, application, and JSON
  persistence foundation
- `src/test/java/...`: baseline, authentication, persistence, and username tests
- `docs/ProjectContext.md`: durable context snapshot
- `docs/BorrowerMilestones.md`: sequential borrower implementation milestones
- `.agents/skills/`: four borrower review skills, eligible for automatic selection
- `tools/borrower/`: personal hooks and disposable-repository test harness
- `docs/AgenticSE.md`: implemented borrower tooling and future team proposals
- `docs/DeveloperGuide.md`: architecture and contribution guidance
- `docs/UserGuide.md`: current user setup placeholder
- `logs/Yikbing-logs/`: personal dated AI-session summaries

## Verified current progress

- Working branch `yikbing` at `bb25354` on 21 September 2026, with uncommitted work.
- HEAD matches locally recorded `origin/main`; no remote fetch in this session.
- Java 25.0.4.1 is installed.
- `gradlew.bat test --no-daemon` passes.
- Shared role selection, borrower login/sign-up, staff entry, logout, and
  placeholder dashboards are implemented.
- Role-specific catalogue, approval, checkout, return, and maintenance features
  do not exist yet.
- Four borrower skills and two hook scripts now exist. Hook fixture tests pass.
- First ownership skill fixture evaluation completed; other skills, repeat runs,
  automatic-selection checks and integration/system evaluations remain unfinished.
- `Reflections.md` and release packaging remain unfinished.
