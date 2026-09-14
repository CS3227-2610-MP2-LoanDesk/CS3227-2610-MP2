# AI Session Summary: Role Feature Folders

- **Date:** 14 September 2026
- **Participant:** Yikbing
- **Purpose:** Create clear starting locations for the three role owners.

## Decisions and changes

- Clarified that role ownership covers more than UI screens.
- Created feature areas under `src/main/java/loandesk/features/`:
  - `borrower/`
  - `supervisor/`
  - `custodian/`
- Each feature area contains tracked placeholders for:
  - `ui/`
  - `application/`
- Added a README in each role area explaining ownership and the boundary with
  shared domain, application, and persistence code.
- Updated `docs/ProjectContext.md` to describe the feature-area layout.
- Shared models, permissions, availability, and JSON persistence remain
  centralized to avoid duplicated cross-role rules.

## Verification

- `gradlew.bat clean test --no-daemon` passed.
- `git diff --check` passed.

## Human verification

Each team member should review the ownership README before branching and agree
which shared-package changes require discussion.
