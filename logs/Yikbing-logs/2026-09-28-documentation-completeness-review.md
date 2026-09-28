# Documentation completeness review — 28 September 2026

## Scope

Reviewed `docs/UserGuide.md` and `docs/DeveloperGuide.md` against the current
borrower implementation, including:

- borrower authentication and password rules;
- H2 persistence and local database behavior;
- catalogue filtering and borrower authorization;
- request submission, editing, cancellation and eligibility rules;
- loan/history display and derived overdue status;
- JavaFX layout and manual GUI verification;
- JUnit, hook and skill-evaluation commands;
- supervisor/custodian integration boundaries and release limitations.

## Findings and updates

The previous guides were accurate but too brief for the professor's
documentation expectations. They lacked concrete usage examples, detailed
validation rules, design rationale, an architecture diagram, extension
guidance, troubleshooting and explicit packaging limitations.

Both guides were expanded without changing product code. The User Guide now
contains prerequisites, local launch steps, database behavior, authentication,
catalogue, request, cancellation/editing, loans, demonstration steps,
troubleshooting and current limitations. The Developer Guide now contains the
architecture diagram, source layout, H2 tables, service boundaries, design
trade-offs, testing and review workflow, extension guidance, packaging status
and cross-role integration contracts.

## Verification

- `git diff --check`: passed.
- A stale-wording scan was performed against both guides.
- No product code or local database data was changed.
- No fresh independent agent panel was run because this was a documentation-only
  update; the completeness review was performed against the current source and
  existing test/documentation evidence.

## Remaining submission work

Screenshots should still be added to the User Guide or submission materials.
Packaged release and cross-platform verification remain open until the team
tests a JavaFX-containing distribution on the target operating systems.
