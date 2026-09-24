# 2026-09-21 independent PR review workflow

## Scope

Added a repeatable independent-review expectation for meaningful borrower pull
requests and a GitHub pull-request template that records review evidence. No
product behaviour, shared Java contract, local data file or Git history was
changed.

## Decisions

- Extend `loandesk-borrower-change-completeness-review` rather than create a
  duplicate borrower PR skill.
- Require a fresh, read-only reviewer pass for meaningful borrower PRs, with no
  inherited implementing-agent conclusions.
- Keep the reviewer outside Git hooks. Hooks remain deterministic, local and
  offline-capable.
- Use the existing GitHub Actions workflow for build/test checks. GitHub-native
  or third-party AI review comments remain optional and are not configured by
  this local repository change.
- Add `.github/PULL_REQUEST_TEMPLATE.md` so the PR records tests, relevant
  skills, independent-review evidence, data safety and shared-contract
  coordination.

## Files changed

- `.agents/skills/loandesk-borrower-change-completeness-review/SKILL.md`
- `.github/PULL_REQUEST_TEMPLATE.md`
- `docs/AgenticSE.md`
- `docs/DeveloperGuide.md`
- this dated log

## Verification

- Baseline `.\gradlew.bat clean test --no-daemon` passed after using the
  repository's ignored `.gradle/agent-home` cache. The default sandbox cache
  location was not writable and the first cache placement under `build/`
  conflicted with Gradle's `clean`; neither issue changed tracked files.
- The skill format validator could not start because the available Windows
  Python launcher was inaccessible in this environment. This is a tooling
  limitation, not a reported skill-validation pass.
- No commit, push, pull, branch switch or GitHub API mutation was performed.

## Follow-up

Before a future meaningful borrower PR, run the completeness skill, arrange the
fresh reviewer, retain decision-relevant evidence and complete the PR template.
If automatic GitHub comments are desired later, configure an approved
GitHub-native or third-party review service separately with repository-owner
authorization.
