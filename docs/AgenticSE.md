# Agentic SE Skills and Hooks

This document defines the proposed custom development-agent practices for
LoanDesk. These practices support the assignment reflection; they do not add
AI functionality to the LoanDesk product.

## Skill format

Every skill should specify its trigger, required inputs, checks, output, and
verification evidence. A skill evaluation must use a controlled fixture or
isolated test change. Do not leave intentional defects in the release branch.

## Proposed skills

### 1. UI acceptance reviewer

**Trigger:** A borrower, supervisor, or custodian UI feature is ready for review.

**Inputs:** Feature requirements, relevant view/controller code, and acceptance
scenario.

**Checks:** Required-field validation, date validation, understandable errors,
role-appropriate controls, displayed status, loading/error states, and ownership
of displayed records.

**Output:** Findings with severity, file/symbol, reproduction scenario, and a
suggested focused test.

**Verification:** Give it a controlled UI change with missing validation or an
incorrect status label and check that it identifies the defect.

### 2. Permission and workflow reviewer

**Trigger:** A service operation or request-state transition is added or changed.

**Inputs:** Role matrix, lifecycle rules, service code, and tests.

**Checks:** Service-layer authorization, record ownership, legal transitions,
availability rechecks at approval, duplicate checkout prevention, and required
reasons for rejection/cancellation.

**Output:** A transition/permission table followed by findings and missing tests.

**Verification:** Evaluate it against fixtures containing a wrong-role call and
an illegal transition, then record whether both are found.

### 3. Persistence-failure tester

**Trigger:** Repository, save, seed, migration, or multi-record update code changes.

**Inputs:** Repository contract, persistence implementation, failure injection
point, and consistency invariant.

**Checks:** Restart persistence, first-run initialization, no reseeding over
changes, recovery from malformed data, and atomicity of loan/equipment updates.

**Output:** Focused test cases, expected state before/after failure, and observed
results.

**Verification:** Inject a controlled save failure during a damaged return and
check that partial state is not committed.

### 4. Cross-role scenario tester

**Trigger:** A milestone integrates two or more role workflows.

**Inputs:** Seed data, acceptance scenario, and runnable application/build.

**Checks:** Submit, approve, check out, return, restart, and history visibility;
also verifies that each role sees only the operations it owns.

**Output:** Scenario steps, evidence, pass/fail result, and defects with owners.

**Verification:** Run the first integration acceptance scenario on a clean local
store and retain the test output or screenshots.

## Hooks and automation

Hooks should be lightweight developer feedback. CI is authoritative because
local hooks can be bypassed.

- **Pre-commit:** formatting check and fast unit tests.
- **Commit message hook:** require a simple prefix such as `feat:`, `fix:`,
  `test:`, `docs:`, or `build:`.
- **Pre-push:** run the complete `./gradlew test` task.
- **Pull-request CI:** use Java 25, run tests, and verify the project builds.
- **Release CI:** build the distributable artifact and attach it to a tagged
  GitHub release after manual cross-platform checks.
- **Dependency/security check:** scan dependencies and detect accidentally
  committed secrets.
- **Documentation check:** verify required guide, reflection, and log paths
  exist before submission.

Do not make hooks block ordinary experimentation with slow UI tests. Keep the
fast checks local and the complete checks in pull-request CI.

## Evidence to retain

For each evaluated skill, keep the original instruction, test prompt/fixture,
actual output, human verification, corrections, and limitations. Summarize
these in `docs/Reflections.md` later. Session-specific AI summaries belong under
`logs/Yikbing-logs/`.
