---
name: loandesk-custodian-fulfilment-workflow-review
description: Review LoanDesk custodian checkout, return, lost/recovery, collection-window, request/loan transition, or custodian-permission changes. Check service authorization, legal transitions, collection-window enforcement, duplicate checkout prevention, and reciprocal request/loan links.
---

# Custodian fulfilment workflow review

Read the applicable approved custodian requirements, documented workflow, and
affected shared services before reviewing. Treat the agreed product policy and
shared contracts as authoritative, and flag conflicts or unsettled policy for
team resolution. Trace each affected screen action through its application
service, lifecycle and availability services, then persistence. Session identity
is authoritative; caller-provided roles or usernames never authorize an
operation.

For checkout, return, lost and recovery operations, verify the service checks
the relevant custodian permission before mutation. Check source and target
states against the agreed lifecycle, and confirm that collection date rules are
provided by one shared predicate rather than reimplemented in the UI or each
operation. A checkout must reject a non-approved, out-of-window, unavailable,
or non-GOOD request/equipment pairing and cannot create a second loan.

For every changed operation, report a table with operation, permission, source
state, target state, and evidence. Then report findings with severity,
file/symbol, a direct service-call reproduction that bypasses the UI, and a
missing or inadequate test. Say separately what was inspected and what was
executed. Rejected operations and failed saves must leave no partial request,
loan, or equipment mutation; collected requests and their loans must link to
each other exactly once.

Use synthetic data and temporary H2 databases in tests. Do not change product
policy, duplicate authorization in JavaFX, or modify another role's feature
area without coordination. If the policy is unsettled, report it as a question.
