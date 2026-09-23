# LoanDesk Developer Guide

## Baseline structure

- `src/main/java`: production Java source
- `src/test/java`: automated tests
- `docs`: user and developer documentation
- `docs/ProjectContext.md`: compact context and current decisions
- `docs/BorrowerMilestones.md`: sequential borrower implementation plan
- `docs/AgenticSE.md`: borrower skills/hooks and future team proposals
- `logs/Yikbing-logs`: Yikbing's dated, verified AI-session summaries

The shared Java package layout is:

- `domain`: models, roles, and statuses independent of JavaFX and persistence
- `application`: use cases, permissions, and workflow rules
- `persistence`: shared H2 database store, repositories and first-launch initialization
- `ui/common`: role selection, login, sign-up, session, and logout
- `features/borrower`, `features/supervisor`, `features/custodian`: role-owned
  `ui/` and `application/` packages

The intended application direction is:

```text
JavaFX views/controllers -> application services -> repositories/local persistence
```

Views should collect input and display results. Services should enforce role,
ownership, and workflow rules. Persistence should remain behind repository
interfaces rather than being implemented directly in controllers.

## Branching workflow

- `main` is the shared integration branch.
- Create focused branches such as `feature/borrower-workflow`.
- Run `./gradlew test` before opening a pull request.
- Pull requests require review from at least one teammate.
- Use the repository PR template to record test results, relevant borrower
  skill reviews, independent-review evidence, data safety and shared-contract
  coordination.
- Do not merge changes that break the build or change a shared contract without
  discussing it with affected owners.

The application uses the embedded H2 dependency declared in `build.gradle`.
Normal users do not install or run a separate database server; the Gradle
application distribution supplies the H2 JAR and the application creates its
ignored local database files under `data/loandesk`. The current schema stores
users, credentials, equipment and a database revision row. A store must load
the database before saving; snapshot writes then use an atomic revision update
inside the transaction, rejecting stale or concurrent writers instead of
silently replacing another instance's newer shared update.
Request, loan, history and maintenance tables will be added through the same
shared persistence boundary as those features are implemented.

## AI-assisted development records

For each meaningful AI-assisted session, add a dated summary under
`logs/Yikbing-logs/`. Record only observed commands, results, decisions, and
files changed. These summaries are checked by a human before submission and
are not intended to replace the full conversation transcript.

## Borrower skills and personal hooks

Four skills live under `.agents/skills/`: `loandesk-borrower-ui-review`,
`loandesk-borrower-ownership-review`, and
`loandesk-borrower-edge-case-test-review`, plus
`loandesk-borrower-change-completeness-review`. Their descriptions enable automatic
selection for relevant borrower changes; this is agent selection, not background
execution. You can also invoke a skill by its `$name`. If newly created skills
are not visible, start a fresh session. Review-only requests produce findings;
fixes/tests are made only within an authorized implementation task.

For a borrower feature or milestone completion check, and for a meaningful PR,
the completeness skill arranges a fresh, read-only reviewer pass over the
relevant change diff. The reviewer must not receive the implementing agent's
conclusions and must report findings with severity, file/line references,
reasoning and next actions. Repeat it before a PR if meaningful changes were
made after the last completion check. Save decision-relevant prompt/output
evidence in a dated log. Routine substeps, small documentation edits and
ordinary test runs do not automatically invoke a fresh reviewer. This
independent pass is not launched by Git hooks; hooks stay deterministic and
local. GitHub Actions runs the repository's build/test checks, while
GitHub-native review services are an optional additional PR-comment layer.

Git for Windows supplies Bash for `tools/borrower/hooks/pre-commit` and
`pre-push`. No Python dependency is needed to run the hooks. Pre-commit checks
staged content for local data, conflict markers and whitespace, and warns about
shared/other-role source changes. Pre-push runs the full clean test suite on
the current working tree; uncommitted changes mean this is not proof that the
commits being pushed pass independently. CI checks the submitted commit.

Activation is per clone. Inspect `git config --show-origin --get core.hooksPath`
and existing `.git/hooks` first; do not replace active hooks without coordination.
When no existing configuration needs preserving:

```powershell
git config --local core.hooksPath tools/borrower/hooks
git config --local --get core.hooksPath
```

To disable this setup, first confirm that the local value is still
`tools/borrower/hooks`, then run `git config --local --unset core.hooksPath`.
This restores default/inherited hook lookup; it does not delete hook scripts.
Local hooks are bypassable and do not replace CI. Marker-like text in docs can
be flagged; inspect and rephrase intentional examples rather than auto-editing.

Reproduce the hook checks with Python 3 (test tooling only):

```powershell
python tools/borrower/test_hooks.py
git hook run pre-commit
git hook run pre-push
```

Fixtures use synthetic files in disposable repositories under ignored `build/`.
Never run `clean` concurrently with these fixtures. No real commit or push is
needed. Skills, hooks and the harness do not modify application data. Unix
execution has not been verified; a future Unix checkout may also require
executable permissions on the two hook scripts.

## First ownership skill evaluation

Follow the [beginner walkthrough](../tools/borrower/skill-evaluations/ownership/README.md).
It contains neutral case A/B examples, synthetic requirements, seven shared
JUnit tests, a reviewer prompt and a separate evaluator answer sheet.

```powershell
.\gradlew.bat -p tools/borrower/skill-evaluations/ownership verifyFixtures --no-daemon
```

An intentional test failure in case A is expected; the harness checks its exact
identity and requires all case B tests to pass. Do not run only the fixture
`test` task and interpret its exit status as acceptance: `verifyFixtures` is the
acceptance task. The application build and normal pre-push tests exclude these
fixtures. See the dated log for the independent review and assessment; use a
fresh reviewer without the answer sheet when repeating the evaluation.

JUnit runs Java assertions only; it does not send the review prompt to an AI.
The agent review and assessment are separate steps. When switching chats/models,
start with `CODEX_HANDOFF.md` for the current stopping point and user preferences.

## Open workflow policies

The team still needs to agree on date-boundary rules, request state names,
request/loan model boundaries, and workflow policy before implementing those
contracts.

See [ProjectContext.md](ProjectContext.md) for the current context snapshot and
[AgenticSE.md](AgenticSE.md) for the skill and hook proposal.
