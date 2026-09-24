# LoanDesk Project Context

This file is the compact context reference for future development sessions.
Update it when a team decision changes the architecture, workflow, or scope.

## Product

LoanDesk is a JavaFX desktop equipment-lending application for one local
installation. It has three roles: borrower, lab supervisor/approver, and
equipment custodian/lab technician. There is no hosted backend or network
synchronization in the current plan.

## Technology baseline

- Java SE 25
- JavaFX
- Gradle 9.1.0 via the committed Gradle wrapper
- JUnit 5
- Source under `src/main/java`; tests under `src/test/java`

## Intended architecture

```text
JavaFX views/controllers -> application services -> repositories/local persistence
```

Views collect input and display results. Services enforce permissions,
ownership, validation, and state transitions. Repositories own local data access.
Controllers should not contain direct file or database access.

### Package responsibilities

`domain/` contains the core application data and vocabulary, independent of
JavaFX and persistence technology. Examples include `User`, `Equipment`, `LoanRequest`, `Role`,
and request or equipment status types.

`application/` contains use cases and business rules between the UI and domain.
Examples include authentication, session management, permissions, request
submission, approval, checkout, and return services. This layer must enforce
rules even when a method is called without using the UI.

`persistence/` contains the embedded H2 database store and repository
implementations. It converts database rows to domain objects and writes domain
changes through transactions. The shared store has role-aware user records,
credential records, equipment records and future request/loan tables. Views and
services use persistence interfaces rather than issuing SQL directly.

The intended dependency direction is:

```text
ui -> application -> domain
				  -> persistence
```

Role-specific UI belongs in `features/borrower/ui`, `features/supervisor/ui`, and
`features/custodian/ui`. Shared login, role selection, and session/logout UI belongs in
`ui/common`. Shared packages need coordination because all three roles depend
on them.

```text
Borrower submits -> Supervisor approves -> Custodian checks out
-> Custodian returns -> Borrower sees history after restart
```

### Initial borrower catalogue contract

The first catalogue slice is intentionally read-only and supports equipment
identifier/name display, case-insensitive name filtering, clearing the filter,
and an understandable empty-results message. It does not add category,
condition, availability or loan fields yet. Those shared fields will be added
with the request/loan foundation so availability can be derived from real
workflow records rather than invented catalogue state.

The agreed future vocabulary is equipment conditions `GOOD`, `DAMAGED`,
`UNDER_MAINTENANCE` and `LOST`; request statuses `PENDING`, `APPROVED`,
`REJECTED`, `NEEDS_CLARIFICATION` and `CANCELLED`; and loan statuses `ACTIVE`,
`RETURNED`, `OVERDUE` and `LOST`. Availability will be derived as
`AVAILABLE`, `ON_LOAN` or `UNAVAILABLE`.

Important invariants include: only approved requests can be checked out; an item
cannot be issued twice; damaged equipment becomes unavailable; borrowers can
only access their own records; and failed multi-record saves must not leave
inconsistent state.

## Current scope boundaries

Initial scope excludes payments, fines, email, QR scanning, multi-item baskets,
parts inventory, repair costing, and detailed maintenance scheduling.

## Login direction

The first screen presents three buttons labelled `Borrower`, `Supervisor`, and
`Custodian`. The final role flows require passwords for borrowers, supervisors
and custodians. The current foundation still opens singleton supervisor and
custodian accounts directly; their role owners must replace that temporary
entry with password login. The borrower path presents separate `Log in` and
`Sign up` actions and uses a unique username with a password. Usernames are trimmed,
compared case-insensitively, cannot be blank or invalid, and have a maximum
length of 30 characters. Internal spaces are not allowed; letters, numbers,
underscores, and hyphens are allowed. Borrower passwords must be 8 to 128
characters. Passwords are stored as salted PBKDF2-HMAC-SHA256 hashes with
recorded parameters, never as plaintext.

Authentication is the gate for protected role work. An unauthenticated user
may choose a role and use the borrower log-in or sign-up flow, but must have an
active session before opening a role dashboard or performing role operations.
Logout clears the active session and returns to role selection. Services must
enforce this boundary as well as the UI, so direct calls cannot bypass it.

The local database is an embedded H2 store rooted at `data/loandesk`; H2
creates its database files in that ignored directory. On first launch, if the
database is empty, the persistence layer creates the schema and seeds the
initial borrower accounts `testBorrower1` and `testBorrower2`, plus equipment
records `camera1` and `camera2`. If the database already contains records, it
is loaded unchanged and is never reseeded automatically. New borrower sign-ups
are written transactionally. Each loaded store tracks a database revision, and
an atomic revision update prevents stale or concurrent snapshots from
overwriting a newer shared update. The same shared database is available to
supervisor and custodian features through the common persistence package.

The current migration policy is to start a fresh H2 database rather than read
old JSON data. Existing ignored JSON files are not modified or imported; create
new local accounts through sign-up. Future schema changes must use an explicit
schema-version or migration policy before they are introduced.

## Proposed ownership layout

Role-specific screens and feature code should be separated into borrower,
supervisor, and custodian feature areas. A role owner may add both UI and
role-specific application services in their feature area. Shared login screens
belong in a common UI package, while domain models, permissions, availability,
and persistence remain shared because cross-role rules must have one source of
truth.

```text
src/main/java/loandesk/
	ui/common/             login, role selection, session/logout components
	features/borrower/    borrower UI and role-specific application logic
	features/supervisor/  supervisor UI and role-specific application logic
	features/custodian/   custodian UI and role-specific application logic
	domain/                shared models, roles, and statuses
	application/           shared services and permission boundaries
	persistence/           shared H2 database store and local data initialization
```

Inside each role feature area, use `ui/` for views/controllers and
`application/` for role-specific use cases. Each role owner should primarily
work in their feature area. Changes to shared packages require coordination
because they affect all three roles. Do not duplicate shared models or storage
just to avoid coordination.

## Decisions still requiring team agreement

- H2 persistence rooted at `data/loandesk`
- Seed only when the database is empty; do not reseed existing data
- Schema versioning and database recovery policy
- Keep supervisor and custodian as fixed singleton roles without usernames
- Inclusive or exclusive date boundaries
- Request and loan model boundaries
- Cancellation and clarification rules for each request state
- Eligibility treatment for borrowers with overdue loans
- Reset behaviour for demonstration data
- Packaging and cross-platform release verification

## Session continuity

For a new chat/model, read `CODEX_HANDOFF.md` first. Yikbing prefers a guided
explanation and agreement on the next scope before new implementation. We paused
after a first isolated ownership skill evaluation: executable JUnit checks and
a separate fresh agent review, not an automated agent-invoking JUnit test.
The fixture policy is not a production cancellation-policy decision.

Yikbing owns borrower work; other team members own supervisor and custodian.
The agreed development setup (21 September 2026) includes three automatically
selectable borrower review skills under `.agents/skills/` and personal Git
hooks under `tools/borrower/hooks/`. See `docs/AgenticSE.md` for their scope and
`docs/DeveloperGuide.md` for local activation. Skills cover UI, ownership and
both existing test failures and additional edge cases. This setup changes no
product contracts and does not settle the open workflow policies above.

Before changing shared contracts, read this file and the relevant developer
logs. Record material decisions in the developer guide or a dated log entry.
