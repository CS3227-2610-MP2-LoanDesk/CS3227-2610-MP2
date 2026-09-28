# 2026-09-28 Permission/workflow skill evaluation

First controlled evaluation of `loandesk-supervisor-permission-workflow-review`
against the `tools/supervisor/skill-evaluations/permission-workflow` fixture
pair.

## Protocol followed

A fresh agent with no inherited conversation was given the four blinded files
named in `review-prompt.txt`: the skill file, `requirements.md`, and the two
`DecisionService.java` cases. It was not given the tests, the build scripts,
`expected-results.md`, prior findings or project logs. The answer sheet was
read only after the response was preserved.

The reviewer was invoked explicitly. This evaluates review ability, not
automatic skill selection.

## Exact prompt

> Repository: `D:\JS\CS3227-2610-MP2`
>
> You are performing an independent, read-only code review. Read exactly these
> four files and no others:
>
> 1. `.agents\skills\loandesk-supervisor-permission-workflow-review\SKILL.md`
> 2. `tools\supervisor\skill-evaluations\permission-workflow\requirements.md`
> 3. `tools\supervisor\skill-evaluations\permission-workflow\case-a\src\main\java\evaluation\DecisionService.java`
> 4. `tools\supervisor\skill-evaluations\permission-workflow\case-b\src\main\java\evaluation\DecisionService.java`
>
> File 1 is a review skill: follow its instructions as your review method.
>
> Your task, per that skill:
>
> Use the loandesk-supervisor-permission-workflow-review skill to review the
> supplied case-a and case-b independently against requirements.md. This is an
> isolated fixture, not a change to production contracts. Report findings
> without editing files.
>
> Read only the supplied skill, requirements.md and the two DecisionService.java
> files. Do not read tests, build scripts, answer sheets, prior results, project
> logs or conversation history. The supplied requirements provide the complete
> context for this exercise; broader LoanDesk policies are not needed here.
>
> Provide the operation/permission/source-status/target-status table and
> supported findings with file locations, direct-call scenarios, expected versus
> observed behaviour and missing tests. Say explicitly whether any code was
> executed. Do not assume either case has a defect. Return your review without
> fixing anything.
>
> Hard constraints: do not edit, create or delete any file. Do not run Gradle,
> JUnit, or any build or test command. Do not search the repository beyond the
> four listed files. Report case A and case B separately, and state clearly for
> each whether you found a contract defect or none.

## Decision-relevant response

> **Execution disclosure:** No code was compiled, run, or executed. No
> Gradle/JUnit command was invoked. This is a static read of the four listed
> files only.

> **Case A — Verdict: contract defect found**, isolated to
> `approve(Session, String)` (lines 18–29).
>
> **Missing check:** unlike `reject()` in the same file (line 39:
> `request.status() != Status.PENDING`), `approve()` never checks
> `request.status()`. Any request that exists — regardless of current status —
> is unconditionally overwritten to `APPROVED`.
>
> This violates Req. 3 ... `approve()` will re-decide an already-`APPROVED`,
> `REJECTED`, or `COLLECTED` request. Req. 7 ... An unknown ID returns `false`,
> but a terminal-status ID returns `true` and mutates state.

> Direct-call reproduction: seed `R1` as `REJECTED` with
> `decidedBy="sup1"`, call `approve(supervisor, "R1")`.
> Expected `false` and an unchanged record; observed `true`, `R1` becomes
> `APPROVED` with `decidedBy="sup2"`, and the original decision metadata is
> erased.

> `reject()` in Case A (lines 31–45) was checked against the same contract and
> is correct: role gate, blank-reason gate, then
> `request == null || request.status() != Status.PENDING` gate, all before any
> mutation — no defect found there.

> **Case B — Verdict: no contract defect found.** ... every `false`-returning
> branch precedes the `requests.put(...)` call in both methods, so a rejected
> call leaves the entire map unchanged. ... I found no evidence supporting a
> defect claim for Case B.

## Assessment against the withheld answer sheet

- **True positive: yes.** The root defect, the missing source-status check in
  `approve`, was identified with its file, its line range and a direct-call
  scenario re-approving a terminal request. The consequences were presented as
  symptoms of one defect rather than counted as separate defects.
- **False negatives: none.**
- **False positives: none.** The answer sheet's specific trap is a reviewer who
  condemns the whole service. The reviewer explicitly checked `reject` in case A
  and reported it correct, which is the discriminating behaviour the fixture was
  built to test.
- **Correct negative: yes.** Case B was reported clean, with each requirement
  checked rather than waved through, and no fabricated concern about the
  excluded session boundary, snapshot seam, persistence or concurrency.
- **Quality: met.** The response separated the authorization gate from the
  transition gate in its table, disclosed that nothing was executed, and
  proposed a focused test seeding each terminal status and asserting `false`
  with an unchanged snapshot.

Score: one defect planted, one detected, zero missed, zero unsupported.

## Observations and limitations

- The reviewer reported that its `Skill` tool did not recognize
  `loandesk-supervisor-permission-workflow-review` as a registered skill, so it
  followed the SKILL.md text as supplied instructions instead. The project
  stores skills under `.agents/skills/`, which is the Codex convention; a
  different agent runtime may not load that directory. This does not affect the
  result, because the protocol supplies the skill explicitly, but it is direct
  evidence that automatic selection cannot be assumed from the file's presence
  alone. The same caveat applies to the five borrower skills in that directory.
- The reviewer noted it was inferring missing tests from the production code
  path rather than reading the test suite, which is the intended blinding.
- This is one unit-level evaluation of one planted defect. It does not
  establish that the skill will detect permission or lifecycle defects
  generally, and it does not test automatic skill selection.
- No rerun and no correction to the skill were needed; the skill text was not
  modified after this evaluation.
