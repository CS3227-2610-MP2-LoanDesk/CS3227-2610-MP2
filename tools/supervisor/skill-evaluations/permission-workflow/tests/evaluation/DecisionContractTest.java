package evaluation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import evaluation.DecisionService.Request;
import evaluation.DecisionService.Role;
import evaluation.DecisionService.Session;
import evaluation.DecisionService.Status;

/** Shared contract tests run against both fixture cases. */
class DecisionContractTest {
    private static final Session SUPERVISOR = new Session("supervisor-a", Role.SUPERVISOR);
    private static final Session BORROWER = new Session("borrower-a", Role.BORROWER);

    private static Map<String, Request> seed() {
        return Map.of(
                "request-pending", new Request("request-pending", Status.PENDING, null, null),
                "request-approved",
                new Request("request-approved", Status.APPROVED, "supervisor-a", null),
                "request-collected",
                new Request("request-collected", Status.COLLECTED, "supervisor-a", null));
    }

    private static DecisionService service() {
        return new DecisionService(seed());
    }

    @Test
    void supervisorApprovesAPendingRequest() {
        DecisionService service = service();

        assertTrue(service.approve(SUPERVISOR, "request-pending"));

        Request decided = service.snapshot().get("request-pending");
        assertAll(
                () -> assertEquals(Status.APPROVED, decided.status()),
                () -> assertEquals("supervisor-a", decided.decidedBy()),
                () -> assertEquals(
                        seed().get("request-approved"),
                        service.snapshot().get("request-approved")),
                () -> assertEquals(
                        seed().get("request-collected"),
                        service.snapshot().get("request-collected")));
    }

    @Test
    void supervisorRejectsAPendingRequestWithAReason() {
        DecisionService service = service();

        assertTrue(service.reject(SUPERVISOR, "request-pending", "Item reserved for a class"));

        Request decided = service.snapshot().get("request-pending");
        assertAll(
                () -> assertEquals(Status.REJECTED, decided.status()),
                () -> assertEquals("supervisor-a", decided.decidedBy()),
                () -> assertEquals("Item reserved for a class", decided.reason()));
    }

    @Test
    void rejectionWithoutAReasonIsRefusedWithoutMutation() {
        DecisionService service = service();

        assertAll(
                () -> assertFalse(service.reject(SUPERVISOR, "request-pending", null)),
                () -> assertFalse(service.reject(SUPERVISOR, "request-pending", "   ")),
                () -> assertEquals(seed(), service.snapshot()));
    }

    @Test
    void loggedOutCallerIsRefusedWithoutMutation() {
        DecisionService service = service();

        assertAll(
                () -> assertFalse(service.approve(null, "request-pending")),
                () -> assertFalse(service.reject(null, "request-pending", "No")),
                () -> assertEquals(seed(), service.snapshot()));
    }

    @Test
    void borrowerCallerIsRefusedWithoutMutation() {
        DecisionService service = service();

        assertAll(
                () -> assertFalse(service.approve(BORROWER, "request-pending")),
                () -> assertFalse(service.reject(BORROWER, "request-pending", "No")),
                () -> assertEquals(seed(), service.snapshot()));
    }

    @Test
    void unknownRequestIsRefusedWithoutMutation() {
        DecisionService service = service();

        assertAll(
                () -> assertFalse(service.approve(SUPERVISOR, "request-missing")),
                () -> assertFalse(service.reject(SUPERVISOR, "request-missing", "No")),
                () -> assertEquals(seed(), service.snapshot()));
    }

    @Test
    void alreadyDecidedRequestIsNotApprovedAgainWithoutMutation() {
        DecisionService service = service();

        assertAll(
                () -> assertFalse(service.approve(SUPERVISOR, "request-approved")),
                () -> assertFalse(service.approve(SUPERVISOR, "request-collected")),
                () -> assertEquals(seed(), service.snapshot()));
    }

    @Test
    void decidedRequestIsNotRejectedAgainWithoutMutation() {
        DecisionService service = service();

        assertAll(
                () -> assertFalse(service.reject(SUPERVISOR, "request-approved", "Changed")),
                () -> assertFalse(service.reject(SUPERVISOR, "request-collected", "Changed")),
                () -> assertEquals(seed(), service.snapshot()));
    }
}
