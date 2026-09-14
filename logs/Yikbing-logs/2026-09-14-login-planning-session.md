# AI Session Summary: Login Planning

- **Date:** 14 September 2026
- **Participant:** Yikbing
- **Purpose:** Decide the local LoanDesk login flow and first-launch demo data.

## Decisions

- The first screen will show three short role buttons: `Borrower`,
  `Supervisor`, and `Custodian`.
- Selecting `Borrower` leads to separate `Log in` and `Sign up` actions.
- Borrowers use unique usernames only; no passwords are required for this local
  application.
- Supervisor and custodian each have one singleton role account and do not need
  a username or password.
- The role buttons are visible to everyone.
- Demo data will be seeded only when the application has no existing local data.
- Seeding should create coherent cross-role demonstration data and must not
  overwrite saved changes on later launches.
- New borrower sign-ups remain possible after the demo data is seeded.
- The agreed short labels are preferred over `Lab Supervisor` and
  `Equipment Custodian` on the first screen.

## Planning implications

The next planning gate is to define the shared login/session contract and the
exact seed data, while still postponing implementation. Remaining decisions
include storage technology, data location, reset behaviour, username validation
characters, and the exact seeded scenarios.

## Files updated

- `docs/ProjectContext.md`
- `docs/ProjectChecklist.md`
- This session summary

## Human verification

Yikbing should review this summary and confirm that the login and seeding
interpretation matches the intended design before implementation begins.
