---
name: loandesk-borrower-change-completeness-review
description: Review whether a LoanDesk borrower change has the necessary focused tests, documentation, session evidence, shared-contract coordination and independent PR review-panel evidence. Use after borrower feature, test, UI, service or persistence changes, or for a borrower milestone readiness check.
---

# Borrower change-completeness review

Read `docs/ProjectContext.md`, `docs/ProjectChecklist.md` and the relevant
requirements before reviewing. Also read `docs/ReviewGapRegistry.md` and check
whether its known gap patterns apply to the current change. Inspect the current
diff and changed files from the repository root. Keep the review limited to the
borrower change and its direct shared dependencies; do not edit the parent
workspace or other roles.

## Review trigger and depth

Use the full completion review, including a fresh independent reviewer, when
the user asks whether a borrower feature or milestone is complete, whether the
work is ready to move to the next milestone, or whether it is ready for a pull
request. For every meaningful borrower feature or milestone slice, run this
review before its commit is finalized. Repeat it before a PR if meaningful
changes were made after the last completion check.

For an ordinary implementation step, run only the relevant focused checks and
the applicable detailed review skills. Do not invoke a fresh reviewer for a
small documentation edit, routine test run or intermediate substep unless the
user specifically requests a review. This keeps independent review focused on
decision points rather than every incremental edit.

First identify the change surface: UI, borrower application service, shared
domain/application contract, persistence, tests, and user/developer-facing
documentation. Do not assume every category needs an update. Require evidence
only when the change affects that category.

For each affected category, check:

- Focused tests cover the main successful behaviour, important rejection or
  boundary cases, and state/persistence effects that the requirements promise.
- Existing tests still exercise the changed contract and assertions check
  observable outcomes rather than only implementation details.
- User documentation describes newly available borrower behaviour and does not
  promise unimplemented features.
- Developer documentation, checklist items and architecture decisions are
  updated only when the change alters the documented product or design state.
- A dated `logs/Yikbing-logs/` entry records meaningful commands, results,
  decisions, files changed and limitations; do not require a log for trivial
  no-op or exploratory edits.
- Shared model, permission, availability or persistence changes are identified
  for coordination before implementation. Do not silently duplicate shared
  rules in borrower code or edit supervisor/custodian features.

Run the relevant tests during an authorized implementation or milestone review.
For code changes, follow the project command `.\gradlew.bat clean test
--no-daemon` on Windows. Do
not run destructive cleanup outside the repository, and do not use
`data/loandesk.json` as test data. Report commands and actual results; mark
suggested tests or documentation as proposed rather than complete.

## Independent PR review panel

For a meaningful borrower change intended for a commit or pull request, arrange
a fresh, read-only independent review panel before declaring the change
complete or PR-ready. Use a maximum of three reviewers and choose the smallest
panel covering the change surface. Do not spawn a panel for a routine test run,
small documentation edit or trivial formatting-only change.
Each reviewer is a separate agent invocation, not the implementing conversation
or another panel member. Give each only the repository path, relevant
requirements and the staged diff (or verified PR diff). Do not provide
suspected defects, expected answers, prior conclusions or other reviewers'
reports. Ask every reviewer not to edit, commit, push, change local data or
treat test fixtures as production policy.

Choose focused reviewers rather than asking every reviewer to repeat the whole
review:

- UI/service or catalogue changes: one UI/interaction reviewer and one
  behaviour/test reviewer.
- Authentication, password or persistence changes: one authentication/security
  reviewer, one persistence/transaction/concurrency reviewer and one
  completeness/shared-contract reviewer.
- Ownership or request/loan changes: one ownership/workflow reviewer and one
  behaviour/test reviewer; add a persistence reviewer when storage changes.

The independent reviewer should report severity, file and line or symbol,
reproduction or reasoning, expected versus observed behaviour, focused next
action and limitations. Record each panel member as `RUN`, `UNAVAILABLE` or
`INCONCLUSIVE`, including the actual prompt and response evidence. A panel with
one unavailable member may provide partial evidence, but must not be described
as a complete panel pass. The main agent should reconcile duplicate findings,
record disagreements, map each finding to a focused test or decision, and
rerun affected checks before deciding readiness.

For a meaningful review, retain the panel prompts, responses and
decision-relevant verification evidence in a dated `logs/Yikbing-logs/` entry.
Never describe an unavailable or inconclusive reviewer as a pass.

After reconciling the panel, update `docs/ReviewGapRegistry.md` with every
valid new gap, its root cause, prevention and focused verification. Mark a gap
as resolved only after the relevant fix and test evidence exist. Record broader
recommendations or environment limitations in the deferred section instead of
turning them into unsupported defects.

If the panel identifies a valid defect, fix it and rerun the affected focused
tests plus the full clean suite before committing. Run another independent
panel only when the fix materially changes behavior or addresses a major
finding; otherwise record the original panel, the fix and its verification in
the session log.

This skill may coordinate the panel, but it does not itself guarantee that
agents return successfully. Git hooks must remain deterministic and local; they
must not spawn agents or make network calls. GitHub CI remains authoritative for
build and test checks, while GitHub-native or third-party review comments are an
additional PR review layer rather than a replacement for this independent
panel.

Output a compact change-surface/evidence table, then findings grouped as:
missing test, missing or stale documentation, missing session evidence,
coordination needed, missing independent review, or no action required.
Include severity, file references, the reason the update matters, and a focused
next action. Distinguish observed gaps from recommendations. A review-only
request reports without editing; an authorized implementation may make only
the necessary in-scope updates.

Use the existing UI, ownership and edge-case/test review skills when their
triggers also apply. This skill checks completeness across the change; it does
not replace their detailed behavioural reviews and does not claim exhaustive
coverage from a checklist alone.
