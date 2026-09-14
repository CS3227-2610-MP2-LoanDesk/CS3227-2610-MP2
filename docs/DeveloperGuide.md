# LoanDesk Developer Guide

## Baseline structure

- `src/main/java`: production Java source
- `src/test/java`: automated tests
- `docs`: user and developer documentation
- `docs/ProjectContext.md`: compact context and current decisions
- `docs/AgenticSE.md`: proposed skills, hooks, and evaluation evidence
- `logs/Yikbing-logs`: Yikbing's dated, verified AI-session summaries

The shared Java package layout is:

- `domain`: models, roles, and statuses independent of JavaFX and JSON
- `application`: use cases, permissions, and workflow rules
- `persistence`: JSON data store, repositories, and first-launch initialization
- `ui/common`: role selection, login, sign-up, session, and logout
- `ui/borrower`, `ui/supervisor`, `ui/custodian`: role-specific screens

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
- Do not merge changes that break the build or change a shared contract without
  discussing it with affected owners.

## AI-assisted development records

For each meaningful AI-assisted session, add a dated summary under
`logs/Yikbing-logs/`. Record only observed commands, results, decisions, and
files changed. These summaries are checked by a human before submission and
are not intended to replace the full conversation transcript.

## Open decisions

The team still needs to agree on date-boundary rules, request state names,
request/loan model boundaries, and workflow policy before implementing those
contracts.

See [ProjectContext.md](ProjectContext.md) for the current context snapshot and
[AgenticSE.md](AgenticSE.md) for the skill and hook proposal.
