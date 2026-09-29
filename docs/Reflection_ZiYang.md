# Reflection

## Withdrawing a convenience fix at the persistence boundary

While implementing supervisor sign-in, I hit a problem that looked trivial: a
database created before my change had no supervisor account, so there was
nothing to sign in as. My first fix was to add the supervisor account to any
database that was missing one during `loadOrSeed`, so the account would simply
appear whenever the data was read.

I asked whether this was safe:

> If the old database doesnt have a supervisor account, could we just add one
> during `loadOrSeed` so an existing db can still sign in? That seems easier
> than telling people to delete their data folder. Or would writing during a
> read break the revision checks?

Working through it, I withdrew the change. Seeding inside `loadOrSeed` meant a
read mutated shared state, which advanced the database revision and broke both
the optimistic-concurrency contract and the legacy-database contract that the
persistence tests pin down. The supervisor account is now seeded on first
launch only, and an older database reports that the local `data/loandesk` files
must be removed to reseed.

The learning point was that the convenience was not free: it traded a clear,
recoverable error message for a silent write on every read, in a component
owned by someone else. A fix that is one line in my lane can be a contract
change in another person's lane, and the boundary is where I should have looked
first rather than last.

## Permissions as a shared mechanism rather than a supervisor feature

The supervisor role was the first one to need a real authorization check, so
the natural move was to write that check for the supervisor. Instead I put one
permission matrix in `PermissionService` covering all three roles, and migrated
the four borrower services off their ad-hoc `Session.requireRole` calls onto
it.

Before doing that I asked:

> Does this permission check need to cover all three roles, or should I just do
> the supervisor one since thats the only role that needs it right now? I was
> thinking of one matrix and moving the borrower services onto it, but I dont
> want to touch someone elses files if theres no real benefit.

I did this because a mechanism exercised by exactly one role is not really
tested — it is a guess that happens to compile. Once the borrower services ran
through the same matrix, two roles were exercising it and the disagreements
surfaced immediately.

The migration also forced a distinction I had been blurring. Role permission
("may a supervisor decide requests at all") belongs in the shared matrix, but
record ownership ("may this borrower read this particular request") stays in
the owning service, because only that service knows what ownership means for
its records. Collapsing the two into one table would have looked tidier and
been wrong.

## Changing a shared domain invariant

My lane needed `LoanRequest` to stop requiring a decision reason for every
decision, because the agreed request policy only requires a reason to reject.
That is a shared domain type, not a supervisor file, so the change reached
beyond my own work.

I checked before changing it:

> `LoanRequest` makes a decision reason compulsory for every decision, but our
> request policy only needs a reason when rejecting. Can I change that record
> even though the borrower side uses it too? And how do I stop someone adding
> the constraint back later once they see approvals with no reason?

Rather than just relaxing the constraint, I added `ApprovalReasonInvariantTest`
to pin the new rule in place. The reason was that the old invariant was
plausible enough that another owner could reinstate it in good faith months
later; a test states the policy as executable text instead of leaving it as an
unwritten agreement.

The same pattern appeared in the persistence guard, which required the
canceller of a request to be the borrower. That had been correct while only
borrowers could cancel, and it would have rejected every supervisor
cancellation of an approved booking. I learned that a guard written for one
role reads as a general rule long after it has stopped being one, and the
cheapest way to find those is to run a second role through the same path.

## Designing a fixture that can catch an overstated finding

When I built the controlled evaluation for the permission/workflow review
skill, my first instinct was to plant a defect and check whether a reviewer
found it. I changed the design: the defect lives in `approve` only, and
`reject` in the same file is left correct.

The prompt I gave the fresh reviewer was deliberately blinded:

> You are performing an independent, read-only code review. Read exactly these
> four files and no others ... Do not read tests, build scripts, answer sheets,
> prior results, project logs or conversation history ... Do not assume either
> case has a defect. Return your review without fixing anything.

I wrote it that way because an evaluation that only rewards finding the bug
cannot tell a careful reviewer apart from an alarmist one. Both score the same
when the whole file is condemned. By leaving `reject` correct, the fixture has
a discriminating question: does the reviewer scope the finding to the method
that is actually broken?

The reviewer identified the missing source-status check in `approve` with its
line range and a direct-call reproduction, and explicitly reported `reject` as
correct. Case B was reported clean with each requirement checked rather than
waved through. One defect planted, one detected, none missed, none invented.

The learning point was that the value of a fixture is in what it can rule out,
not only in what it can catch. Instructing the reviewer not to assume a defect
exists, and giving it a case where the honest answer is "nothing here", tests
restraint — which is the failure mode I actually worry about in an automated
reviewer.

## Skill presence is not skill selection

The evaluation produced a finding I had not gone looking for. The reviewer
reported that its `Skill` tool did not recognize
`loandesk-supervisor-permission-workflow-review` as a registered skill, so it
followed the `SKILL.md` text as supplied instructions instead.

This project stores skills under `.agents/skills/`, which is the Codex
convention. A different agent runtime may not load that directory at all. The
result was unaffected, because my protocol hands the skill to the reviewer
explicitly, but it is direct evidence that automatic selection cannot be
assumed from a file's presence. The same caveat applies to the five borrower
skills in the same directory.

What I took from this is that my evaluation measured review ability and nothing
else. I have evidence that the skill reviews well when invoked, and no evidence
at all that it will be invoked at the right moment. Those are two separate
claims and I had been treating them as one. It also matches what my teammate
found from the opposite direction, where passing tests validated a skill's
checks but not its invocation timing.

## Recording what was not verified

The strongest habit I picked up was writing down the absences. My session logs
state that no manual GUI test was performed, that no independent review panel
was dispatched, that the CI workflow itself had never executed, and that the
three supervisor screens are verified only by compilation and by the service
tests behind them.

This mattered because 130 passing tests are easy to present as "the supervisor
workflow works". They do not show that a person can sign in, filter the queue,
approve, reject, cancel and read decision history in the actual interface,
because the project has no JavaFX interaction harness. The project's own
logging rule is that a summary must not claim an interaction or test occurred
when it did not, and the pressure to round an absence up to a pass is real
precisely when everything else is green.

I also left a failure alone rather than tidy it away.
`tools/borrower/test_hooks.py` reports ten errors on this machine because
`write_text(newline=...)` needs Python 3.10 and the local interpreter is 3.9.6.
It is a pre-existing environment mismatch in a borrower-owned harness, so I
recorded it and did not modify it.

## Keeping CI scope honest

When I expanded CI from a single test step, the temptation was to add every
check I could think of. I kept two things out on purpose.

`test_hooks.py` stayed out because it fails on this machine and its behaviour
on a Linux runner is unverified; adding it would have made CI red for a reason
unrelated to whatever change was under review, which trains people to ignore
red. No formatter check was added either, because `build.gradle` configures no
formatter and picking one is a team decision, not something to settle inside a
CI commit.

What I did add was a failure mode rather than a feature: the build job uploads
the packaged distribution with `if-no-files-found: error`, so a silent
packaging regression fails the run instead of quietly producing nothing. I
split the skills job from the build job for the same reason, so an
evaluation-protocol regression is distinguishable at a glance from an
application failure.

## What I would carry into another project

- Look at the shared boundary before writing the convenient fix, not after.
  The persistence seeding attempt cost more to unpick than reading the
  concurrency contract would have cost up front.
- Run a second role or a second caller through any new shared mechanism in the
  same change. One caller does not test a design.
- Pin a changed invariant with a test, so a policy decision survives as
  executable text rather than as an agreement someone can reasonably undo.
- Build evaluation fixtures that can be failed by overreaction as well as by
  oversight, and include a case where the correct answer is that nothing is
  wrong.
- Separate "the skill reviews well" from "the skill gets invoked". Evidence for
  one is not evidence for the other.
- Write the limitations section first when the tests are all passing, because
  that is when an unverified claim is easiest to make and hardest to notice.
- Keep unverified checks out of CI. A check that fails for an unrelated reason
  is worse than no check, because it teaches the team to ignore the signal.
- Leave another owner's broken harness recorded and untouched rather than
  fixing it inside an unrelated change.

Overall, the supervisor lane taught me less about writing features than about
stating claims precisely. Most of the real work was deciding what I actually
had evidence for — which contract a change touches, which role has exercised a
mechanism, whether a green suite says anything about the interface, and whether
a skill that reviews well will ever be chosen.
