# Borrower skill contract evaluation — 27 September 2026

## Scope

Added repository-side coverage for all five borrower skills:

- `loandesk-borrower-ui-review`
- `loandesk-borrower-ownership-review`
- `loandesk-borrower-edge-case-test-review`
- `loandesk-borrower-change-completeness-review`
- `loandesk-borrower-implementation-preflight`

The coverage validates skill frontmatter, trigger text, output expectations,
the complete manifest and one controlled synthetic evaluation case per skill.
The cases are evaluation fixtures, not LoanDesk production policy.

## Changes

- Added `tools/borrower/skill-evaluations/skill-evaluation-manifest.json`.
- Added controlled UI, edge-case, completeness and implementation-preflight
  cases; the existing ownership case remains the executable fixture.
- Added `tools/borrower/test_skill_contracts.py`.
- Updated the evaluation README and agentic documentation with the boundary
  between repository tests and external automatic skill selection.

## Verification

- `python tools/borrower/test_skill_contracts.py`: passed 4 tests.
- `python tools/borrower/test_hooks.py`: passed 9 tests under normal local
  permissions. The restricted sandbox initially reported Git dubious-ownership
  errors for its disposable repositories.
- `./gradlew.bat clean test --no-daemon`: passed; build successful.
- `./gradlew.bat -p tools/borrower/skill-evaluations/ownership verifyFixtures
  --no-daemon`: passed. Case A reported exactly one expected failure and case B
  passed all seven tests.

## Limitation

These repository tests cannot invoke or observe Codex's external automatic
selection model. A fresh-agent run with a realistic prompt is still required
to evaluate actual selection and reviewer behaviour for the four controlled
cases that do not yet have retained agent responses. This limitation is kept
explicit rather than treating contract-test success as proof of invocation.
