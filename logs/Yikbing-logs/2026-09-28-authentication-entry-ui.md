# Authentication entry UI update — 28 September 2026

## Scope

Replaced the intermediate borrower action menu with direct authentication forms
for Borrower, Supervisor and Custodian. This is a presentation-only change;
authentication remains delegated to `AuthenticationService`.

## Borrower UI review

- `showBorrowerOptions` now opens the borrower login form directly.
- The borrower sign-in form shows `Selected role: Borrower`, username and
  password inputs, a red `Log in` button, `Sign up instead`, and `Return to
  role selection`.
- Failed borrower authentication keeps the username in place, clears the
  password, and displays the service error inline. Successful authentication
  still opens the dashboard through the existing service call.
- The sign-up view reuses the form and offers `Log in instead`; the existing
  username and password validation remains service-owned.
- Supervisor and Custodian use the same presentation pattern but retain only a
  password input and their existing role-specific service calls.

## Verification and limits

- Code inspection confirmed no UI-only authorization was introduced; all
  authentication calls remain in `AuthenticationService`.
- `./gradlew --quiet classes` and `./gradlew --quiet test` were run after the
  change.
- There is no JavaFX interaction harness, so visual interaction was not claimed
  as executed evidence.
