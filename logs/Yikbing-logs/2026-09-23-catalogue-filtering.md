# 2026-09-23 Catalogue filtering

## Scope

Implemented the first borrower catalogue slice: read-only equipment display,
case-insensitive name filtering, clearing the filter and empty-results feedback.
Category, condition, availability and loan status remain deferred until the
request/loan foundation exists.

## Decisions

- Keep the existing shared `Equipment` contract (`id`, `name`) for this slice.
- Load equipment through the shared `DataStore` boundary.
- Keep filtering in `CatalogueService` rather than in JavaFX event code.
- Defer availability and condition fields until real request/loan records can
  support their calculation.

## Files changed

- `src/main/java/loandesk/application/CatalogueService.java`
- `src/main/java/loandesk/LoanDeskApp.java`
- `src/test/java/loandesk/CatalogueServiceTest.java`
- relevant borrower milestone, checklist, context, developer and user guide
  documentation

## Verification

- Pre-change `.\gradlew.bat clean test --no-daemon`: passed.
- Post-change `.\gradlew.bat clean test --no-daemon`: passed.
- Tests cover loading shared equipment, case-insensitive trimmed matching,
  blank/null filters, no-match results, and reload of synthetic equipment after
  recreating the H2-backed store.
- Post-fix `.\\gradlew.bat clean test --no-daemon`: passed after the
  restart-test and evidence updates.
- Pre-commit hook passed with expected shared-file coordination warnings.
- Manual GUI smoke test completed: the user verified `camera`, uppercase and
  partial-name filters, Filter, Clear and a non-matching search.
- There is no JavaFX interaction-test harness in the project; the Clear action
  and visible feedback were verified manually.
- No commit or push has been made.

## Review evidence

The borrower UI review inspected the new Catalogue screen and service call.
It checked filter application, clearing, no-match feedback, retained input,
repeated filtering and the read-only nature of the screen. The only finding
was that the original fixed-height scene could clip the results list; the
catalogue now uses a taller scene and groups the Filter/Clear controls.

The edge-case/test review covered successful loading, case-insensitive and
trimmed matches, blank/null filters, no matches, clearing, and restart-backed
loading through H2. All automated checks passed. No claim of exhaustive UI
coverage is made.

A first fresh two-member independent completion panel was dispatched against
the staged diff. Both reviewers returned `RUN` static reviews. The behaviour
reviewer identified that the restart test only reloaded seeded records, and the
UI reviewer identified a misleading message for an actually empty catalogue.
The empty-catalogue message was corrected and the restart test was then
strengthened to persist and reload synthetic equipment.

A second fresh two-member validation panel was dispatched after those fixes.
Both reviewers again returned `RUN` static reviews. The UI reviewer found no
new actionable issue. The behaviour reviewer confirmed the restart-test gap
until the synthetic-data assertion was added; the remaining Clear interaction
coverage limitation is accepted because the project has no JavaFX interaction
test harness. The panel did not run Gradle or JavaFX, so those are separately
evidenced below by local tests and the user's manual test.

The panel prompts asked for independent, read-only UI/interaction and
behaviour/test review of the staged catalogue change, without editing files,
changing local data, or relying on other reviewers' conclusions. Their reports
and limitations are summarized above; the findings were mapped to the focused
test and documentation updates above. The original two-member panel prompt
text was not copied into this log at dispatch time, which is recorded as a
process limitation rather than reconstructed as exact evidence.

## Final focused validation evidence

The following prompt and response are retained for the final validation that
checked the repaired persistence test and evidence updates.

### Exact prompt

> Repository: `C:\Users\yikbi\NUS\CS3227\MP2\CS3227-2610-MP2`
>
> Perform one final independent, read-only behaviour/test review of the
> staged catalogue fix. Focus on whether
> `src/test/java/loandesk/CatalogueServiceTest.java` now proves that custom
> non-seeded equipment survives `DatabaseDataStore` recreation, and whether
> the catalogue session log and `BorrowerMilestones.md` accurately record the
> completed manual test, panel evidence, and remaining limitations. Inspect
> the staged diff and relevant requirements. Do not edit files, commit, push,
> change local data, or rely on prior reviewer conclusions. Report RUN,
> UNAVAILABLE, or INCONCLUSIVE; include severity, file/symbol, reasoning, and
> any remaining focused action. Do not treat fixture rules as production
> policy.

### Decision-relevant response

> Result: `RUN` — static review completed. Runtime verification was
> unavailable in the review environment.
>
> No behavioural defect was found in
> `CatalogueServiceTest.loadsTheCatalogueAfterDatabaseRecreation()`:
> it saves non-seeded `tripod1`, creates a fresh `DatabaseDataStore` for the
> same path, and asserts that the catalogue contains exactly that record. This
> directly proves the requested recreation behaviour against the H2 contract.
>
> The manual-test record was appropriately scoped and the milestone accurately
> marked the catalogue work complete while leaving later workflow milestones
> open. The reviewer noted that the earlier panel prompts and responses were
> summarized rather than copied into this log; this entry addresses that gap
> for the final validation and records the earlier prompt-capture limitation
> explicitly.

The main workspace independently ran
`.\\gradlew.bat clean test --no-daemon` after the fix and received
`BUILD SUCCESSFUL`; the reviewer environment's runtime limitation does not
replace that local verification.

## Remaining review

- No catalogue-specific review action remains. The lack of automated JavaFX
  interaction coverage remains a documented limitation; manual GUI evidence is
  recorded above.
