# Release readiness exercise requirements

This is a synthetic release candidate, not a real product and not a statement
about any actual LoanDesk release.

Review each supplied candidate independently against this contract:

1. The packaged distribution must support every platform the release notes
   claim. A dependency published with a platform classifier, such as a JavaFX
   artefact ending in `-win`, `-mac` or `-linux`, works only on that platform;
   a cross-platform claim requires every claimed platform's artefacts.
2. The version in the build, the planned tag and the release notes must agree.
   A leading `v` on a git tag is conventional and is not a mismatch.
3. The developer guide must contain an acknowledgement section citing reused
   ideas, code and documentation.
4. The user guide must describe the features the release actually has. A
   feature the guide explicitly marks as unavailable, and which the release
   does not claim to provide, is an honestly recorded limitation and is not a
   defect.
5. A product website must exist and describe the released version.
6. Verification claims must be supported by the evidence supplied. Do not
   assume a check was performed if the candidate does not say so.

Classify each finding as a blocker, which makes the release incorrect, a gap,
which makes it incomplete, or an accepted limitation, which is recorded
honestly and needs no action.

Assume the supplied evidence is complete and accurate: the listed archive
contents are the real contents, and the stated verification is all that was
performed. There is no access to the real repository, build or website. Do not
invent requirements for signing, notarization, installers, dependency scanning
or update channels; they are outside this exercise.
