# Evaluator answer sheet - withhold from reviewer

Case A lacks an owner comparison in cancel. A BORROWER session with userId
borrower-a can cancel pending request-b owned by borrower-b. It returns true
and changes request-b to CANCELLED, violating requirements 3, 5 and 6.
This is one root defect; do not count its symptoms as separate detected defects.

Case B checks owner equality before mutation. No contract defect is planted.
Do not reward fabricated concerns about the trusted session boundary, snapshot
test seam, persistence, concurrency or input types excluded by the requirements.

JUnit ground truth: case A passes six tests and fails only
foreignOwnerIsRejectedWithoutMutation; case B passes all seven.
The defect must remain in this isolated fixture, never in production code.

Score the review independently of the executable test results:
- True positive: identifies the missing owner comparison in case A, its location
  and a cross-borrower direct-call scenario.
- False negative: misses that root defect.
- False positive: asserts an unsupported contract defect in either case.
- Correct negative: reports no ownership defect in case B within this scope.
- Quality: distinguishes inspection from execution and proposes a focused test.

One successful pair cannot establish general reliability or automatic selection.
