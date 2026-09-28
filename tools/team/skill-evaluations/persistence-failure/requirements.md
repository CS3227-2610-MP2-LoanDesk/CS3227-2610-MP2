# Persistence-failure exercise requirements

This is a synthetic, in-memory storage exercise, not a production model and
not an agreement about LoanDesk's eventual storage policy.

Review each supplied case independently against this contract:

1. Recording a damaged return closes the loan and marks its equipment
   damaged. Both changes belong to one operation.
2. The Sink writes both record sets together, or throws and writes neither.
   A thrown IOException means nothing was persisted.
3. When the write fails, the service's own in-memory state must be left
   exactly as it was before the call. A caller that catches the exception and
   reads the snapshots must not observe a partially applied return.
4. An unknown loan, an already-returned loan, or a loan whose equipment is
   missing is refused by returning false, without calling the Sink and
   without mutating anything.
5. A successful operation changes only the targeted loan and its equipment.
6. The IOException from a failed write propagates to the caller rather than
   being swallowed and reported as success.

Assume non-null IDs and well-formed seeded records. Records are immutable and
one thread calls the service. The snapshot methods are test observation
seams. There is no UI, restart, schema migration, concurrency or credential
handling in this exercise. Do not invent requirements for those excluded
concerns.
