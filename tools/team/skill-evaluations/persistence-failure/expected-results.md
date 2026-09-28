# Evaluator answer sheet - withhold from reviewer

Case A mutates its own `loans` and `equipment` maps before calling
`sink.write`, and passes those same live maps to the Sink. When the write
throws, the in-memory state has already been changed and is never rolled back,
so a caller that catches the IOException observes the loan as RETURNED and the
equipment as DAMAGED even though nothing was persisted. This violates
requirement 3. It is one root defect; the aliasing of the live maps into the
Sink is part of the same defect, not a second one.

Note what is *not* wrong in case A: the refusal paths are correct, the
exception does propagate, and the successful path produces the right result.
A reviewer that reports the whole operation as broken, or claims the failure
is not propagated, has overstated the finding.

Case B builds updated copies, writes them, and only assigns them to its fields
after the write returns. A failed write leaves the originals in place. No
contract defect is planted. Do not reward fabricated concerns about
concurrency, restart, schema versions, the snapshot seams or the absence of
retry logic, all excluded by the requirements.

JUnit ground truth: case A passes five tests and fails only
aFailedSaveLeavesTheInMemoryStateUnchanged; case B passes all six.

Score the review independently of the executable test results:
- True positive: identifies that case A mutates in-memory state before the
  write succeeds and never restores it, with an injected-failure scenario.
- False negative: misses that root defect.
- False positive: asserts an unsupported defect in either case, or claims the
  refusal paths or exception propagation are wrong.
- Correct negative: reports no persistence defect in case B.
- Quality: judges the failure path rather than the happy path, proposes a test
  that injects a real write failure and asserts the whole state is unchanged,
  and distinguishes inspection from execution.

One successful pair cannot establish general reliability or automatic selection.
