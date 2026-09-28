# Documentation consistency exercise requirements

This is a synthetic documentation candidate, not a real product and not a
statement about any actual LoanDesk guide.

Review each supplied candidate independently against this contract:

1. Every user guide statement a reader could act on must match the code that
   decides it. A contradiction is a defect, not a wording preference.
2. A field the guide calls required must be rejected when absent, and a field
   it calls optional must be accepted when absent.
3. Named values in the guide, such as limits and windows, must match the
   constants and validation in the code.
4. A guide statement that a feature is unavailable is accurate when the
   feature is genuinely absent from the code, and is not a defect.
5. Behaviour present in the code but absent from the guide is a gap, reported
   separately from contradictions.

Assume the supplied excerpts are complete and representative: the code shown
is the code that decides the documented behaviour, and the stated repository
state is accurate. There is no access to the real repository. Do not invent
requirements for tone, formatting, screenshots, accessibility or translation;
they are outside this exercise.
