# Evaluator answer sheet - withhold from reviewer

Case A does not check the source status in approve. A SUPERVISOR session can
approve request-approved or request-collected, returning true and overwriting a
terminal status with APPROVED, violating requirements 3, 6 and 7. This is one
root defect; do not count its symptoms as separate detected defects. Note that
reject in case A does check the source status, so a reviewer who reports the
whole service as unguarded has overstated the finding.

Case B checks the source status before mutating in both operations. No contract
defect is planted. Do not reward fabricated concerns about the trusted session
boundary, the snapshot test seam, persistence, concurrency, expiry,
availability, eligibility or input types excluded by the requirements.

JUnit ground truth: case A passes seven tests and fails only
alreadyDecidedRequestIsNotApprovedAgainWithoutMutation; case B passes all eight.

Score the review independently of the executable test results:
- True positive: identifies the missing source-status check in case A approve,
  its location, and a direct-call scenario re-approving a terminal request.
- False negative: misses that root defect.
- False positive: asserts an unsupported contract defect in either case, or
  claims reject in case A shares the defect.
- Correct negative: reports no permission or lifecycle defect in case B within
  this scope.
- Quality: separates the authorization check from the transition check,
  distinguishes inspection from execution, and proposes a focused test.

One successful pair cannot establish general reliability or automatic selection.
