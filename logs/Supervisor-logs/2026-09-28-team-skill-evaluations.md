# 2026-09-28 Team-level skill evaluations

First controlled evaluation of the four team-level review skills. Each fresh
reviewer had no inherited conversation and was given only the four blinded
files named in its `review-prompt.txt`: the skill, `requirements.md` and the
two cases. None was given tests, build scripts, answer sheets, prior findings
or project logs. Each answer sheet was read only after the response was
preserved. All four reviewers were invoked explicitly, so these evaluate
review ability, not automatic skill selection.

## Results

| Skill | Planted | Detected | Missed | False positives |
| --- | --- | --- | --- | --- |
| `cross-role-scenario-review` | 1 | 1 | 0 | 0 |
| `persistence-failure-test-review` | 1 | 1 | 0 | 0 |
| `release-readiness-review` | 2 | 2 | 0 | 0 |
| `documentation-consistency-review` | 1 | 1 | 0 | 0 |

Every reviewer stated explicitly that it executed nothing, and every reviewer
reported case B clean.

## Per-skill assessment

### Cross-role scenario review

Identified that `checkout` in case A creates the loan but never updates the
request, leaving an ACTIVE loan against a request still reporting APPROVED
with a null `loanId`. It gave the supervisor-then-custodian sequence that
exposes it and located it correctly.

It traced one consequence the answer sheet had not spelled out: because the
request never reaches COLLECTED, the `status() != APPROVED` guard never fires,
so a second checkout succeeds and creates a second ACTIVE loan for the same
request. It still described the root defect as isolated to the missing
request-side write rather than counting the duplicate loan separately.

### Persistence-failure test review

Identified that case A mutates its live `loans` and `equipment` fields before
`sink.write`, with no rollback, so a caller catching the IOException observes a
partially applied return. It supplied the injected-failure reproduction and
proposed the right test: assert the whole snapshot equals the pre-call state,
not merely that an exception was thrown.

It avoided the planted trap, explicitly confirming that the refusal paths and
the exception propagation are correct rather than condemning the operation. It
also noted that passing the live maps by reference into the Sink would let a
retaining implementation mutate internal state, and labelled that a design
note rather than a separate defect because the requirements do not address
Sink trust. That restraint was the behaviour the trap was built to measure.

### Documentation consistency review

Identified the contradiction in candidate A: the guide states an approval note
is mandatory and that approvals without one are rejected, while `approve`
normalizes a blank note to null and proceeds. It quoted both sides and
declined to decide whether the guide or the code should change, which is the
correct boundary for a review.

It avoided both traps, confirming that the documented 30-day window matches
`MAX_DAYS_AHEAD` and that the maintenance statement is accurate because the
feature is genuinely absent. It separately reported an undocumented
permission requirement as a gap, which the sheet allows but does not require.

One minor deviation: it marked the "pending reservation" claim unverifiable
because the `decide` body was not supplied, although `requirements.md` asks
the reviewer to assume the excerpts are complete and representative. It did
not assert a defect on that basis, so this is cautious rather than incorrect.

### Release readiness review

Identified both planted items in candidate A: the blocker, a distribution
carrying only `-win` JavaFX artefacts under a three-platform claim, and the
separate gap, the missing developer-guide acknowledgement section. It avoided
both traps, accepting the recorded maintenance limitation and treating the
`v1.0.0` tag as conventional rather than a mismatch.

## Correction to the release-readiness answer sheet

The reviewer reported a gap the answer sheet had not anticipated: candidate B
claims macOS support and ships macOS artefacts, but its recorded verification
covers only Windows and Ubuntu. Requirement 6 of that fixture says a
verification claim must be supported by the supplied evidence and that an
unstated check must not be assumed performed, so this is a legitimate gap. The
reviewer classified it as a gap rather than a blocker and still judged
candidate B releasable, which is the correct severity call.

The answer sheet, not the reviewer, was wrong. `expected-results.md` has been
amended with a dated correction recording the corrected scoring. The fixture
files themselves were deliberately left unchanged so this recorded evaluation
still matches what the reviewer was given.

## Verification

- `python tools/team/test_skill_contracts.py`: 8 tests, passed, before and
  after the answer-sheet correction.
- `.\gradlew.bat -p tools/team/skill-evaluations/cross-role verifyFixtures --no-daemon`:
  case A ran 8 tests failing exactly `checkoutLeavesTheRequestAndItsLoanAgreeing`;
  case B ran 8 with none.
- `.\gradlew.bat -p tools/team/skill-evaluations/persistence-failure verifyFixtures --no-daemon`:
  case A ran 6 tests failing exactly `aFailedSaveLeavesTheInMemoryStateUnchanged`;
  case B ran 6 with none.

## Limitations

- The release-readiness and documentation-consistency fixtures have no
  executable ground truth. Their answer sheets are the only ground truth and
  the assessment rests on a human comparison, so their results are weaker
  evidence than the two Java pairs. The sheet flaw found in this session is a
  direct illustration: a descriptive fixture's ground truth is only as good as
  the person who wrote it.
- Each skill has one evaluation against one planted defect. That does not
  establish that any of them detects defects of its class generally.
- Automatic skill selection remains untested for all of them. An earlier
  reviewer reported that its runtime did not load `.agents/skills/` as a
  registered skill directory, so skills were supplied as explicit text in
  every run here.
- No skill text was changed as a result of these evaluations; only the
  release-readiness answer sheet was corrected.
