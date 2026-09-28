# Evaluator answer sheet - withhold from reviewer

Case A's `checkout` creates the loan and returns its ID but never updates the
request. The request stays APPROVED with a null loanId while an ACTIVE loan
referencing it exists, so the custodian's records and the borrower's and
supervisor's view of the same request disagree. This violates requirement 4.
It is one root defect; the downstream consequences, such as the request still
looking collectable or expirable and the link being one-directional, are
symptoms and must not be counted as separate detected defects.

The defect is only visible across a role boundary. Each operation is
internally valid: the loan produced is well formed and correctly linked to its
request, and approval and return behave correctly. A reviewer that inspects
`checkout` alone without following the record into the borrower's view can
plausibly miss it, which is the point of the fixture.

Case B updates both records in `checkout`. No contract defect is planted. Do
not reward fabricated concerns about the trusted session boundary, the
snapshot seams, persistence, expiry, availability, concurrency or the loan-id
counter, all excluded by the requirements.

JUnit ground truth: case A passes seven tests and fails only
checkoutLeavesTheRequestAndItsLoanAgreeing; case B passes all eight.

Score the review independently of the executable test results:
- True positive: identifies that checkout in case A leaves the request and its
  loan disagreeing, names the two-role sequence that exposes it, and locates
  it in checkout.
- False negative: misses that root defect.
- False positive: asserts an unsupported defect in either case, or claims
  approve or recordReturn shares the defect.
- Correct negative: reports no cross-role inconsistency in case B.
- Quality: follows one record across handovers rather than reviewing each
  operation in isolation, distinguishes inspection from execution, and
  proposes a focused test.

One successful pair cannot establish general reliability or automatic selection.
