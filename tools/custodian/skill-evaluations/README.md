# Custodian skill evaluations

This directory contains controlled, synthetic evaluation material for the two
custodian review skills. Run the repository-side checks from the repository
root:

```text
python3 tools/custodian/test_skill_contracts.py
```

The fixture pairs are deliberately defective/correct source examples. They
verify the documented evaluation protocol but do not prove automatic skill
selection or a reviewer's reasoning. Evaluate each skill separately with a
fresh agent, the relevant skill, requirements, source pair and review prompt;
do not reveal the answer sheet first. Record the outcome as `RUN`,
`UNAVAILABLE`, or `INCONCLUSIVE` under `logs/`.

