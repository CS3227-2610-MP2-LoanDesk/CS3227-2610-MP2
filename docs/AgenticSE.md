# Agentic SE Skills and Hooks

This document defines the custom development-agent practices for
LoanDesk. These practices support the assignment reflection; they do not add
AI functionality to the LoanDesk product.

## Yikbing's borrower setup (implemented 21 September 2026)

Four repository skills and two personal Git hooks implement the setup agreed
with Yikbing. Skills allow automatic selection when their descriptions match;
they do not run continuously. Hooks run at commit/push after local activation.
See [DeveloperGuide.md](DeveloperGuide.md#borrower-skills-and-personal-hooks)
for usage, activation, disabling and verification. No online imports were installed.
The hooks were activated in Yikbing's checkout on 21 September 2026. Nine hook
fixture tests and the three original skill format checks passed. The fourth
skill passed manual frontmatter checks; its official validator could not run
because the available Python runtime lacked PyYAML. The real
pre-push hook also passed the full Gradle clean test suite. See the dated
[implementation log](../logs/Yikbing-logs/2026-09-21-borrower-skills-hooks-implementation.md).

Yikbing owns the borrower role; other members own supervisor and custodian.
Keep borrower work under `src/main/java/loandesk/features/borrower/`. Skills
may read shared contracts, but should flag unavoidable shared changes for
coordination. Ask before changing shared contracts or major architecture;
do not invent unresolved date, cancellation, or request-state policies.

### What each mechanism does

- **Skill:** reusable agent instructions for a task requiring judgment;
  optionally includes scripts and references. It does not guarantee correctness.
- **Git hook:** a deterministic script triggered by a Git event, such as a
  commit or push. Local hooks can be bypassed.
- **CI:** checks run on GitHub; the repository already runs Gradle tests on
  pushes to `main` and pull requests targeting `main`.
- **Independent reviewer:** a fresh agent invocation that reviews the change
  without inheriting the implementing conversation's conclusions. It is
  evidence for review quality, not a replacement for tests or human approval.

### Implemented custom skills

| Skill name | Purpose | Planned controlled evaluation |
| --- | --- | --- |
| `loandesk-borrower-ui-review` | Review catalogue filters, empty results, validation messages, request status, and enabled actions. | Detect an accepted invalid date range in an isolated fixture. |
| `loandesk-borrower-ownership-review` | Review service-layer ownership and edits/cancellation against agreed policy. | Detect borrower A being allowed to cancel borrower B's request. |
| `loandesk-borrower-edge-case-test-review` | Run and review existing tests, investigate failures, check valid success and safe rejection, and identify missing boundary, stale-state, repeated-action and save-failure tests. | Identify meaningful omissions and weak assertions in an incomplete test fixture. |
| `loandesk-borrower-change-completeness-review` | Check whether a borrower change has the necessary focused tests, documentation, session evidence, shared-contract coordination and independent PR review-panel evidence. | Review change completeness without duplicating detailed UI, ownership or edge-case analysis. |

Each review should report concrete findings, file references, reproduction
steps, and verification evidence. Evaluate against both defective and correct
examples, and retain missed defects and false positives as well as successes.
Do not leave intentional defects in production code.

### Independent PR review workflow

For a borrower feature or milestone completion check, or a meaningful borrower
pull request, run the completeness skill and arrange the smallest fresh,
read-only review panel covering the change surface, with no more than three
reviewers. Use UI/interaction plus behaviour/test reviewers for catalogue or
screen changes; authentication/security plus persistence/concurrency reviewers
for password or database changes; and ownership/workflow plus behaviour/test
reviewers for request/loan changes. Add a third reviewer only when the change
surface warrants it.

Each reviewer receives the requirements and raw staged change only, with no
implementing-agent conclusions or other reviewer reports. Record each result as
`RUN`, `UNAVAILABLE` or `INCONCLUSIVE`; a partial panel must not be described
as a complete pass. Reconcile duplicate findings and disagreements, map
findings to focused tests or decisions, and save prompts, responses and
decision-relevant evidence in a dated log when the review affects milestone or
PR readiness. Repeat the panel before a PR if meaningful changes were made
after the last completion check. Report unavailable or inconclusive reviews
honestly.

The PR template records this evidence for GitHub reviewers. The repository's
GitHub Actions workflow remains responsible for build/test checks. Local hooks
do not launch agents or call GitHub because they must stay deterministic,
offline-capable and quick. GitHub Copilot or another GitHub-integrated review
service can add PR comments separately when enabled by the repository owners;
that service is not configured by the local skill.

Routine implementation substeps, small documentation edits and ordinary test
runs use the relevant focused checks without automatically invoking a fresh
reviewer. This keeps the independent pass at meaningful decision points.

Location: `.agents/skills/<skill-name>/SKILL.md` inside this
repository. Automatic selection is enabled by default. Example explicit invocation:
`$loandesk-borrower-ownership-review Review my borrower changes.`

### Implemented personal Git hooks

- **Pre-commit:** inspect staged changes for whitespace errors and conflict
  markers; reject staged `data/` files; warn when shared or other-role files
  are included. Scope warnings should not block necessary coordinated changes.
- **Pre-push:** run `.\gradlew.bat clean test --no-daemon` on Windows and stop
  the push if tests fail.

Scripts live in `tools/borrower/hooks/`, with LF line endings controlled by a
local `.gitattributes`. Activation uses this checkout's `core.hooksPath`; it does
not configure teammates' clones. Existing hook configuration must be inspected
before activation. Scripts do not stage, rewrite, or delete local data. Pre-push
does clean generated build output through Gradle. Pre-commit examines the index,
including force-added data and partially staged files; scope warnings do not block.

`tools/borrower/test_hooks.py` exercises hooks in disposable repositories inside
ignored `build/`. It checks clean and empty indexes, data blocking without content
disclosure, conflict markers, whitespace, scope warnings, partial staging and
pre-push argument/exit-status propagation. The pre-push test uses a stub wrapper;
real Gradle verification is recorded separately in the session log.

Skill format validation does not prove review quality. The first controlled
ownership evaluation now has two isolated cases, shared executable contract
tests and one fresh reviewer run. The reviewer identified the seeded defect
and reported no unsupported defect in the correct case. See the
[walkthrough](../tools/borrower/skill-evaluations/ownership/README.md) and
[evaluation record](../logs/Yikbing-logs/2026-09-21-ownership-skill-evaluation.md).
This is one unit-level evaluation with explicit skill invocation; automatic
selection, integration/system evaluation, other skills and detailed human
reflections remain unfinished. The skills do not authorize unrelated edits
or claim exhaustive coverage.

JUnit does not launch an agent or invoke these skills. It tests the synthetic
Java services. The fresh agent review is a separate, explicitly invoked step,
and its findings are compared against the withheld answer sheet. An automated
agent evaluation runner has not been implemented.

Defer commit-message enforcement and automatic formatting. No formatter was
configured in `build.gradle` when this recommendation was recorded. The
broader team proposals below remain future options, not activated requirements.

### Online imports worth considering later

- [gh-fix-ci](https://github.com/openai/skills/tree/main/skills/.curated/gh-fix-ci):
  investigate failing GitHub Actions checks on borrower pull requests.
- [gh-address-comments](https://github.com/openai/skills/tree/main/skills/.curated/gh-address-comments):
  collect teammate review comments and implement selected fixes.

Both require authenticated GitHub CLI access. Recheck their instructions and
dependencies before importing. `skill-creator` and `skill-installer` were
already available in the planning session; no import was needed for them.

The inspected `playwright` skill targets browser automation, not JavaFX.
The inspected `security-best-practices` skill targets Python,
JavaScript/TypeScript, and Go, so it is not a Java-specific review choice.

References checked during planning:

- [Official skill format, invocation, and repository locations](https://learn.chatgpt.com/docs/build-skills)
- [Git hook behaviour](https://git-scm.com/docs/githooks)
- [OpenAI curated skill catalogue](https://github.com/openai/skills/tree/main/skills/.curated)

## Skill format

Every skill should specify its trigger, required inputs, checks, output, and
verification evidence. A skill evaluation must use a controlled fixture or
isolated test change. Do not leave intentional defects in the release branch.

## Broader team skill proposals (not implemented by this setup)

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

## Broader team automation proposals (not activated by this setup)

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
