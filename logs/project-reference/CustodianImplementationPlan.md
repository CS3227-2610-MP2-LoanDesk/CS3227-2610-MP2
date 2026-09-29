# LoanDesk Custodian Implementation Plan

## Purpose and authority

This is the execution plan for the LoanDesk equipment-custodian feature. It is
intended to let the team delegate one bounded stage at a time without changing
the agreed shared workflow by accident.

The agreed Pages data contract is the source of product policy for this plan.
The early workload PDF is historical context only where it conflicts with that
contract. Existing source code and current shared documentation describe the
starting implementation, not a reason to override these agreed decisions.

This plan is itself a planning artifact. Updating the User Guide, Developer
Guide, Project Context, Project Checklist, and Agentic SE documentation is
deliberately the final stage.

## Agreed custodian contract

| Area | Decision |
| --- | --- |
| Custodian account | A fixed, seeded singleton account with password login, mirroring the supervisor MVP account. |
| Collection window | A request is collectable from its requested start date through three additional calendar days, inclusive. |
| Expiry | An uncollected approved request expires when the current date is after `startDate + 3 days`. |
| Checkout checks | The custodian checks the collection window, `APPROVED` status, and current equipment availability. Custodian checkout does not re-check borrower eligibility. |
| Checkout result | The request becomes `COLLECTED`; a separate linked `ACTIVE` loan is created atomically. |
| Good return | The loan becomes `RETURNED`, the item condition is `GOOD`, and it can become available. |
| Damaged or maintenance return | The loan becomes `RETURNED`; the item becomes `DAMAGED` or `UNDER_MAINTENANCE` and unavailable. |
| Lost item | Only a custodian may mark an active loan/item `LOST`. |
| Recovered lost item | The custodian restores the loan to `ACTIVE` and the equipment to `GOOD`, then records a separate return with the observed condition. The earlier lost state is intentionally not retained. |
| Maintenance scope | Condition-only. No maintenance record, issue/resolution text, or notes are part of the MVP. |
| Persistence | Use the current transactional full-snapshot save and stale-revision protection for this MVP. A save conflict must be reported as a refresh/retry action. |
| Deferred inventory fields | Categories, descriptions, retirement, deletion, notes, and bulk import are out of scope. |

### Collection-window examples

For a request with a start date of 1 October:

- Checkout is allowed on 1, 2, 3, and 4 October.
- Checkout is rejected on 30 September and on 5 October.
- On 5 October, the request is `EXPIRED` when the relevant shared data is
  loaded and its reservation is released.

## Non-negotiable engineering rules

- Keep JavaFX screens thin. Permissions, lifecycle checks, availability checks,
  and persistence mutations belong in application/domain/persistence code.
- Use `PermissionService`, `RequestLifecycleService`, and
  `AvailabilityService` as shared sources of truth. Do not duplicate their
  logic inside custodian UI code.
- Treat the session user as authoritative. Direct service calls by a logged-out
  user or another role must fail before mutation.
- Follow test-driven development: create a focused failing test, implement the
  smallest change to pass it, refactor, and run the relevant regression tests.
- Use temporary H2 databases and synthetic data in tests. Never use or reset
  `data/loandesk` as test data.
- Do not make unrelated borrower or supervisor changes. Flag shared-contract
  changes for teammate coordination before implementing them.

## Agent handoff protocol

Delegate **one stage only** to one implementation agent. The agent may make
only the changes needed for that stage and must not begin a later stage merely
because it looks convenient.

Before coding, the agent must state the stage boundary, inspect the relevant
shared contracts, and list its planned red tests. At the end of every stage,
the agent must stop and give the user this exact handoff report:

```markdown
## Custodian stage <number> complete

### Done
- Implemented: ...
- Tests added/changed: ...
- Verification run: `<exact command>` — PASS/FAIL

### Issues or decisions identified
- Confirmed issue: ...
- Deferred issue or risk: ...
- Shared-contract/team coordination needed: ...

### Manual checks for you
- [ ] Exact screen/action to exercise: ...
- [ ] Expected result: ...
- [ ] Decision or teammate confirmation needed: ...

### Not done
- Explicitly excluded from this stage: ...
- Recommended next stage: ...
```

The report must distinguish observed facts from recommendations. If blocked,
the agent must report the blocker and stop rather than guessing a new shared
policy. It must never describe unexecuted JavaFX interaction as tested.

## Stage 0 — Agentic safeguards and test fixtures

### Goal

Create the custodian review mechanisms before production custodian behaviour,
so they apply consistently to every later stage.

### Build

1. Create `loandesk-custodian-fulfilment-workflow-review`.
   - Trigger: checkout, return, lost/recovery, collection-window, request/loan
     transition, or custodian-permission changes.
   - Review: service authorization, legal transitions, collection-window
     enforcement, duplicate checkout prevention, and reciprocal request/loan
     links.
2. Create `loandesk-custodian-persistence-condition-review`.
   - Trigger: equipment condition, return/lost/recovery, H2 save, schema, or
     persistence changes.
   - Review: atomic multi-record saves, rollback, stale writes, restart
     persistence, and condition-to-availability/eligibility effects.
3. Add controlled defective/correct fixtures, manifest entries, and
   deterministic contract tests for both skills under `tools/custodian/`.
4. Add reusable custodian test fixtures for authenticated sessions, approved
   requests, active/lost loans, and equipment conditions.

### TDD and verification

- Write fixture/contract tests before declaring either skill ready.
- Run the existing full Gradle suite plus the custodian fixture checks.
- Arrange at least one fresh, read-only evaluation for each skill. Record it as
  `RUN`, `UNAVAILABLE`, or `INCONCLUSIVE`; fixture tests alone do not prove an
  agent review ran.

### Exit criteria

- Both skills are discoverable and have controlled fixtures.
- Existing tests remain green.
- No production checkout/return behaviour has been introduced yet.

## Stage 1 — Custodian authentication and protected entry

### Goal

Replace the temporary passwordless custodian session with the fixed seeded
account model used by the supervisor.

### Build

- Seed the custodian user and a PBKDF2 password credential during first-time
  database initialization.
- Add dedicated custodian password login and remove passwordless entry.
- Wire protected dashboard entry and logout.
- Continue using the existing custodian permissions in `PermissionService`.

### TDD cases

- Correct custodian password succeeds.
- Invalid password and missing seeded account fail without a session.
- A fresh store reload recognizes the persisted custodian account.
- Logged-out, borrower, and supervisor callers cannot invoke custodian service
  operations.

### Exit criteria

- Custodian access mirrors supervisor access for the MVP.
- No inventory, checkout, or return action is implemented in this stage.

## Stage 2 — Shared collection window

### Goal

Make the three-day grace period one shared rule before any custodian checkout
screen can depend on it.

### Build

- Change the lazy expiry rule to expire approved requests only after
  `startDate + 3 days`.
- Keep approved equipment reserved throughout the collection window.
- Count approved reservations throughout the same window for borrower limits.
- Expose/reuse the collection-window predicate from a shared service; do not
  reimplement date arithmetic separately in checkout code.

### TDD cases

- Start date and final grace date are collectable.
- Day after final grace date expires the request.
- The item remains `RESERVED` until expiry and is released after expiry.
- Borrower reservation limits use the same expiry boundary.
- Persistence/reload and lazy expiry remain correct.

### Teammate coordination

- Borrower owner: availability and reservation-limit behaviour changes during
  the three-day window.
- Supervisor owner: approved-booking expiry and approval availability checks
  must use the revised shared rule.

### Exit criteria

- One tested policy controls expiry, availability, and eligibility.
- Checkout itself is not implemented yet.

## Stage 3 — Collections queue and atomic checkout

### Goal

Allow a custodian to issue a currently collectable approved item once.

### Build

- Add a custodian application service and a read-only collections queue.
- Require `CHECK_OUT_LOAN`.
- Reload shared state and apply the expiry sweep before displaying or acting.
- At checkout time, recheck the request is `APPROVED`, today is inside the
  collection window, and the item is currently available and `GOOD`.
- Create a generated `ACTIVE` loan, transition the request to `COLLECTED`, and
  link both records.
- Save one complete updated `LoanDeskData` snapshot in one transaction.
- Translate stale-save failures to a clear refresh/retry message in the UI.

### TDD cases

- Valid checkout creates exactly one linked active loan and collected request.
- Early, late, pending, rejected, cancelled, expired, and previously collected
  requests are rejected.
- Wrong-role and logged-out calls do not mutate data.
- Damaged, maintenance, lost, reserved, and on-loan items are rejected.
- A second checkout cannot create a second loan.
- Failed saves and two stale checkout attempts leave no partial/duplicate state.
- A fresh store reload preserves the complete checkout result.

### Exit criteria

- A collectable approval can be checked out exactly once.
- The borrower can see the resulting active loan through existing read-only
  history functionality.

## Stage 4 — Returns, conditions, loss, and recovery

### Goal

Complete the physical fulfilment lifecycle without introducing maintenance
records or notes.

### Build

- Add active-loan view with derived overdue display.
- Allow custodians to return active loans and select `GOOD`, `DAMAGED`, or
  `UNDER_MAINTENANCE` condition.
- Allow custodians to mark an active loan/item `LOST`.
- Allow custodians to recover a lost item by changing its loan to `ACTIVE`
  and its condition to `GOOD`, so they can then select and record its return
  condition.

### Required outcomes

| Operation | Loan | Equipment condition | Availability |
| --- | --- | --- | --- |
| Good return | `RETURNED` | `GOOD` | `AVAILABLE` if no other blocker exists |
| Damaged return | `RETURNED` | `DAMAGED` | `UNAVAILABLE` |
| Maintenance return | `RETURNED` | `UNDER_MAINTENANCE` | `UNAVAILABLE` |
| Mark lost | `LOST` | `LOST` | `UNAVAILABLE` |
| Recover lost item | `ACTIVE` | `GOOD` | `ON_LOAN` |

### TDD cases

- Only custodians may return, mark lost, or recover.
- Only active loans may be returned or marked lost.
- Returned loans cannot be processed repeatedly. Lost loans may be recovered
  once to `ACTIVE`, after which the custodian records the return condition or
  marks the item lost again.
- Return dates cannot precede checkout; recovery has no return date.
- Damaged, maintenance, and lost equipment block approval and checkout.
- A lost loan blocks borrower eligibility; recovery removes that blocker.
- Each multi-record update rolls back completely on a failed save and persists
  exactly after restart.

### Teammate coordination

- Borrower owner: verify lost/recovered eligibility and loan-history display.
- Supervisor owner: verify physical conditions block approval through shared
  availability.

### Exit criteria

- All agreed physical states and their cross-role consequences are tested.

## Stage 5 — MVP inventory

### Goal

Let the custodian manage the minimal shared equipment inventory.

### Build

- Add equipment with a unique ID and non-blank name.
- Edit equipment name and condition.
- Display derived availability as read-only.
- Preserve existing request/loan references during valid edits.

### TDD cases

- Blank and duplicate IDs, blank names, wrong-role calls, and failed saves are
  rejected without mutation.
- Added/edited equipment persists on a fresh H2 reload.
- Condition updates immediately change derived availability.

### Explicit exclusions

- Categories, descriptions, retirement, deletion, notes, and bulk import.

### Exit criteria

- The minimal inventory model is usable without changing borrower/supervisor
  feature behaviour beyond the shared condition/availability contract.

## Stage 6 — Cross-role acceptance and review

### Goal

Prove the integrated workflow, including restart and error handling.

### Required scenario

```text
Borrower submits request
→ Supervisor approves it
→ Custodian checks it out within the collection window
→ Borrower sees ACTIVE loan
→ Custodian returns, damages, loses, or recovers the item and records its
  return condition
→ Application restarts
→ All roles see the appropriate final shared state
```

### Verification

- Run focused integration tests plus the full Gradle test suite.
- Run both custodian review skills and the existing supervisor workflow review
  for the shared lifecycle surface.
- Arrange a small, fresh independent review panel: workflow/authorization and
  persistence/transaction reviewers at minimum.
- Manually exercise each custodian screen because the project has no JavaFX
  interaction harness.

### Exit criteria

- The required scenario passes from a fresh local database.
- Findings, unavailable reviews, and manual-test limitations are reported
  honestly before documentation begins.

## Stage 7 — Documentation and final evidence

Perform this stage only after Stages 0–6 meet their exit criteria.

- Update `docs/UserGuide.md` with custodian login, collection window,
  checkout, returns, conditions, loss/recovery, and scope exclusions.
- Update `docs/DeveloperGuide.md` with the service boundaries, shared date
  rule, permissions, snapshot-save behaviour, and retry semantics.
- Update `ProjectContext.md` and `ProjectChecklist.md` to reflect
  completed work only.
- Update `docs/AgenticSE.md` with custodian skill purpose, evaluations, and
  limitations.
- Add dated logs containing commands, results, review evidence, decisions, and
  manual-check outcomes.

## Snapshot-save implementation note

The current `DatabaseDataStore.save` transaction deletes and reinserts the
entire stored snapshot, then commits only if its revision has not changed.
That preserves atomic checkout/return changes but is coarse-grained: any newer
unrelated save makes the older snapshot fail. For this MVP, retain this design,
show a refresh/retry message, and test conflicts. Do not introduce a targeted
JDBC checkout repository unless the team later requires multiple concurrently
used application instances or observes unacceptable conflict rates.
