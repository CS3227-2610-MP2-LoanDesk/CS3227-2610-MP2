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
JavaFX and JSON. Examples include `User`, `Equipment`, `LoanRequest`, `Role`,
and request or equipment status types.

`application/` contains use cases and business rules between the UI and domain.
Examples include authentication, session management, permissions, request
submission, approval, checkout, and return services. This layer must enforce
rules even when a method is called without using the UI.

`persistence/` contains local JSON storage and repository implementations. It
converts JSON data to domain objects and writes domain data back to disk. Views
and services use repository interfaces rather than reading or writing files
directly.

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

Existing local files created before password support may contain users without
credentials. They are not silently assigned passwords or overwritten; a
fresh/reset local demonstration file or an explicit migration policy is
required before those accounts can log in.

The local file is `data/loandesk.json`. On first launch, if this file does not
exist, the persistence layer creates it and writes the initial borrower accounts
`testBorrower1` and `testBorrower2`, plus equipment records `camera1` and
`camera2`. The application can access these records by loading the JSON file
through its repositories after initialization. If the file already exists, it
is loaded unchanged and is never reseeded automatically. New borrower sign-ups
remain possible after seeding. Complex request, damage, and maintenance
scenarios will be added by role-feature developers after branching.

If the JSON file is malformed, the application must show an understandable
error and must not overwrite the original file. Normal saves should serialize
the complete new state to a temporary file in the same data directory, verify
that the write completed, and then replace the original file. A failed save
leaves the previous valid file available for recovery. Saves should happen
after successful operations, rather than only when the application exits, so a
crash does not discard an entire session. The temporary file is discarded after
failure and should not be committed to Git.

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
	persistence/           JSON repositories and local data initialization
```

Inside each role feature area, use `ui/` for views/controllers and
`application/` for role-specific use cases. Each role owner should primarily
work in their feature area. Changes to shared packages require coordination
because they affect all three roles. Do not duplicate shared models or storage
just to avoid coordination.

## Decisions still requiring team agreement

- JSON persistence at `data/loandesk.json`
- Seed only when the data file is absent; do not reseed existing data
- Preserve the original file if parsing or replacement fails
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
