# Supervisor review-context equipment-name check

## Scope

The supervisor review detail screen now displays an outstanding loan's equipment
name rather than its identifier. This required a read-only
`SupervisorRequestService.equipmentNameOf` operation.

## Permission and workflow review

| Operation | Permission | Source status | Target status |
| --- | --- | --- | --- |
| Read equipment name for review context | `REVIEW_REQUESTS` | Any request being reviewed | No state change |

The service requires `REVIEW_REQUESTS` before loading the equipment name. It
does not write to the datastore or alter request, loan, availability, or
decision metadata. Existing approve, reject, and cancellation entry points and
their lifecycle checks are unchanged.

## Evidence

`SupervisorRequestServiceTest` now verifies a supervisor resolves `camera1` to
`Camera 1`, while borrower, custodian, and signed-out sessions are refused by
the new service entry point. The focused supervisor test suite was run after
the change.

## Limitation

This is a focused read-path check, not a complete lifecycle or UI interaction
test. JavaFX visual layout was not exercised programmatically.
