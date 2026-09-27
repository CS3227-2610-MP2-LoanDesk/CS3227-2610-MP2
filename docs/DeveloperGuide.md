# LoanDesk Developer Guide

## Baseline structure

- `src/main/java`: production Java source
- `src/test/java`: automated tests
- `docs`: user and developer documentation
- `docs/ProjectContext.md`: compact context and current decisions
- `docs/BorrowerMilestones.md`: sequential borrower implementation plan
- `docs/AgenticSE.md`: borrower skills/hooks and future team proposals
- `logs/Yikbing-logs`: Yikbing's dated, verified AI-session summaries
- `logs/Supervisor-logs`: dated, verified supervisor-lane AI-session summaries

The current shared Java package layout is:

- `domain`: models, roles, and statuses independent of JavaFX and persistence
- `application`: use cases, permissions, and workflow rules
- `persistence`: shared H2 database store, repositories and first-launch initialization
- `ui/common`: role selection, login, sign-up, session, and logout
- `features/borrower`, `features/supervisor`, `features/custodian`: role-owned
  `ui/` and `application/` packages as those role slices are added

Borrower-owned services currently include `AuthenticationService`,
`CatalogueService`, `BorrowerEligibilityService`, `BorrowerRequestService` and
`BorrowerLoanService`. Supervisor-owned code adds `PermissionService`,
`RequestLifecycleService`, `ReviewFilter` and `SupervisorRequestService`; the
first two are shared infrastructure that every role is expected to use. The JavaFX composition currently remains in
`loandesk.LoanDeskApp` while role owners continue migrating toward the feature
package layout.

The intended application direction is:

```text
JavaFX views/controllers -> application services -> repositories/local persistence
```

Views should collect input and display results. Services should enforce role,
ownership, and workflow rules. Persistence should remain behind repository
interfaces rather than being implemented directly in controllers.

## Permission matrix

`PermissionService` is the single place that decides which role may perform
which operation. Services name a `Permission` rather than checking a `Role`
inline:

```java
permissions.require(Permission.APPROVE_REQUEST);
```

`require` throws `IllegalStateException` when no user is signed in or when the
signed-in role does not hold the permission. `isGranted` answers the same
question without throwing, for enabling or hiding a control. Every permission
is held by exactly one role, which is asserted by a test.

| Permission | Borrower | Supervisor | Custodian |
| --- | --- | --- | --- |
| `BROWSE_CATALOGUE` | yes | | |
| `SUBMIT_REQUEST` | yes | | |
| `EDIT_OWN_REQUEST` | yes | | |
| `CANCEL_OWN_REQUEST` | yes | | |
| `VIEW_OWN_REQUESTS` | yes | | |
| `VIEW_OWN_LOANS` | yes | | |
| `REVIEW_REQUESTS` | | yes | |
| `APPROVE_REQUEST` | | yes | |
| `REJECT_REQUEST` | | yes | |
| `CANCEL_APPROVED_REQUEST` | | yes | |
| `VIEW_DECISION_HISTORY` | | yes | |
| `MANAGE_EQUIPMENT` | | | yes |
| `CHECK_OUT_LOAN` | | | yes |
| `RECORD_RETURN` | | | yes |
| `RECORD_MAINTENANCE` | | | yes |

The matrix decides role capability only. Record ownership, such as a borrower
reading only their own requests, stays in the owning service, which filters by
the session-derived username after the permission check passes. Custodian
permissions are declared ahead of the custodian implementation so that owner
adopts the same mechanism rather than adding role checks inline.

## Request lifecycle

`RequestLifecycleService` owns the legal transitions and the expiry rule. Role
services decide whether an actor may attempt a transition; the lifecycle
service decides whether the transition itself is allowed, through
`requireLegalTransition`.

```text
PENDING ---> APPROVED ---> COLLECTED
   |             |
   |             +--------> CANCELLED
   |             |
   |             +--------> EXPIRED
   +--------> REJECTED
   +--------> CANCELLED
```

`COLLECTED`, `REJECTED`, `CANCELLED` and `EXPIRED` are terminal. A rejected
request is replaced by a new request rather than reopened.

Decision metadata is stored on the request rather than in a separate history
table. `decisionBy`, `decisionAt` and `decisionReason` are set by approval and
rejection; `cancelledBy`, `cancelledAt` and `cancellationReason` are set by
either a borrower or a supervisor cancellation. A reason is required to reject
and to cancel, and optional to approve.

`EXPIRED` means an approved request was not collected by the end of its
requested start date. Expiry is applied lazily: `loadWithExpiredApprovals`
sweeps lapsed approvals whenever shared request data is read by the supervisor
queue or the borrower request list, and persists only when a status actually
changed. There is no background task and no startup-only sweep, so a shared
desktop left open across a date boundary still reports the correct status.

Availability precedence is unchanged and remains in `AvailabilityService`:
`UNAVAILABLE` for damaged, under-maintenance or lost equipment, then `ON_LOAN`,
then `RESERVED` for an approved uncollected request, then `AVAILABLE`. A
pending request reserves nothing. Note that an approved reservation currently
blocks its item outright rather than for a date range, so two non-overlapping
future bookings for the same item cannot both be approved; this is a known
limitation of the shared contract rather than a supervisor-specific rule.

`SupervisorRequestService.approve` rechecks both availability and borrower
eligibility at decision time through the same shared calculations the borrower
services use, so a request that was submittable earlier is refused once the
item was taken or the borrower fell behind.

## Supervisor authentication

The supervisor is a fixed singleton account seeded on first launch and verified
by `AuthenticationService.loginSupervisor` against the shared credentials table
using the same PBKDF2 hasher as borrower login. Passwordless `loginStaff`
remains only for the custodian role and rejects the supervisor role. Because
the account is seeded rather than created on read, a database created before
supervisor login existed has no supervisor account and reports that the ignored
local data must be removed so the demonstration accounts are seeded again.

## Branching workflow

- `main` is the shared integration branch.
- Create focused branches such as `feature/borrower-workflow`.
- Run `./gradlew test` before opening a pull request.
- Pull requests require review from at least one teammate.
- Use the repository PR template to record test results, relevant borrower
  skill reviews, independent-review evidence, data safety and shared-contract
  coordination.
- Do not merge changes that break the build or change a shared contract without
  discussing it with affected owners.

The application uses the embedded H2 dependency declared in `build.gradle`.
Normal users do not install or run a separate database server; the Gradle
application distribution supplies the H2 JAR and the application creates its
ignored local database files under `data/loandesk`. The current schema stores
users, credentials, equipment, equipment condition, requests, loans and a
database revision row. A store must load the database before saving; snapshot
writes then use an atomic revision update inside the transaction, rejecting
stale or concurrent writers instead of silently replacing another instance's
newer shared update. Request and loan records are separate: a request is
created by a borrower, while a loan is created by the future custodian
checkout workflow. Borrower screens read these shared records but do not
duplicate them or mutate custodian state.

The initial borrower catalogue is implemented by `CatalogueService`. It loads
equipment through `DataStore` only for an active borrower session and owns
case-insensitive name filtering, while the JavaFX screen is responsible only
for collecting the filter and displaying the results. The filtering helper is
pure; the persistence boundary is protected by the role check. Category,
condition and availability are represented by shared equipment and request/loan
state; borrower-visible availability is derived from that shared state rather
than duplicated in the catalogue UI.

`BorrowerRequestService` enforces session ownership, request validation,
eligibility, availability, pending-request editing, cancellation state/date
rules and persistence. Its edit operation updates only the purpose and dates
of an eligible pending request while preserving the request and equipment IDs.
The borrower request screen displays persisted requests and invokes only
permitted borrower actions. `BorrowerLoanService` reads loans for the active borrower,
filters by session-derived username, and orders active/lost loans before
returned history. Loan overdue status is derived from the due date; checkout,
return and physical-condition mutations remain custodian responsibilities.

Run the complete verification suite on Windows with:

```text
.\\gradlew.bat clean test --no-daemon
```

JavaFX interaction is currently verified manually because the project does not
have an automated JavaFX interaction harness. The borrower review skills and
the independent review panel are run at meaningful feature or milestone
readiness points; their evidence is recorded in `logs/Yikbing-logs/`.

## AI-assisted development records

For each meaningful AI-assisted session, add a dated summary under
`logs/Yikbing-logs/`. Record only observed commands, results, decisions, and
files changed. These summaries are checked by a human before submission and
are not intended to replace the full conversation transcript.

## Borrower skills and personal hooks

Five skills live under `.agents/skills/`: `loandesk-borrower-ui-review`,
`loandesk-borrower-ownership-review`, and
`loandesk-borrower-edge-case-test-review`,
`loandesk-borrower-change-completeness-review` and
`loandesk-borrower-implementation-preflight`. Their descriptions enable automatic
selection for relevant borrower changes; this is agent selection, not background
execution. You can also invoke a skill by its `$name`. If newly created skills
are not visible, start a fresh session. Review-only requests produce findings;
fixes/tests are made only within an authorized implementation task.

For a meaningful borrower feature or milestone slice, before finalizing its
commit or PR, the completeness skill arranges a fresh, read-only reviewer pass
over the
relevant change diff. The reviewer must not receive the implementing agent's
conclusions and must report findings with severity, file/line references,
reasoning and next actions. Repeat it before a PR if meaningful changes were
made after the last completion check. Save decision-relevant prompt/output
evidence in a dated log. Routine substeps, small documentation edits, trivial
formatting changes and ordinary test runs do not automatically invoke a fresh
reviewer. If a valid finding is fixed, rerun affected tests and the full clean
suite; repeat the panel only for a material behavioral change or major finding.
This independent pass is not launched by Git hooks; hooks stay deterministic and
local. GitHub Actions runs the repository's build/test checks, while
GitHub-native review services are an optional additional PR-comment layer.

The completeness workflow also maintains `docs/ReviewGapRegistry.md`. Read it
before a meaningful borrower review and add valid reviewer findings with their
prevention and verification after reconciliation. Deferred recommendations and
environment limitations are recorded separately from resolved gaps.

Before implementing a meaningful borrower feature, invoke the implementation
preflight skill. It converts applicable registry gaps into a short prevention
checklist, identifies focused tests and GUI evidence, and flags unresolved
shared-policy questions. It is planning guidance, not a substitute for the
later implementation reviews or independent panel.

Git for Windows supplies Bash for `tools/borrower/hooks/pre-commit` and
`pre-push`. No Python dependency is needed to run the hooks. Pre-commit checks
staged content for local data, conflict markers and whitespace, and warns about
shared/other-role source changes. Pre-push runs the full clean test suite on
the current working tree; uncommitted changes mean this is not proof that the
commits being pushed pass independently. CI checks the submitted commit.

Activation is per clone. Inspect `git config --show-origin --get core.hooksPath`
and existing `.git/hooks` first; do not replace active hooks without coordination.
When no existing configuration needs preserving:

```powershell
git config --local core.hooksPath tools/borrower/hooks
git config --local --get core.hooksPath
```

To disable this setup, first confirm that the local value is still
`tools/borrower/hooks`, then run `git config --local --unset core.hooksPath`.
This restores default/inherited hook lookup; it does not delete hook scripts.
Local hooks are bypassable and do not replace CI. Marker-like text in docs can
be flagged; inspect and rephrase intentional examples rather than auto-editing.

Reproduce the hook checks with Python 3 (test tooling only):

```powershell
python tools/borrower/test_hooks.py
git hook run pre-commit
git hook run pre-push
```

Fixtures use synthetic files in disposable repositories under ignored `build/`.
Never run `clean` concurrently with these fixtures. No real commit or push is
needed. Skills, hooks and the harness do not modify application data. Unix
execution has not been verified; a future Unix checkout may also require
executable permissions on the two hook scripts.

## First ownership skill evaluation

Follow the [beginner walkthrough](../tools/borrower/skill-evaluations/ownership/README.md).
It contains neutral case A/B examples, synthetic requirements, seven shared
JUnit tests, a reviewer prompt and a separate evaluator answer sheet.

```powershell
.\gradlew.bat -p tools/borrower/skill-evaluations/ownership verifyFixtures --no-daemon
```

An intentional test failure in case A is expected; the harness checks its exact
identity and requires all case B tests to pass. Do not run only the fixture
`test` task and interpret its exit status as acceptance: `verifyFixtures` is the
acceptance task. The application build and normal pre-push tests exclude these
fixtures. See the dated log for the independent review and assessment; use a
fresh reviewer without the answer sheet when repeating the evaluation.

JUnit runs Java assertions only; it does not send the review prompt to an AI.
The agent review and assessment are separate steps. When switching chats/models,
start with `CODEX_HANDOFF.md` for the current stopping point and user preferences.

## Open workflow policies

The agreed request/loan policy is recorded in `ProjectContext.md`. It covers
separate request and loan records, supervisor approval, custodian
checkout/return, fourteen-day default due dates, expiry after a missed
collection, cancellation, overdue eligibility and derived availability. Shared
request and loan contracts should follow those decisions rather than inventing
role-specific alternatives.

The borrower request UX is also recorded in `ProjectContext.md`. Its important
implementation boundaries are that the UI may explain or disable actions, but
the application service must remain authoritative for eligibility, duplicate
pending requests, ownership, date rules and fresh availability checks. The
dashboard may summarize active loans, overdue warnings, approved collection
reminders and request history, while shared persistence remains responsible for
the underlying records. Supervisor reasons and custodian condition data are
shared inputs; they are not reimplemented as borrower-only state.

The first request implementation slice adds shared request/loan vocabulary,
additive H2 persistence and read-only eligibility/availability queries. It does
not add supervisor approval or custodian checkout/return screens. A checked-out
request is `COLLECTED` and links to a separate loan; `OVERDUE` is derived from
an active loan and its due date. Existing local H2 data must remain readable
after the additive schema change.

See [ProjectContext.md](ProjectContext.md) for the current context snapshot and
[AgenticSE.md](AgenticSE.md) for the skill and hook proposal.
