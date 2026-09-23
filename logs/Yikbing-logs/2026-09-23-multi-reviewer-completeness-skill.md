# 2026-09-23 Multi-reviewer completeness skill

## Reason

Independent reviewers gave useful but different feedback during the H2 work,
while later catalogue reviewer tasks timed out. A single broad reviewer can
miss a concern outside its focus, and a timeout should not be mistaken for a
passing review.

## Changes

Updated `.agents/skills/loandesk-borrower-change-completeness-review/SKILL.md`
and `docs/AgenticSE.md` to use an adaptive independent review panel:

- Use at most three fresh read-only reviewers at milestone or PR checkpoints.
- Use UI/interaction plus behaviour/test reviewers for catalogue or screen work.
- Use authentication/security plus persistence/concurrency reviewers for
  password or database work, with a third completeness reviewer when needed.
- Use ownership/workflow plus behaviour/test reviewers for request/loan work,
  adding persistence review when storage changes.
- Keep prompts independent; do not share prior findings or other reviewer
  responses.
- Record each reviewer as `RUN`, `UNAVAILABLE` or `INCONCLUSIVE` and do not call
  a partial panel complete.
- Reconcile duplicate/disputed findings and map them to tests or decisions.

## Verification

- Skill frontmatter and body were inspected after editing.
- `git diff --cached --check` remains part of the final repository check.
- No application code, local database or Git history was modified by this
  skill change.
