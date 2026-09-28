# Documentation candidate A — ShelfDesk

Everything below is the complete supplied evidence for this candidate.

## `docs/UserGuide.md` excerpt

> ## Approving a reservation
>
> Select `Approve` on a pending reservation. You must enter a justification
> note before the approval is accepted; approvals without a note are rejected.
>
> To decline a reservation instead, select `Decline` and enter a reason. A
> reason is required so the member can see why.
>
> Reservations can be approved up to 30 days ahead.
>
> Shelf maintenance reporting is not available in this release.

## `src/main/java/shelfdesk/application/ReservationDecisionService.java` excerpt

```java
public Reservation approve(String reservationId, String note) throws IOException {
    String approver = permissions.require(Permission.APPROVE_RESERVATION).username();
    String trimmedNote = note == null || note.isBlank() ? null : note.trim();
    return decide(reservationId, current ->
            decided(current, Status.APPROVED, approver, trimmedNote));
}

public Reservation decline(String reservationId, String reason) throws IOException {
    String approver = permissions.require(Permission.DECLINE_RESERVATION).username();
    if (reason == null || reason.isBlank()) {
        throw new IllegalArgumentException("Decline reason must not be blank.");
    }
    return decide(reservationId, current ->
            decided(current, Status.DECLINED, approver, reason.trim()));
}
```

## `src/main/java/shelfdesk/application/ReservationRules.java` excerpt

```java
public final class ReservationRules {
    public static final int MAX_DAYS_AHEAD = 30;

    public static void requireStartWithinWindow(LocalDate start, LocalDate today) {
        if (start.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException(
                    "Reservations cannot start more than 30 days ahead.");
        }
    }
}
```

## Repository state

There is no maintenance screen, service or model anywhere in `src/`.
