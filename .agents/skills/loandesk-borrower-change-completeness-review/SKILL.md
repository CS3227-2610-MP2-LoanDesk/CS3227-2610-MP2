---
name: loandesk-borrower-change-completeness-review
description: Review whether a LoanDesk borrower change has the necessary focused tests, documentation, session evidence, shared-contract coordination and independent PR review evidence. Use after borrower feature, test, UI, service or persistence changes, or for a borrower milestone readiness check.
---

# Borrower change-completeness review

Read `docs/ProjectContext.md`, `docs/ProjectChecklist.md` and the relevant
requirements before reviewing. Inspect the current diff and changed files from
the repository root. Keep the review limited to the borrower change and its
direct shared dependencies; do not edit the parent workspace or other roles.

## Review trigger and depth

Use the full completion review, including a fresh independent reviewer, when
the user asks whether a borrower feature or milestone is complete, whether the
work is ready to move to the next milestone, or whether it is ready for a pull
request. Repeat it before a PR if meaningful changes were made after the last
completion check.

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

## Independent PR review

For a meaningful borrower change that is intended for a pull request, arrange
one fresh, read-only reviewer pass before declaring the change PR-ready. This
is a separate agent invocation, not the same conversation that implemented the
change. Give the reviewer only the repository path, relevant requirements and
the staged diff (or a verified PR diff); do not provide suspected defects,
expected answers or conclusions from the first review. Ask it not to edit,
commit, push, change local data or treat test fixtures as production policy.

The independent reviewer should report severity, file and line, reproduction
or reasoning, expected versus observed behaviour, and a focused next action.
Record whether the pass was run, unavailable or inconclusive. Never describe
an unavailable review as a pass. For a meaningful review, retain the prompt,
response and verification evidence in a dated `logs/Yikbing-logs/` entry when
the result affects the milestone or PR decision.

This skill may coordinate the reviewer, but it does not itself guarantee that
another agent was invoked. Git hooks must remain deterministic and local; they
must not spawn agents or make network calls. GitHub CI remains authoritative
for build and test checks, while GitHub-native or third-party review comments
are an additional PR review layer rather than a replacement for this
independent pass.

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
