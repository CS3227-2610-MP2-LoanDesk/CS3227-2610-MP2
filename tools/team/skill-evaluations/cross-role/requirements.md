# Cross-role journey exercise requirements

This is a synthetic, in-memory journey exercise, not a production model and
not an agreement about LoanDesk's eventual fulfilment policy.

Review each supplied case independently against this contract:

1. A trusted application boundary supplies Session; callers cannot impersonate
   another role. Session is null when logged out.
2. Only a SUPERVISOR may approve. Only a CUSTODIAN may check out or record a
   return.
3. Only a PENDING request may be approved. Only an APPROVED request may be
   checked out. Only an ACTIVE loan may be returned.
4. Checkout creates a loan for the request and leaves the two records
   agreeing: the loan references its request, and the request becomes
   COLLECTED and references the loan it produced. Neither record may be left
   describing a state the other contradicts.
5. The loan and its request must agree on borrower and equipment.
6. A refused operation changes nothing: no record is mutated and no loan is
   created. Approval and checkout return false or null respectively on refusal.
7. Recording a return closes only that loan.

Assume non-null IDs and well-formed seeded records. Records are immutable and
one thread calls the service. The snapshot methods are test observation seams,
not caller-facing endpoints. There is no UI, disk storage, expiry,
availability calculation or concurrency in this exercise. Do not invent
requirements for those excluded concerns.
