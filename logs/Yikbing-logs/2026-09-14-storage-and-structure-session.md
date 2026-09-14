# AI Session Summary: Storage and Structure Decisions

- **Date:** 14 September 2026
- **Participant:** Yikbing
- **Purpose:** Confirm the shared package structure and local JSON storage
  behaviour before implementing the login foundation.

## Decisions

- Keep the agreed package structure for shared code and role-specific UI.
- Document `domain/` as shared models and vocabulary independent of JavaFX/JSON.
- Document `application/` as use cases and business rules that enforce
  permissions and transitions outside the UI.
- Document `persistence/` as the owner of JSON files and repository
  implementations.
- Use `data/loandesk.json` for local persistent data.
- If the file is absent on first launch, create it and seed `testBorrower1`,
  `testBorrower2`, `camera1`, and `camera2`.
- If the file exists, load it without reseeding or overwriting it.
- If JSON is malformed, show an error and preserve the original file.
- Save successful changes through a temporary file in the same directory, then
  replace the original only after the temporary write succeeds.
- Save after successful operations rather than waiting until application exit.
- Discard failed temporary files and keep local data ignored by Git.
- Keep supervisor and custodian as fixed singleton roles without usernames.

## Clarification recorded

The seed file does not need to exist beforehand. The persistence initializer
creates `data/loandesk.json` when it is missing, writes the initial data, and
then repositories load that data for the application to use.

## Files updated

- `docs/ProjectContext.md`
- `docs/ProjectChecklist.md`
- This session summary

## Human verification

Yikbing should review the save and seeding interpretation before implementation
of the persistence layer begins.
