---
name: loandesk-release-readiness-review
description: Review a LoanDesk release candidate before tagging or publishing, when preparing a formal release, packaging a distributable jar, or checking submission readiness. Check what the build actually produces against what the release claims, including cross-platform dependencies, guides, acknowledgements and the published website.
---

# Release readiness review

Read the required delivery items, then check the artefact and the claims
separately. Resolve paths from the repository root. A release is unready when
the build output, the documentation and the published pages disagree with each
other, even when every individual file looks finished.

Inspect what the build produces, not what the build script intends:

- List the actual contents of the packaged distribution. Platform-classified
  dependencies, such as JavaFX artefacts ending in `-win`, `-mac` or `-linux`,
  make the artefact usable only on the platform that built it. A release
  claiming cross-platform support must carry every required platform's
  artefacts or ship a per-platform build.
- The declared entry point runs from the packaged artefact, not only from a
  Gradle task in the development tree.
- The version recorded in the build, the tag and the release notes match.
- The artefact contains no local database, credential or ignored data file.

Then check the claims against the product:

- Every feature the user guide describes exists and behaves as described. A
  guide statement that no longer matches the product is a defect, not a
  documentation preference.
- Features implemented but undocumented are reported as gaps.
- The developer guide matches the released design and carries an
  acknowledgement section citing reused ideas, code and documentation.
- The published website exists, resolves, and reflects the released version.
- Any capability described as available is not in fact blocked, unverified or
  owned by unfinished work elsewhere.

Distinguish three outcomes clearly: a blocker that makes the release
incorrect, a gap that makes it incomplete, and an accepted limitation recorded
honestly. Do not mark a limitation as resolved because it is documented; do
not report an honestly recorded limitation as a blocker.

Report a table of required delivery items with observed status and evidence,
then blockers in order of severity, each with what was claimed, what was
observed, and the smallest change that closes it. State exactly which commands
you ran and which artefacts you opened, and never infer package contents from
the build script alone.

For a review-only request, report without editing or publishing anything. Do
not tag, release, or push. Verified release evidence belongs under logs/. One
passing build on one operating system is not cross-platform verification, and
must not be reported as such.
