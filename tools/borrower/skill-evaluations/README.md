# Borrower skill evaluations

This directory contains controlled evaluation material for the five borrower
review skills. `tools/borrower/test_skill_contracts.py` checks that every skill
has a valid frontmatter contract, a documented trigger and a controlled
evaluation case.

Run the repository-side checks from the repository root with:

```text
python tools/borrower/test_skill_contracts.py
```

These tests validate the evaluation protocol. They do not prove that Codex's
external automatic-selection model will select a skill for a natural-language
prompt. Automatic skill selection must be tested separately by giving a fresh agent
a realistic prompt and recording whether the expected skill was selected.
JUnit likewise does not invoke skills or agents.

The controlled cases are synthetic and must not be treated as production
policy. Preserve the prompt, original response, findings, verification and
limitations for each actual agent evaluation under `logs/Yikbing-logs/`.

The existing ownership fixture remains under
`tools/borrower/skill-evaluations/ownership/` and has its own Gradle test
command and evaluation record.
