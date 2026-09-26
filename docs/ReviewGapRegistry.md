# Borrower Review Gap Registry

This registry records valid gaps found by independent borrower reviewers and
the process changes made to prevent repeating them. It is a development
process aid, not production policy. A `Resolved` entry means the specific gap
was addressed for the recorded scope; it does not claim exhaustive coverage.

## Resolved gaps

| ID | First observed | Area | Gap | Prevention/evidence | Status |
| --- | --- | --- | --- | --- | --- |
| RG-001 | 2026-09-21 | Ownership | A borrower operation could target another borrower's record when the service trusted the requested owner. | Use session-derived identity, service-level owner filtering and direct foreign-owner tests. | Resolved |
| RG-002 | 2026-09-23 | Persistence | A failed credential save could leave a newly created account in memory. | Persist before publishing state and test controlled save failure plus retry/reload behavior. | Resolved |
| RG-003 | 2026-09-23 | Catalogue persistence | A restart test reloaded only seeded equipment and could pass without proving custom data persistence. | Restart tests must save synthetic non-seeded data, recreate the store and assert exact reload. | Resolved |
| RG-004 | 2026-09-23 | Catalogue UI | Empty-catalogue feedback could be misleading because it reused the no-match message. | Test both an empty catalogue and a non-empty catalogue with no matching filter. | Resolved |
| RG-005 | 2026-09-25 | Loan domain | A returned loan could contain a return date before checkout when constructed directly. | Enforce checkout-before-return in the domain record and test the invalid boundary. | Resolved |
| RG-006 | 2026-09-25 | Request/loan persistence | Multiple collected requests could reference one loan without reciprocal request linkage. | Validate reciprocal request/loan IDs and add duplicate-link persistence tests. | Resolved |
| RG-007 | 2026-09-25 | Cancellation persistence | A cancelled request could name a cancellation actor different from its borrower. | Validate cancellation ownership in persistence and test a foreign cancellation actor. | Resolved |
| RG-008 | 2026-09-25 | Request domain | A `PENDING` request could contain approval or rejection metadata. | Enforce state-specific metadata invariants and test pending decision metadata. | Resolved |
| RG-009 | 2026-09-25 | Documentation | Handoff/checklist wording became stale after follow-up fixes. | Reconcile milestone, checklist and handoff evidence before PR readiness. | Resolved |
| RG-010 | 2026-09-25 | My Requests UI | A read-only screen did not explain why edit/cancel actions were unavailable. | State deferred actions explicitly in the UI and keep the scope documented. | Resolved |
| RG-011 | 2026-09-25 | My Requests UI/tests | Request details could be clipped, and the list lacked a fresh-store reload assertion. | Use a scrollable details area and recreate H2 in a request-list persistence test. | Resolved |
| RG-012 | 2026-09-25 | Review process | Independent review was not consistently invoked before meaningful feature commits. | Completeness skill now requires a pre-commit panel and this registry is read before review. | Resolved |
| RG-013 | 2026-09-25 | Cancellation tests | Cancellation lacked direct proof of approved-reservation release, fresh reload metadata and role-boundary rejection. | Test availability after cancellation, recreate H2 and test logged-out/wrong-role calls. | Resolved |
| RG-014 | 2026-09-25 | Cancellation UI | A selected request could inherit a previous cancellation reason, and pending cancellation feedback incorrectly mentioned releasing a reservation. | Reset reason controls on selection and make success feedback status-aware. | Resolved |
| RG-015 | 2026-09-25 | Cancellation UI | Ineligible cancellation feedback did not distinguish terminal status from same-day/past-date policy. | Render specific status/date guidance and verify the action remains unavailable. | Resolved |
| RG-016 | 2026-09-26 | Loans ownership evidence | The initial active-loans test did not explicitly cover a custodian session, an empty borrower result, or the read-only save boundary. | Add wrong-role, empty-result and save-failure test-double coverage to borrower loan listing. | Resolved |
| RG-017 | 2026-09-26 | Loans UI error state | A loan-load failure could leave empty-list placeholders that looked like a successful account with no loans. | Use failure-specific placeholders and record manual load-error verification as a GUI limitation. | Resolved |
| RG-018 | 2026-09-26 | Synthetic lifecycle ownership evidence | The initial borrower-only lifecycle fixture did not include foreign records or all non-borrower session boundaries. | Add foreign request/loan filtering plus logged-out, supervisor and custodian rejection assertions, and confirm persisted state is unchanged. | Resolved |

## Deferred hardening or known limitations

These were raised as broader recommendations or environment limitations, not
blocking defects in the completed borrower slices:

- Schema-version tracking and database-level foreign keys for the nullable
  reciprocal request/loan link remain future hardening work.
- Purpose-built mid-write failure injection for every persistence path remains
  future hardening work; implemented save paths have rollback coverage.
- The project has no automated JavaFX interaction harness. GUI behavior is
  manually verified and recorded until a suitable test approach is agreed.
- Direct `findOwnRequest` logged-out/wrong-role tests and broader stale-screen
  scenarios can be expanded when request mutation actions are implemented.

## Maintenance rule

Before reviewing a meaningful borrower feature, read this registry and check
whether its gap patterns apply. After an independent panel, add every valid
new gap with its reproduction, prevention and focused verification. Do not add
unsupported reviewer suggestions as defects; record them as deferred or
inconclusive with their evidence.
