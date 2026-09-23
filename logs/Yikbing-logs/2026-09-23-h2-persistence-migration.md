# 2026-09-23 H2 persistence migration

## Scope

Replaced the active JSON data store with a shared embedded H2 database for
users, password credentials and equipment. The borrower login and sign-up flow
remains unchanged. The shared store is role-aware so supervisor and custodian
records can use the same persistence boundary when their owners implement those
flows.

## Decisions

- Use H2 `2.5.250`, embedded in the Java application; no separate database
  server is required for users.
- Store the database under ignored `data/loandesk`.
- Create the schema and seed data only when the database is empty.
- Store password algorithm, iteration count, salt and derived hash; never store
  plaintext passwords.
- Use JDBC prepared statements and transactions for database writes.
- Track a database revision so stale snapshot writes are rejected instead of
  silently replacing a newer shared update.
- Do not import existing ignored JSON files. Start a fresh database and create
  local borrowers through sign-up.
- Keep supervisor/custodian password login out of this borrower-owned change;
  their future records can use the shared role-aware database.

## Files changed

- `build.gradle`
- `src/main/java/loandesk/persistence/DataStore.java`
- `src/main/java/loandesk/persistence/DatabaseDataStore.java`
- `src/main/java/loandesk/persistence/JsonDataStore.java` (removed)
- `src/main/java/loandesk/application/AuthenticationService.java`
- `src/main/java/loandesk/LoanDeskApp.java`
- `src/test/java/loandesk/AuthenticationServiceTest.java`
- `src/test/java/loandesk/DatabaseDataStoreTest.java`
- related context, checklist, guide and handoff documentation

## Verification

- Pre-change `.\gradlew.bat clean test --no-daemon`: passed.
- Post-change `git diff --check`: passed.
- Post-change `.\gradlew.bat clean test --no-daemon`: passed.
- Packaged-build check `.\gradlew.bat clean test installDist --no-daemon`: passed;
  the distribution contains `h2-2.5.250.jar`.
- Tests cover first-run H2 seeding, restart persistence, parent-directory
  creation, hash-only credential storage, case-insensitive duplicate rejection
  and one shared database containing all three role types. Follow-up tests also
  cover failed-save retry state, stale snapshot rejection, concurrent writers,
  and the load-before-save contract.

## Review follow-up

The independent review identified two issues after the first implementation:

- A failed save could leave a new borrower in the authentication service's
  in-memory snapshot. `AuthenticationService` now updates that snapshot only
  after a successful save, with a retry/login/restart regression test.
- Whole-snapshot saves could overwrite another instance's newer state. The
  H2 store now keeps a revision row and rejects stale saves; a two-instance
  regression test verifies that the newer state remains intact. A concurrent
  two-writer regression test verifies that only one writer can commit.

The follow-up `.\gradlew.bat clean test --no-daemon` passed after these fixes.
A final independent-review attempt was bounded and did not return after the
last refinement. The earlier independent findings were recorded above and
addressed; the final staged diff was then checked locally with the clean test,
packaged-build and pre-commit checks.
- No commit or push has been made for the H2 migration.

## Remaining before commit/PR

- Run the borrower completeness, edge-case and relevant UI reviews.
- Manually smoke-test the JavaFX sign-up/login flow against a fresh local H2
  database.
- Verify the packaged application and CI dependency resolution on supported
  platforms.
- Update any remaining release/architecture reflection material, then commit
  the complete H2 change as one focused commit.
