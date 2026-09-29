# Agentic Software Engineering Reflections

This document contains reflections on basic Agentic Software Engineering by
the project contributors. Each section is identified by author.

# ZiYang's Reflections

## Withdrawing a convenience fix at the persistence boundary

While implementing supervisor sign-in, I considered adding a missing
supervisor account whenever an old database was read. I asked whether this
would be safer than asking users to reseed their local data. The change was
withdrawn because seeding inside `loadOrSeed` would make a read mutate shared
state, advance the database revision, and violate both the optimistic
concurrency and legacy-database contracts. The final behaviour seeds the
account on first launch and reports an actionable reseeding requirement for
older databases.

The learning point was that a convenient fix can silently change a shared
contract. Shared boundaries should be inspected before implementation, not
after a local fix has already been written.

## Permissions as a shared mechanism rather than a supervisor feature

The supervisor role was the first role requiring real authorization, but I
implemented one permission matrix covering borrower, supervisor, and
custodian operations. The borrower services were migrated to the same
mechanism so that more than one role exercised it.

This distinguished role permission from record ownership. A shared matrix can
decide whether a role may perform an operation, but the owning service must
still decide whether the signed-in user may access a particular record. A
mechanism used by only one role is not meaningfully tested; a second role or
caller exposes disagreements early.

## Changing a shared domain invariant

The request policy required a rejection reason but not an approval reason, so
the shared `LoanRequest` invariant had to change. I added an invariant test so
that the policy remained executable and could not be accidentally restored by
a future contributor.

The same issue appeared in cancellation persistence: a guard written for
borrower cancellation would have rejected supervisor cancellation. I learned
that guards often outlive the role assumptions that created them, so shared
operations should be exercised by every relevant role.

## Designing a fixture that can catch an overstated finding

For the permission/workflow review evaluation, I left one method defective and
another method in the same file correct. The blinded reviewer was instructed
to inspect only the supplied materials, not assume a defect, and report without
fixing anything.

The reviewer found the missing source-status check in `approve`, reported a
direct-call reproduction, and explicitly judged `reject` correct. The second
case was reported clean. This demonstrated that a useful fixture tests for
overreaction as well as missed defects and includes cases where the right
answer is that nothing is wrong.

## Skill presence is not skill selection

An evaluation showed that a reviewer could follow the supplied skill text even
when its runtime did not recognize the repository skill as registered. The
review itself was useful, but this did not prove that the skill would be
automatically selected at the correct time.

The distinction is important: evidence that a skill reviews well is separate
from evidence that the development workflow invokes it when needed.

## Recording what was not verified

I learned to record absences explicitly. Passing service tests did not prove
that a user could operate the supervisor screens, and the project had no
JavaFX interaction harness. I also recorded an unrelated Python-version
failure in an existing borrower hook rather than changing another owner's
harness inside an unrelated task.

The pressure to turn a green test suite into a broader claim is real. Honest
logs should state exactly what was inspected and executed.

## Keeping CI scope honest

When expanding CI, I excluded an unverified hook test and a formatter that had
not been agreed on. I did add packaging failure detection and separated the
skills job from the application build so failures remained meaningful and
diagnosable.

The lesson was that an unreliable check can train a team to ignore red builds;
checks should be added when their environment and purpose are understood.

## What I would carry into another project

- Inspect shared boundaries before implementing convenience fixes.
- Exercise new shared mechanisms through a second role or caller.
- Pin policy changes with focused tests.
- Build evaluation fixtures that detect both oversight and overreaction.
- Separate skill quality from skill-invocation reliability.
- Record limitations before presenting a green suite as evidence.
- Keep unverified checks out of CI until their environment is understood.

Overall, the supervisor lane taught me to state claims precisely: which
contract changed, which roles exercised it, whether tests say anything about
the interface, and whether a skill that works will actually be selected.

# Yikbing's Reflections

## Planning and skill-trigger reliability

During the migration from JSON to H2, a previously created skill triggered a
check that I had nearly forgotten. This showed how skills preserve planned
verification as the project grows. It also exposed the difference between
testing a skill's internal checks and testing whether the skill is invoked at
the right time. A reliable workflow needs both clear triggers and tests for
the skill's behaviour.

## Using multiple independent reviewers

Independent reviewers sometimes produced different findings from the same
prompt. I considered using multiple reviewers with narrower responsibilities.
The result was that focused reviewers can reduce blind spots, but their extra
time means they should be reserved for changes important enough to justify
the additional review.

## Independent review and persistence boundaries

Pre-PR reviewers found issues that were not visible through the GUI by calling
service and persistence methods directly. I initially questioned whether this
was appropriate because users cannot make those calls directly. I learned
that direct boundary testing is valuable because future UI changes can expose
those paths, and service-layer authorization must remain safe even when the
UI is bypassed.

## Recording and preventing recurring review gaps

Repeated findings led to a review-gap registry and changes to the completeness
review skill. I then recognized that preventing a known problem during
implementation is better than discovering it only during final review. This
led to the implementation-preflight skill reading earlier gaps before coding.

## Keeping documentation current

I noticed that guides were not always updated during feature work. Adding
documentation completeness to commit and pull-request readiness checks made
the omission visible earlier and reduced the chance of treating documentation
as an optional final task.

## Learning from the complete workflow

The later milestones became faster because policies, ownership, scope, tests,
documentation, UI conventions, review gaps, and reusable skill checks were
defined earlier. Independent review and hooks reduced routine verification,
but the skills did not replace my responsibility to make policy decisions,
review generated changes, test the GUI, and coordinate shared-role work.

Overall, I learned that Agentic SE moves effort earlier into planning,
automation, and reusable checks. That reduces rework without removing the
developer's responsibility for judgment and acceptance.

# Xinping's Reflections

## Reflection 1 — Creating a UI design skill

Creating a UI-design skill with detailed project design guidelines streamlined
the development process. It established a shared visual language for colours,
typography, spacing, cards, buttons, forms, feedback, and interaction states.
This created a consistent look across the project and reduced unnecessary
back-and-forth because the agent could consult the same rules when refining
screens.

The skill also clarified the boundary between UI and application logic. It
encouraged validation, authorization, state transitions, and persistence to
remain in services instead of being moved into JavaFX screens for convenience.

There was room for improvement because the original request was not specific
enough about component-level details. For example, buttons could have been
required to include icons when icons would make their purpose easier to
understand. If I repeated the task, I would specify icon expectations,
confirmation behaviour, and accessibility details in the skill, and pair it
with a structured UI acceptance checklist.

## Reflection 2 — Creating a focused custodian workflow review skill

The custodian fulfilment-workflow review skill converted high-risk workflow
knowledge into a repeatable review procedure. Checkout, return, loss,
recovery, and collection-window operations affect permissions, legal state
transitions, duplicate-loan prevention, and reciprocal request/loan links.
These risks can be missed when reviewing only the screen, especially when a UI
appears to disable invalid actions.

The skill directs the agent to trace each operation from the service boundary
through lifecycle and availability rules to persistence. It also requires an
operation table, direct service-call scenarios, expected versus observed
behaviour, and missing-test findings. This makes the review concrete and
prevents UI restrictions from being mistaken for authorization.

The main lesson was that a skill needs both a clear trigger and a clear output
contract. If I repeated the task, I would add more complete borrower-to-
supervisor-to-custodian integration fixtures and connect each review result to
a completion checklist before the feature could be considered ready.

## Reflection 3 — Creating a persistence and condition review skill

The custodian persistence-condition review skill addressed risks that differ
from workflow authorization. A return, loss, recovery, or condition update
changes multiple related records, so a UI can look correct while only part of
the state is saved. The skill makes atomic snapshots, rollback, stale-write
rejection, restart persistence, and condition-driven availability explicit
review targets.

Its controlled fixtures help reviewers distinguish a correct multi-record save
from a service that saves individual records separately. The requirements also
make recovery precise: it restores an active loan and `GOOD` equipment, does
not set a return date, and keeps the item on loan until its observed return
condition is recorded.

This skill improved testing efficiency by directing attention to failure and
restart scenarios instead of only successful UI actions. Its limitation is
that fixtures validate the evaluation protocol, not automatic skill selection
or the reasoning quality of a fresh reviewer. If I repeated the task, I would
add a fresh independent evaluation and a reusable failure-injection harness
for every multi-record custodian mutation.

## Overall reflection

The custodian work showed me that an effective agentic workflow combines
specialized skills with explicit assets and honest evidence. The UI skill
reduced design ambiguity, the workflow skill focused reviews on authorization
and legal transitions, and the persistence skill focused transactional and
restart behaviour. Together they reduced repeated explanations and directed
the agent toward the risks that mattered most.

Skills do not remove the need for human decisions. The developer still has to
define policy, choose the correct trigger, inspect the result, distinguish a
defect from an unresolved requirement, and verify interfaces that automated
tests cannot exercise.
