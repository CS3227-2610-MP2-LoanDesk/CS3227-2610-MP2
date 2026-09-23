package evaluation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static evaluation.CancellationService.*;
import static org.junit.jupiter.api.Assertions.*;

class CancellationContractTest {
    private CancellationService service;
    private final Session borrowerA = new Session("borrower-a", Role.BORROWER);

    @BeforeEach
    void seed() {
        service = new CancellationService(Map.of(
                "request-a", new Request("request-a", "borrower-a", Status.PENDING),
                "request-b", new Request("request-b", "borrower-b", Status.PENDING),
                "cancelled-a", new Request("cancelled-a", "borrower-a", Status.CANCELLED)));
    }

    @Test
    void ownerCanCancelOnlySelectedRequest() {
        var before = service.snapshot();
        assertTrue(service.cancel(borrowerA, "request-a"));
        assertEquals(Map.of(
                "request-a", new Request("request-a", "borrower-a", Status.CANCELLED),
                "request-b", before.get("request-b"),
                "cancelled-a", before.get("cancelled-a")), service.snapshot());
    }

    @Test
    void foreignOwnerIsRejectedWithoutMutation() {
        rejectedWithoutMutation(borrowerA, "request-b");
    }

    @Test
    void loggedOutIsRejectedWithoutMutation() {
        rejectedWithoutMutation(null, "request-a");
    }

    @Test
    void wrongRoleIsRejectedWithoutMutation() {
        rejectedWithoutMutation(new Session("borrower-a", Role.STAFF), "request-a");
    }

    @Test
    void unknownRequestIsRejectedWithoutMutation() {
        rejectedWithoutMutation(borrowerA, "unknown");
    }

    @Test
    void cancelledRequestIsRejectedWithoutMutation() {
        rejectedWithoutMutation(borrowerA, "cancelled-a");
    }

    @Test
    void repeatedCancellationIsRejectedWithoutMutation() {
        assertTrue(service.cancel(borrowerA, "request-a"));
        rejectedWithoutMutation(borrowerA, "request-a");
    }

    private void rejectedWithoutMutation(Session session, String id) {
        var before = service.snapshot();
        boolean result = service.cancel(session, id);
        assertAll(
                () -> assertFalse(result, "rejected action must return false"),
                () -> assertEquals(before, service.snapshot(), "rejected action must preserve all records"));
    }
}
