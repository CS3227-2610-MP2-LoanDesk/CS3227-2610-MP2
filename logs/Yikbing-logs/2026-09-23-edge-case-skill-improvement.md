# 2026-09-23 Edge-case skill improvement

## Reason

The borrower password/persistence work exposed a process gap: the edge-case
skill already mentioned failed saves, but its routing description did not name
authentication, password hashing, sign-up/login or credential persistence. The
review was therefore not reliably selected before an independent reviewer found
the failed-save in-memory-state defect.

## Changes

Updated `.agents/skills/loandesk-borrower-edge-case-test-review/SKILL.md` to:

- Explicitly route borrower authentication, password, credential persistence,
  database-save and catalogue changes to the skill.
- State that it is discoverable but not a background watcher or Git hook.
- Add an authentication checklist for wrong/null passwords, boundaries,
  duplicates, hash-only storage, failed saves and restart behavior.
- Add persistence checks for rollback, stale snapshots and concurrent writers.
- Require every triggered review to report `RUN`, `UNAVAILABLE` or
  `NOT APPLICABLE`, never silently treating a skipped review as passed.

## Verification

- The bundled `quick_validate.py` could not run because no runnable
  `python.exe` was available in the environment.
- PowerShell fallback checks confirmed the required frontmatter, routing text,
  password checklist, non-watcher guidance and explicit result statuses.
- No application code, local database or Git history was modified.
