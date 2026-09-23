# 2026-09-21 independent-review trigger policy

## Decision

The borrower completeness skill now uses a two-level review policy:

- A borrower feature or milestone completion check, a request to move to the
  next milestone, and meaningful PR readiness checks receive the full review,
  including one fresh, read-only independent reviewer.
- Routine implementation substeps, small documentation edits and ordinary
  test runs use only the applicable focused checks unless the user explicitly
  asks for a review.
- A PR review is repeated when meaningful changes were made after the last
  completion check.

This keeps an independent second opinion at meaningful decision points without
making every small edit wait for a separate reviewer. The reviewer remains
outside local Git hooks and does not edit, commit, push or change local data.

## Files changed

- `.agents/skills/loandesk-borrower-change-completeness-review/SKILL.md`
- `docs/AgenticSE.md`
- `docs/DeveloperGuide.md`
- `CODEX_HANDOFF.md`
- this dated log

## Verification

- Pre-change `.\gradlew.bat clean test --no-daemon`: passed.
- Post-change manual skill trigger/frontmatter checks and `git diff --check`:
  passed. The official validator remains unavailable because the environment's
  Python launcher cannot start.
- Post-change `.\gradlew.bat clean test --no-daemon`: passed.
- No commit, push, pull or branch switch was performed.
