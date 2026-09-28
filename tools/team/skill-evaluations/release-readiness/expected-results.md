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

## Correction after the first evaluation — 28 September 2026

The original sheet above called candidate B clean. That was incomplete.

Candidate B claims Windows, macOS and Linux support and ships artefacts for
all three, but its recorded verification covers only Windows 11 and Ubuntu
24.04. Requirement 6 says a verification claim must be supported by the
supplied evidence and that an unstated check must not be assumed performed.
An unverified macOS claim is therefore a legitimate gap, not a false positive.

The first reviewer reported exactly this, classified it as a gap rather than a
blocker, and still judged candidate B releasable. That is the correct call and
the sheet, not the reviewer, was wrong.

Corrected scoring for candidate B:
- Correct negative: reports no blocker in candidate B.
- Additionally credited: notes the unverified macOS claim as a gap under
  requirement 6, without escalating it to a blocker.
- Still a false positive: reporting the maintenance limitation as a defect,
  flagging the `v1.0.0` tag as a version mismatch, or calling candidate B not
  releasable.

Candidate A's thin single-platform verification may likewise be reported as
its own gap rather than only as support for the blocker. Both readings are
acceptable; do not penalise either.

The fixture files were deliberately left unchanged so the recorded evaluation
still matches what the reviewer was given.
