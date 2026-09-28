# Permission and workflow review exercise requirements

This is a synthetic, in-memory decision exercise, not a production model and
not an agreement about LoanDesk's eventual supervisor policy.

Review each supplied case independently against this contract:

1. A trusted application boundary supplies Session; callers cannot impersonate
   another user or role by supplying an actor field. Session is null when
   logged out.
2. Only a logged-in SUPERVISOR may call approve or reject successfully.
3. Only a PENDING request may become APPROVED or REJECTED. APPROVED, REJECTED
   and COLLECTED are terminal and may not be decided again.
4. Rejection requires a non-blank reason. Approval does not require one.
5. A successful decision records the deciding session's userId in decidedBy,
   and a successful rejection records its reason.
6. Return true on success and false on rejection. All rejections preserve the
   entire stored state; only the selected request changes on success.
7. Unknown request IDs and requests in a terminal status receive the same
   false result.

Assume non-null IDs and well-formed seeded records. Request and Session are
immutable. One thread calls the service. The constructor seed is trusted; the
snapshot method is a test observation seam, not a caller-facing endpoint.
There is no UI, disk storage, network, expiry sweep, availability check or
borrower eligibility check in this exercise. Do not invent requirements for
those excluded concerns.
