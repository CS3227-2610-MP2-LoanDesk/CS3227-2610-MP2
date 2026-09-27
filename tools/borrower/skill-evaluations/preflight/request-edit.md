# Implementation-preflight fixture

Use this controlled fixture with `loandesk-borrower-implementation-preflight`.

## Request

Add borrower editing for pending requests. The request owner is held in the
active session, the repository persists to H2, and the UI will reuse the
submission form. The project already has a review-gap registry containing
foreign-owner, failed-save and stale-state gaps.

## Expected preflight behaviour

Before coding, the response should define the feature boundary, read the
project policy and review-gap registry, identify service-layer owner and state
checks, list success/rejection/boundary/failed-save/restart tests, identify
manual JavaFX evidence, flag shared-contract coordination, and list required
guide and session-log updates.
