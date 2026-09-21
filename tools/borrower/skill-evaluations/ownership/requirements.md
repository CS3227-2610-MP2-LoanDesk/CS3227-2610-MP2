# Ownership review exercise requirements

This is a synthetic, in-memory cancellation exercise, not a production model
or an agreement about LoanDesk's eventual cancellation policy.

Review each supplied case independently against this contract:

1. A trusted application boundary supplies Session; callers cannot impersonate
   a different user by supplying an owner ID. Session is null when logged out.
2. Only a logged-in BORROWER may call cancel successfully.
3. The request must exist and belong to the session's userId.
4. Only PENDING requests may become CANCELLED. A repeated cancellation is rejected.
5. Return true on success and false on rejection. All rejections preserve the
   entire stored state; only the selected request changes on success.
6. Unknown and foreign request IDs receive the same false result.

Assume non-null IDs and well-formed seeded records. Request and Session are
immutable. One thread calls the service. The constructor seed is trusted; the
snapshot method is a test observation seam, not a borrower-facing endpoint.
There is no UI, disk storage, network or concurrent access in this exercise.
Do not invent requirements for those excluded concerns.
