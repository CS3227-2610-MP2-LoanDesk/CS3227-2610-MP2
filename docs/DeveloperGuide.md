# LoanDesk Developer Guide

This guide explains the current architecture, local development workflow,
borrower implementation boundaries and extension rules. LoanDesk is a JavaFX
desktop application backed by an embedded H2 database.

## Prerequisites and local setup

- JDK 25.
- Internet access for the first Gradle dependency download.
- Git and a shell capable of running the Gradle wrapper.

Clone the repository, change into its root, and use the wrapper rather than a
machine-installed Gradle version:

```powershell
.\gradlew.bat clean test --no-daemon
.\gradlew.bat run
```

On macOS/Linux, use `./gradlew` instead of `gradlew.bat`.

The JavaFX plugin supplies JavaFX 21.0.6 modules and the project targets Java
25. H2 2.5.250 is an application dependency. No external database server is
required.

## Architecture

The intended dependency direction is:

```text
JavaFX UI
    |
    v
Application services and session authorization
    |
    v
Shared domain records and workflow rules
    |
    v
DataStore interface -> DatabaseDataStore -> embedded H2 database
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
src/main/java/loandesk/
  LoanDeskApp.java
  application/       use cases, session and shared rules
  domain/            records, roles and status enums
  persistence/       H2-backed DataStore implementation
  security/          password hashing
  features/          role-owned extension boundaries
src/main/resources/  loandesk.css
src/test/java/       JUnit unit and integration-style tests
docs/                 guides, context, milestones and reflections
logs/Yikbing-logs/   dated development evidence
.agents/skills/       borrower review skills
tools/borrower/       personal hooks and skill-evaluation fixtures
```

## Shared data model and persistence

`DatabaseDataStore` creates or upgrades these H2 tables:

| Table | Main contents |
| --- | --- |
| `users` | username, normalized username key and role |
| `credentials` | password algorithm, iterations, salt and derived hash |
| `equipment` | equipment ID, name and condition |
| `loan_requests` | borrower request, dates, status, decisions and cancellation metadata |
| `loans` | checkout, due and return dates, borrower, equipment and status |
| `database_state` | revision used for stale-snapshot detection |

The database path is `data/loandesk`, resolved from the process working
directory. The first empty database is seeded once; later launches preserve
existing records. JSON is no longer the production persistence mechanism and
there is no JSON importer.

Saves are transactional. Before writing, the store checks the loaded revision;
an older snapshot cannot silently overwrite a newer one. It validates foreign
keys, request/loan relationships, unique IDs and collected-request links. A
failed save rolls back the transaction and leaves the previous database state
intact.

Do not place passwords, real borrower data or `data/loandesk` in Git. Tests use
temporary directories and synthetic records.

## Domain and workflow rules

### Authentication and sessions

`AuthenticationService` handles borrower login and sign-up. Usernames are
normalized and validated by `UsernamePolicy`. Passwords must be 8–128
characters and are hashed with salted `PBKDF2WithHmacSHA256` using 600,000
iterations. `Session` is the source of the authenticated identity; services
must not trust a caller-supplied borrower ID as an authorization substitute.

`requireUser()` confirms that a session exists. `requireRole(...)` builds on
that check and additionally enforces the required role. Borrower operations
must call the role-specific guard before reading or mutating borrower data.

### Equipment and availability

Equipment condition is one of `GOOD`, `DAMAGED`, `UNDER_MAINTENANCE` or `LOST`.
Availability is derived by `AvailabilityService`:

```text
physical condition not GOOD or lost loan -> UNAVAILABLE
active loan                         -> ON_LOAN
future approved request             -> RESERVED
otherwise                           -> AVAILABLE
```

Pending requests do not reserve equipment. Availability is recalculated at
the service operation, not trusted from an earlier UI display.

### Requests and loans

Requests and loans are separate records. A borrower creates a request; a
supervisor decides it; a custodian creates the loan during physical checkout.
A collected request links to exactly one loan. Rejected, cancelled and expired
requests do not create loans.

The current request vocabulary is:

```text
PENDING, APPROVED, COLLECTED, REJECTED, CANCELLED, EXPIRED
```

The current loan vocabulary is:

```text
ACTIVE, RETURNED, LOST
```

Borrower services currently implement submission, own-request listing,
eligible editing and eligible cancellation. Supervisor approval and custodian
checkout/return/condition mutations are shared integration work owned by the
other role members.

## Borrower service boundaries

- `CatalogueService` permits catalogue loading only for an active borrower and
  performs case-insensitive name filtering.
- `BorrowerEligibilityService` checks overdue/lost loans and the limit of three
  active loans or approved reservations.
- `BorrowerRequestService` validates equipment, purpose, dates, availability,
  duplicate pending requests, ownership, request state and save outcomes.
- `BorrowerLoanService` lists only loans belonging to the active borrower and
  places active/lost loans before returned history.
- `AvailabilityService` centralizes derived availability rules.

Every service operation should be safe when called directly, bypassing the UI:
logged-out access, wrong-role access, unknown IDs, foreign-owned records,
stale states and failed saves must be rejected without unauthorized mutation.

## Design decisions and trade-offs

### H2 instead of JSON

JSON was simple for the first prototype, but H2 provides structured tables,
constraints, transactions and a better path for concurrent/shared updates while
remaining local and dependency-contained for a standalone laptop. The trade-off
is more schema and migration code. The current store uses additive schema
initialization and a revision row rather than a full migration framework.

### Separate requests and loans

A request represents an approval decision and may never become a physical loan.
A loan represents actual checkout and return activity. Keeping them separate
prevents rejected or uncollected requests from appearing as borrowed equipment
and allows the borrower history to distinguish planned borrowing from physical
possession.

### Derived availability

Availability is derived from condition, approved reservations and loans rather
than stored as a manually edited duplicate. This avoids contradictory states,
but means every approving or checkout operation must recalculate availability
against fresh data.

### Service-layer authorization

The UI hides unavailable buttons for usability, but services enforce the rules
because direct calls, stale screens and future role integrations can bypass UI
controls. The trade-off is more explicit service checks and focused tests, in
exchange for stronger ownership guarantees.

### Manual JavaFX verification

The project has JUnit coverage for services, domain rules, persistence and
application-level integration, but no automated JavaFX interaction harness.
GUI layout and navigation are therefore verified manually with documented
scenarios. This is a known testing limitation, not evidence that the UI has
been exhaustively tested.

## Testing workflow

Run the main suite on Windows with:

```powershell
.\gradlew.bat clean test --no-daemon
```

The test suite covers authentication, password boundaries and hash-only
storage, session roles, catalogue filtering and authorization, availability,
eligibility, request submission/edit/cancellation, loan listing and ordering,
domain validation, H2 persistence, failed saves, restart reloads and stale
snapshot/concurrency checks. `BorrowerLifecycleIntegrationTest` covers
synthetic persisted lifecycle states without claiming that supervisor or
custodian UI exists.

Manual borrower GUI checks should cover:

1. role selection, borrower login and sign-up;
2. wrong credentials and invalid sign-up input;
3. catalogue filtering, uppercase/partial input, clear and no matches;
4. request validation, success and duplicate/eligibility rejection;
5. request editing, locked equipment identity, invalid input and confirmation;
6. future cancellation, reason selection and terminal-state restrictions;
7. dashboard, scrolling, empty states, responsive sizing and logout;
8. My Loans active/history layout using synthetic shared records when available.

Skill contract checks and controlled evaluation cases are described in
`tools/borrower/skill-evaluations/README.md`. Run the repository-side check
with Python 3:

```powershell
python tools/borrower/test_skill_contracts.py
```

This validates the skill definitions and fixtures; it cannot prove Codex's
external automatic-selection model. The existing ownership fixture has a
separate acceptance command:

```powershell
.\gradlew.bat -p tools/borrower/skill-evaluations/ownership verifyFixtures --no-daemon
```

Its case A intentional failure is expected; the acceptance task verifies that
case A has exactly one expected failure and case B passes all seven tests.

## Development and review workflow

Use the project workflow:

```text
Plan -> implement -> focused tests -> review -> fix -> full test -> GUI verify
-> document -> commit -> push -> PR
```

Before a meaningful borrower feature, use the implementation-preflight skill.
Before a milestone or PR readiness decision, use the relevant UI, ownership,
edge-case and completeness skills. Independent reviewers are separate evidence
and do not replace tests or human review.

The personal hooks are optional per checkout:

- pre-commit checks staged whitespace, conflict markers, local data and scope
  warnings;
- pre-push runs the full clean Gradle test suite.

Hooks do not launch agents or make network calls. GitHub Actions also runs the
Gradle test task for pushes to `main` and pull requests targeting `main`.

## Extending the project safely

When adding a borrower feature:

1. Read `docs/ProjectContext.md`, `docs/ProjectChecklist.md` and
   `docs/ReviewGapRegistry.md`.
2. Confirm whether the change affects a shared contract, role permission,
   availability rule or persistence schema. Coordinate before changing shared
   contracts.
3. Add service-layer authorization and state checks before wiring the UI.
4. Add success, rejection, boundary, failed-save and restart tests as
   applicable.
5. Add manual GUI evidence when JavaFX screens change.
6. Update both guides and a dated session log when behaviour or design changes.
7. Run the full clean suite and complete the relevant review before committing.

Do not place domain or persistence logic in `LoanDeskApp`, duplicate H2 stores
inside role folders, trust UI-only authorization, or use production data in
fixtures.

## Packaging and release status

The Gradle `application` plugin is configured with
`loandesk.LoanDeskApp` as the main class. A final release still needs a verified
distribution containing JavaFX runtime modules and dependencies, followed by
clean-machine checks on the supported operating systems. A plain JAR is not
currently a verified cross-platform release artifact. Do not claim Windows,
macOS or Linux compatibility until the packaged distribution has been tested
on those systems.

## AI-assisted development records

Meaningful AI-assisted sessions are summarized under
`logs/Yikbing-logs/`. Records should contain observed commands, results,
decisions, changed files, reviewer evidence and limitations. `docs/AgenticSE.md`
describes the five borrower skills, controlled evaluations and personal hooks.

## Open shared-workflow boundary

The borrower implementation depends on supervisor and custodian members
completing the shared integration points:

- supervisors review pending requests and approve/reject them;
- custodians check out approved equipment and create loans;
- custodians record returns, condition and maintenance state;
- all roles use the same H2 request, loan and equipment contracts.

Fine settlement, supervisor overrides, clarification workflows and a complete
cross-role acceptance journey remain outside the completed borrower-only scope.
