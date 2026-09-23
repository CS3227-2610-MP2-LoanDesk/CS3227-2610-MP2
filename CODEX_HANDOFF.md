# LoanDesk Codex Handoff

Read this file first when continuing the project from a terminal.

## Resume here: 23 September 2026

The user is continuing after implementing borrower review tooling, the session
role guard, borrower password authentication, the shared H2 persistence
foundation and the first read-only borrower catalogue slice. Request, loan and
history product features remain unfinished. Do not restart completed tooling,
storage or catalogue work.

### User preferences and boundaries

- Yikbing is learning testing for the first time. Explain each step in plain
  language, including why it is needed and what the result means.
- Discuss the next implementation scope and wait for the user's instruction
  before starting new work. Once authorized, complete that scope and explain progress.
- Work only inside this repository; never edit/delete the parent MP2 files.
- Borrower only; avoid supervisor/custodian changes. Ask before shared-contract
  or major architecture changes. Keep shared rules and persistence centralized.
- Preserve existing uncommitted work and ignored personal data. Do not commit,
  push, switch branches or pull automatically as part of resuming this chat.
- Run `.\gradlew.bat clean test --no-daemon` before and after changes; update
  relevant artifacts and a dated session log. Use synthetic test data.

### Completed and verified

- Four automatically selectable skills under `.agents/skills/`: borrower UI,
  ownership, edge-case/test review, and change-completeness review. The three
  original skills passed official format validation; the fourth passed manual
  frontmatter checks because its validator lacked PyYAML. Automatic selection
  is eligible, not proven by the explicit-invocation evaluation.
- The change-completeness skill now requires or records a fresh, read-only
  independent reviewer pass for borrower feature/milestone completion checks
  and meaningful PRs. Routine substeps do not trigger it automatically. It
  explicitly keeps agent invocation out of local Git hooks.
- The edge-case skill explicitly routes borrower authentication, password,
  credential-persistence, database-save and catalogue changes; it requires a
  reported `RUN`, `UNAVAILABLE` or `NOT APPLICABLE` result and is not a
  background watcher or Git hook. See the dated skill-improvement log.
- The change-completeness skill now uses an adaptive independent review panel
  of at most three focused reviewers for meaningful milestone/PR checks. It
  records each member as `RUN`, `UNAVAILABLE` or `INCONCLUSIVE` and never calls
  a partial panel complete. See the multi-reviewer skill log.
- `.github/PULL_REQUEST_TEMPLATE.md` records tests, relevant skills,
  independent-review evidence, data safety and shared-contract coordination.
- Pre-commit and pre-push hooks are active only in this checkout through
  `core.hooksPath=tools/borrower/hooks`. Nine hook fixture tests passed.
- Ownership exercise: `tools/borrower/skill-evaluations/ownership/README.md`.
  Standalone Gradle project; not included in normal application tests.
- Same seven JUnit tests run against two synthetic Java services. Case A fails
  only foreign-owner rejection; case B passes seven. `verifyFixtures` checks
  these exact outcomes and succeeds only when the expected pattern occurs.
- A separately invoked fresh reviewer, explicitly approved by the user, read
  the skill, requirements and cases without conversation history or answer sheet.
  It detected the missing owner check in A and reported no defect in B.
- IMPORTANT: JUnit did NOT invoke the agent or skill. JUnit tested Java code;
  the agent review was run separately and assessed against an answer sheet.
  Calling this a unit-level skill evaluation does not make it a JUnit skill test.
- Evidence: `logs/Yikbing-logs/2026-09-21-ownership-skill-evaluation.md` and its
  linked prompt, original response and saved JUnit XML. No human sign-off yet.

### First actions for the next chat

1. Read this handoff, `docs/ProjectContext.md`, `docs/ProjectChecklist.md`,
   `docs/DeveloperGuide.md`, `docs/AgenticSE.md`, and the ownership evaluation log.
2. Inspect `git status --short --branch` and `git log --oneline --decorate -5`.
   Report actual status; the H2 migration is committed and the catalogue slice
   may have staged work awaiting its own commit.
3. Report the catalogue implementation summary and actual test status before
   any further changes. Discuss the exact files and scope before editing.
4. The next borrower milestone is the remaining active-session gate for
   protected operations, followed by the catalogue contract and implementation.
   Integration/system evaluation, automatic-selection checks, repeat runs and
   detailed reflections remain pending. Do not claim they have been done.
5. Preserve the user's local data and staged work; do not commit, push, pull or
   switch branches automatically.

To reproduce the completed fixture check from the repository root:

```powershell
.\gradlew.bat -p tools/borrower/skill-evaluations/ownership verifyFixtures --no-daemon
```

One reported JUnit failure in case A is intentional. Never fix that isolated
fixture merely to make all tests green. Normal Gradle clean removes generated
reports, but preserved XML and review evidence remain in logs.

### Model suggestion (advice, not a configuration change)

As of 21 September 2026, Luna (`gpt-5.6-luna`) with Medium reasoning is the
suggested economical starting point for the next narrow evaluation. Terra
(`gpt-5.6-terra`) with Medium reasoning is recommended for broader feature
implementation. Escalate for difficult shared-rule/debugging work. These are
task-based recommendations, not measured LoanDesk benchmarks. No model setting
was changed. Source: https://learn.chatgpt.com/docs/models

## Repository

- Repository: `CS3227-2610-MP2`
- Remote: `https://github.com/CS3227-2610-MP2-LoanDesk/CS3227-2610-MP2.git`
- Default branch: `main`
- Inspected on 23 September 2026: branch `yikbing` is based on merged
  `origin/main` at `f2748da`; H2 migration commit is `f28e1d2`.
- Recent commits include `f28e1d2 migrate LoanDesk storage from JSON to H2`,
  `715e1b7 docs(agentic): formalize independent review checkpoints` and
  `f2748da Merge pull request #1 ...`.
- Work from the repository root, not the parent `MP2` directory.

## Toolchain and commands

- Java SE 25
- Gradle 9.1.0 through the committed wrapper
- JavaFX 21.0.6
- JUnit 5

PowerShell commands from the repository root:

```powershell
.\gradlew.bat clean test --no-daemon
.\gradlew.bat run --no-daemon
```

The clean test suite has passed after the H2 persistence migration and the
catalogue implementation. Run it before claiming a change is ready.

## Current product foundation

The shared foundation currently provides:

- Three role buttons: `Borrower`, `Supervisor`, `Custodian`
- Borrower `Log in` and `Sign up`
- Borrower password login/sign-up with salted persisted hashes
- Fixed supervisor and custodian entry pending their password-login work
- Session and logout handling
- Placeholder three-button dashboards
- Embedded H2 persistence rooted at `data/loandesk`
- First-launch schema and seed data: `testBorrower1`, `testBorrower2`, `camera1`, `camera2`
- Transactional H2 saves through the shared data-store boundary
- A fresh H2 database is used; old ignored JSON files are not imported
- Tests for authentication, persistence, and username rules
- Read-only borrower catalogue loaded through `DataStore`
- Case-insensitive catalogue name filtering, clearing and empty-result feedback

Request submission, approval, checkout, return, maintenance and loan-history
features are not implemented yet. Catalogue category, condition and derived
availability remain deferred until those workflows exist.

Professor feedback changed the borrower plan: authentication must gate all
protected dashboards and operations, including direct service calls. Login,
sign-up and role selection remain the unauthenticated entry points.

## Package layout

The Java namespace was intentionally flattened to `loandesk`:

```text
src/main/java/loandesk/
  LoanDeskApp.java
  domain/          shared models and roles
  application/     shared session/authentication logic
  persistence/     shared H2 database storage
  features/
    borrower/
      ui/
      application/
    supervisor/
      ui/
      application/
    custodian/
      ui/
      application/
```

The role feature folders contain README files and placeholders. Put role-owned
UI and role-specific application use cases in the relevant feature area. Keep
shared models, permissions, availability rules, and persistence centralized.

## Data and Git

`data/`, `.gradle/`, and `build/` are ignored. Do not commit local H2 database
files under `data/`; each developer should have independent local test data.
Do not add a hook that deletes data automatically. Tests should use temporary
folders.

The repository also contains `.github/workflows/ci.yml` and shared Java settings
under `.vscode/settings.json`. The parent workspace's `.github/modernize` and
parent `.vscode` folder are not part of this repository.

## Documentation artifacts

- `docs/ProjectContext.md`: decisions and architecture context
- `docs/ProjectChecklist.md`: end-to-end progress checklist
- `docs/DeveloperGuide.md`: contributor and package guidance
- `docs/UserGuide.md`: current user instructions
- `docs/AgenticSE.md`: implemented borrower skills/hooks and future team proposals
- `docs/BorrowerMilestones.md`: sequential borrower implementation milestones
- `logs/Yikbing-logs/`: verified summaries of Yikbing's AI sessions

Four borrower `SKILL.md` files now live in `.agents/skills/`; the third covers
edge cases and existing test failures, and the fourth covers change
completeness. Automatic selection is enabled by default.
Two personal hooks live in `tools/borrower/hooks/`, with a reproducible harness
at `tools/borrower/test_hooks.py`. Activated in Yikbing's checkout on 21 September
2026 via local `core.hooksPath=tools/borrower/hooks`; activation is not cloned.
See the Developer Guide for local activation
and removal. The first controlled ownership skill evaluation is under
`tools/borrower/skill-evaluations/ownership/`, with evidence in the
21 September ownership evaluation log. One fresh reviewer detected the planted
defect and accepted the correct case; other skill evaluations, repeat runs,
`docs/Reflections.md`, release packaging, website, and cross-role workflows remain.

## Next work

Yikbing owns only the borrower role. Avoid supervisor/custodian edits unless
unavoidable, and discuss shared-contract or major architecture changes first.
When asked about skills or hooks again, consult **Yikbing's borrower setup**
in `docs/AgenticSE.md` and the 21 September implementation log. The four
skills and two hooks were explicitly authorized for implementation. The
completeness skill and PR template now document the independent-review workflow.
GitHub Actions already runs build/test checks; GitHub Copilot or another
GitHub-integrated reviewer still requires repository/account-owner setup and
is not configured by this local change.

1. Inspect and preserve uncommitted work before updating from `main`; do not
   switch branches or pull over existing work blindly.
2. Agree on request/loan models, statuses, date boundaries, clarification and
   cancellation policy, and permissions before changing shared contracts.
3. Create a feature branch for one role.
4. Implement and test role work inside its feature area while coordinating any
   shared-package changes.
5. Keep the checklist and session logs truthful.

Example:

```powershell
git switch main
git pull origin main
git switch -c feature/borrower-workflow
.\gradlew.bat clean test --no-daemon
```

## Important correction history

The Java package was deliberately changed from
`sg.edu.nus.cs3227.loandesk` to `loandesk`. If VS Code displays the old path,
close the stale editor tab and reopen the file from `src/main/java/loandesk/`,
or run `Java: Clean Java Language Server Workspace`.

A previous push attempt failed with HTTP 403 because the GitHub account
`Yikbing` lacked repository write permission. The repository later reached a
clean state matching `origin/main`; if pushing again fails, ask the organization
owner to grant write access.
