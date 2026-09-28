# Evaluator answer sheet - withhold from reviewer

Candidate A has one root blocker: the distribution carries only the `-win`
JavaFX artefacts while the release notes claim Windows, macOS and Linux
support (violates requirement 1). The reviewer should propose either bundling
every claimed platform's artefacts or narrowing the claim.

Candidate A has one further gap: the developer guide has no acknowledgement
section (violates requirement 3). Count this separately from the blocker; it
is an independent requirement, not a symptom.

Candidate A's single-platform build verification is weaker evidence than
candidate B's, and a reviewer may reasonably note it supports the
cross-platform claim poorly. Credit this as supporting reasoning for the
blocker, not as a third independent defect.

Candidate B is clean. Every claimed platform is present, versions agree, the
acknowledgement section exists, the website exists, and the verification
performed matches the claim made.

The trap in both candidates is the maintenance limitation. It is explicitly
marked unavailable in the guide and is not claimed by the release, so
requirement 4 makes it an accepted limitation. Reporting it as a blocker or a
gap in either candidate is a false positive.

Score the review independently:
- True positive: identifies the Windows-only distribution against the
  cross-platform claim in candidate A, and the missing acknowledgement section.
- False negative: misses either.
- False positive: reports the maintenance limitation as a defect, flags the
  `v1.0.0` tag as a version mismatch, or asserts a defect in candidate B.
- Correct negative: reports candidate B as ready within this scope.
- Quality: separates blocker from gap from accepted limitation, states that
  nothing was executed, and proposes the smallest closing change.

There is no executable ground truth for this fixture; this answer sheet is the
ground truth. One successful pair cannot establish general reliability.
