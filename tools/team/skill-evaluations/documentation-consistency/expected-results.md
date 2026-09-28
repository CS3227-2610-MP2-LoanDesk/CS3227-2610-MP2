# Evaluator answer sheet - withhold from reviewer

Candidate A has one contradiction. The guide says an approval justification
note is required and that approvals without a note are rejected, but
`approve` normalizes a blank or missing note to null and proceeds; only
`decline` enforces a reason. This violates requirements 1 and 2. The smallest
correction is to the guide, since the code matches the decline behaviour the
guide also describes correctly.

Candidate B states the note is optional, which matches the code. No
contradiction is planted.

Both candidates contain two traps:

- The 30-day window. The guide says 30 days ahead and
  `ReservationRules.MAX_DAYS_AHEAD` is 30, so it matches. Reporting it as a
  mismatch is a false positive.
- The maintenance statement. The guide says maintenance reporting is not
  available and the repository state confirms no maintenance code exists, so
  requirement 4 makes it accurate. Reporting it as a defect is a false
  positive.

A reviewer may reasonably note as a gap that the guide does not document who
may approve or decline, given the code shows distinct permissions. Credit this
as a gap if clearly separated from contradictions; do not require it.

Score the review independently:
- True positive: identifies the required-versus-optional approval note
  contradiction in candidate A, quoting both the guide claim and the code.
- False negative: misses it.
- False positive: flags the 30-day window, flags the maintenance statement, or
  asserts a contradiction in candidate B.
- Correct negative: reports candidate B as consistent within this scope.
- Quality: separates contradictions from gaps, names which side should change
  and why, and states that nothing was executed.

There is no executable ground truth for this fixture; this answer sheet is the
ground truth. One successful pair cannot establish general reliability.
