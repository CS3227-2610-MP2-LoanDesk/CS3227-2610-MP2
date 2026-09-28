---
name: loandesk-documentation-consistency-review
description: Review whether LoanDesk guides match the product when a user guide, developer guide or feature description is written or changed, after a behaviour change lands, or when assembling documentation for delivery. Check each documented claim against the code that implements it and report contradictions as defects.
---

# Documentation consistency review

Read the changed documentation and the code paths it describes. Resolve paths
from the repository root. Peer testers follow the guide literally, so a guide
sentence that the product contradicts is a defect in the same sense as a wrong
return value; report it that way rather than as a wording preference.

Work claim by claim rather than section by section. For each statement a
reader could act on, locate the code that decides it and compare:

- Named values match: seeded accounts, passwords, identifiers, limits,
  defaults and date rules stated in the guide match the constants and
  validation in the services.
- Required and optional inputs match. A field the guide calls optional must
  not be rejected when blank, and one it omits must not be mandatory.
- Described outcomes match. A status, message or transition the guide promises
  must be the one the code produces.
- Described availability matches ownership. A feature the guide presents as
  usable must not depend on work another role has not finished.
- Steps are executable in the order written, from the state the guide assumes
  the reader is in.
- Behaviour that exists but is undocumented is reported as a gap, separately
  from contradictions.

Check the guides against each other too: a developer guide describing a design
the user guide contradicts is one defect, not two, and the code decides which
of them is wrong.

Treat deliberate scope statements fairly. A guide that says a feature is not
yet implemented is accurate when the feature is absent; it becomes a defect
only once the feature exists or once the statement misleads about what a
reader can do today.

Report a claim table: the quoted documentation statement, the file and symbol
that decides it, and whether it matches. Follow it with contradictions ordered
by how likely a reader is to act on them, each with the quoted text, the
observed behaviour and the smallest correction. State which claims you
verified by reading code, which by executing it, and which you could not
verify.

For a review-only request, report without editing. In an authorized
documentation task, correct only the claims you verified, and never resolve a
contradiction by changing the code to match the guide without its owner's
agreement. Log meaningful evaluation results and limitations under logs/.
Checking the claims in one guide section says nothing about the others.
