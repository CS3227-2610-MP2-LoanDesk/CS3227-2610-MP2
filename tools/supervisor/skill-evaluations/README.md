# Supervisor skill evaluations

This directory contains controlled evaluation material for the supervisor
review skill. `tools/supervisor/test_skill_contracts.py` checks that every
supervisor skill has a valid frontmatter contract, a documented trigger and a
controlled evaluation case.

Run the repository-side checks from the repository root with:

```text
python tools/supervisor/test_skill_contracts.py
```

Run the controlled fixture pair with:

```text
.\gradlew.bat -p tools/supervisor/skill-evaluations/permission-workflow verifyFixtures --no-daemon
```

These tests validate the evaluation protocol. They do not prove that an
agent's external automatic skill selection model will select a skill for a
natural-language prompt. Automatic skill selection must be tested separately by
giving a fresh agent a realistic prompt and recording whether the expected
skill was selected. JUnit likewise does not invoke skills or agents.

The controlled cases are synthetic and must not be treated as production
policy. Preserve the prompt, original response, findings, verification and
limitations for each actual agent evaluation under `logs/Supervisor-logs/`.

The borrower skills and their evaluations remain under `tools/borrower/` and
are checked by a separate contract test.
