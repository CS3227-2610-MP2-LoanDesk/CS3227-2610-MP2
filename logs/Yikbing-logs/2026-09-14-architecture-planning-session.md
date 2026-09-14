# AI Session Summary: Architecture Planning

- **Date:** 14 September 2026
- **Participant:** Yikbing
- **Purpose:** Finalize local persistence, username rules, seed scope, and
  feature-folder ownership before implementing login.

## Decisions

- Use JSON persistence.
- Keep local application data in an ignored project-local `data/` folder.
- Do not clear local data before pushes or pull requests. CI should use a clean
  checkout and tests should use temporary data.
- Do not add an automatic reset hook. Reset behaviour remains a future explicit
  developer action.
- Borrower usernames are trimmed, case-insensitive, non-blank, maximum 30
  characters, and allow letters, numbers, underscores, and hyphens only. Internal
  spaces are not allowed.
- Seed only `testBorrower1`, `testBorrower2`, `camera1`, and `camera2` initially.
- Defer complex request, damage, and maintenance scenarios until after role
  developers branch and build their own features/tests.
- Do not build internal dashboard contents in the shared foundation.
- Prefer role-specific packages for borrower, supervisor, and custodian screens,
  with shared login UI in `ui/common` and shared domain/application/persistence
  packages kept separate because they must remain one source of truth.

## Planning outcome

The shared foundation should eventually contain role selection, borrower
login/sign-up, session/logout, JSON account persistence, first-launch seed
initialization, and empty role dashboards. Login implementation is still
intentionally postponed until Yikbing is satisfied with the design.

## Files updated

- `docs/ProjectContext.md`
- `docs/ProjectChecklist.md`
- This session summary

## Human verification

Yikbing should review this summary and confirm the username rules and package
ownership interpretation before implementation begins.
