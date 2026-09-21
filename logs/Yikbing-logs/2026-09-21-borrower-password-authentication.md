# AI Session Summary: Borrower Password Authentication

- **Date:** 21 September 2026
- **Purpose:** Replace username-only borrower authentication with password
  login/sign-up after professor feedback.
- **Authorization:** User approved dependency-free password hashing, the
  8-to-128-character password rule, and two synthetic seed credentials.

## Changes

- Added `PasswordCredential` as the persisted credential record.
- Added dependency-free PBKDF2-HMAC-SHA256 hashing with random salts,
  recorded parameters and constant-time comparison under `loandesk.security`.
- Extended `LoanDeskData` and JSON seeding/persistence for credentials.
- Updated borrower login/sign-up to accept a `PasswordField` and clear the
  password input after each attempt.
- Added authentication tests for successful login, wrong/null passwords,
  length validation, restart persistence and plaintext absence.
- Updated `ProjectContext.md`, `UserGuide.md` and `ProjectChecklist.md`.
- Left supervisor/custodian UI and direct singleton entry unchanged; their
  password work remains with their role owners.

## Seed and data limitation

The two seed values are synthetic demonstration credentials only. They are
hashed before being written to JSON and must not be reused elsewhere.
Pre-password local JSON files are not silently migrated or overwritten because
there is no safe password to infer; an explicit migration/reset policy remains
open.

## Verification

- `git diff --check` passed; Git reported only normal LF/CRLF conversion
  warnings.
- `.\gradlew.bat clean test --no-daemon` passed with Gradle 9.1.0.
