# Team-level skill evaluations

Controlled evaluation material for the four team-level review skills: the
cross-role scenario reviewer, the persistence-failure test reviewer, the
release readiness reviewer and the documentation consistency reviewer.

`tools/team/test_skill_contracts.py` checks that every one of them has a valid
frontmatter contract, a documented trigger, a controlled case, a blinded
reviewer prompt, a withheld answer sheet naming a false-positive trap, and, for
the executable fixtures, exactly one seeded defect matched by exactly one test.

Run the repository-side checks from the repository root with:

```text
python tools/team/test_skill_contracts.py
```

Run the two executable fixture pairs with:

```text
.\gradlew.bat -p tools/team/skill-evaluations/cross-role verifyFixtures --no-daemon
.\gradlew.bat -p tools/team/skill-evaluations/persistence-failure verifyFixtures --no-daemon
```

## Two kinds of fixture

`cross-role` and `persistence-failure` are **executable**: a defective and a
correct Java implementation share one JUnit suite, and `verifyFixtures` asserts
that case A fails exactly the one test that detects the seeded defect while
case B passes everything. The reviewer's judgement is scored against that
independent ground truth.

`release-readiness` and `documentation-consistency` are **descriptive**: the
candidates are synthetic documents about a fictional product, so nothing can be
run and the answer sheet is the only ground truth. Their results are weaker
evidence than the executable pairs and should be cited as such.

## What these checks do and do not prove

These tests validate the evaluation protocol. They do not prove that an
agent's external automatic skill selection model will select a skill for a
natural-language prompt. Automatic skill selection must be tested separately by
giving a fresh agent a realistic prompt and recording whether the expected
skill was selected. JUnit likewise does not invoke skills or agents.

The controlled cases are synthetic and must not be treated as production
policy. Preserve the prompt, original response, findings, verification and
limitations for each actual agent evaluation under `logs/`.

## Ownership

`cross-role` spans all three roles and belongs to the team. `release-readiness`
and `documentation-consistency` support the delivery-coordination
responsibilities. `persistence-failure` corresponds to the custodian owner's
agent skill in the team workload plan and was drafted here so it exists; its
owner should review it and take it over rather than inherit it unexamined.

The borrower skills and their evaluations remain under `tools/borrower/`, and
the supervisor skill under `tools/supervisor/`, each with its own contract test.
