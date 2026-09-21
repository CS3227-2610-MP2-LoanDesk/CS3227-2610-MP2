Case A has one supported authorization defect: a borrower can cancel another borrower’s pending request. Case B has no identified defect against the supplied contract. These conclusions are from static inspection; no code was executed.

Only the four authorized files were read. The supplied requirements replaced broader project-context reading, and the read-only restriction superseded the skill’s logging instruction. No files were edited.

For evidence below, [Case A](C:/Users/yikbi/NUS/CS3227/MP2/CS3227-2610-MP2/tools/borrower/skill-evaluations/ownership/case-a/src/main/java/evaluation/CancellationService.java) and [Case B](C:/Users/yikbi/NUS/CS3227/MP2/CS3227-2610-MP2/tools/borrower/skill-evaluations/ownership/case-b/src/main/java/evaluation/CancellationService.java) are reviewed independently against [requirements.md](C:/Users/yikbi/NUS/CS3227/MP2/CS3227-2610-MP2/tools/borrower/skill-evaluations/ownership/requirements.md:6).

| Operation | Role/session | Owner and stored state | Required behavior | Case A evidence and inferred behavior | Case B evidence and inferred behavior |
|---|---|---|---|---|---|
| `cancel` | Logged out | Any | `false`; entire state unchanged | Lines 19–20 reject before lookup or mutation | Lines 19–20 reject before lookup or mutation |
| `cancel` | `STAFF` | Any | `false`; entire state unchanged | Lines 19–20 reject before mutation | Lines 19–20 reject before mutation |
| `cancel` | `BORROWER` | Own `PENDING` request | `true`; only selected request becomes `CANCELLED` | Lines 22–27 implement this transition | Lines 22–28 check session ownership and implement this transition |
| `cancel` | `BORROWER` | Foreign `PENDING` request | `false`; entire state unchanged | **Lines 23–27 permit cancellation without checking ownership** | Lines 23–25 reject owner mismatch before mutation |
| `cancel` | `BORROWER` | Own or foreign `CANCELLED` request | `false`; entire state unchanged | Lines 23–24 reject non-pending state | Lines 23–25 reject foreign ownership or non-pending state |
| `cancel` | `BORROWER` | Unknown ID | `false`; entire state unchanged; same result as foreign ID | Lines 23–24 return `false`; differs from foreign pending case | Lines 23–25 return `false`, matching foreign-ID rejection |

**Finding: [P1] Case A permits cancellation of another borrower’s request.**

Location: [`CancellationService.cancel`, line 23](C:/Users/yikbi/NUS/CS3227/MP2/CS3227-2610-MP2/tools/borrower/skill-evaluations/ownership/case-a/src/main/java/evaluation/CancellationService.java:23), with unauthorized mutation at line 26.

The method checks the session’s role, request existence, and pending state, but never compares `request.ownerId()` with `session.userId()`. Any logged-in borrower can therefore cancel a foreign pending request by supplying its ID. This violates requirements 3 and 5. Returning `true` for that foreign ID while returning `false` for an unknown ID also violates requirement 6. These are consequences of the same missing ownership check, rather than separate findings.

Direct-call reproduction, using synthetic records:

```java
var service = new CancellationService(Map.of(
    "r-bob", new Request("r-bob", "bob", Status.PENDING),
    "r-alice", new Request("r-alice", "alice", Status.PENDING)
));
var before = service.snapshot();
boolean result = service.cancel(
    new Session("alice", Role.BORROWER), "r-bob"
);
var after = service.snapshot();
```

Expected: `result == false` and `after.equals(before)`.

Inferred from Case A: `result == true`; `r-bob` becomes `CANCELLED`, retaining Bob as its owner. `r-alice` remains unchanged. Calling `cancel` with the same session and an unknown ID returns `false`, demonstrating the inconsistent foreign/unknown result. This scenario does not require impersonation or an untrusted session: Alice uses her own trusted session.

**Case B: no supported findings.**

Case B checks ownership against the trusted session at lines 23–24, immediately before its sole mutation at line 27. Every rejection exits before that mutation. Successful cancellation replaces only the selected map entry, preserves its ID and owner, and changes its status to `CANCELLED`. Subsequent cancellation encounters the new state and returns `false`.

For an independent direct-call check, instantiate Case B with the same synthetic seed above:

| Call sequence | Expected behavior | Behavior inferred from Case B |
|---|---|---|
| Alice cancels `r-bob` | `false`; whole snapshot unchanged | Matches: owner mismatch rejects |
| Alice cancels unknown ID | `false`; whole snapshot unchanged | Matches: missing request rejects |
| Alice cancels `r-alice` | `true`; only `r-alice.status` changes | Matches: selected entry replaced |
| Alice cancels `r-alice` again | `false`; post-success snapshot unchanged | Matches: `CANCELLED` state rejects |
| Logged-out caller cancels pending request | `false`; whole snapshot unchanged | Matches: null-session guard rejects |
| Staff caller cancels pending request | `false`; whole snapshot unchanged | Matches: role guard rejects |

Suggested tests for each case independently:

- A borrower cancelling a foreign pending request returns `false` and preserves the **entire** snapshot. This should expose Case A’s defect.
- Foreign pending and unknown IDs both return `false`, with unchanged state after each call.
- An owner cancelling a pending request succeeds, preserves ID and owner, and leaves all other entries unchanged.
- Repeated cancellation returns `false` and preserves the snapshot taken after the first successful cancellation.
- Own and foreign pre-cancelled records both reject without mutation.
- Logged-out and staff sessions reject cancellation of pending records without mutation.

Existing test coverage is unknown because tests were deliberately not inspected. No runtime results, build status, UI behavior, persistence guarantees, concurrency behavior, or broader project permission coverage are claimed. The trusted seed, immutable records, trusted session boundary, and single-threaded in-memory scope are accepted as specified; the snapshot seam is not treated as a borrower-facing access defect.
