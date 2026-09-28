package evaluation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import evaluation.JourneyService.LoanStatus;
import evaluation.JourneyService.Request;
import evaluation.JourneyService.RequestStatus;
import evaluation.JourneyService.Role;
import evaluation.JourneyService.Session;

/** Shared contract tests run against both fixture cases. */
class JourneyContractTest {
    private static final Session BORROWER = new Session("borrower-a", Role.BORROWER);
    private static final Session SUPERVISOR = new Session("supervisor-a", Role.SUPERVISOR);
    private static final Session CUSTODIAN = new Session("custodian-a", Role.CUSTODIAN);

    private static Map<String, Request> seed() {
        return Map.of(
                "request-1",
                new Request("request-1", "borrower-a", "camera1", RequestStatus.PENDING, null),
                "request-2",
                new Request("request-2", "borrower-b", "camera2", RequestStatus.PENDING, null));
    }

    private static JourneyService service() {
        return new JourneyService(seed());
    }

    private static JourneyService approvedService() {
        JourneyService service = service();
        service.approve(SUPERVISOR, "request-1");
        return service;
    }

    @Test
    void approvalMovesAPendingRequestToApproved() {
        JourneyService service = service();

        assertTrue(service.approve(SUPERVISOR, "request-1"));

        assertAll(
                () -> assertEquals(RequestStatus.APPROVED,
                        service.requestSnapshot().get("request-1").status()),
                () -> assertEquals(seed().get("request-2"),
                        service.requestSnapshot().get("request-2")),
                () -> assertTrue(service.loanSnapshot().isEmpty()));
    }

    @Test
    void approvalRequiresASupervisorSession() {
        JourneyService service = service();

        assertAll(
                () -> assertFalse(service.approve(BORROWER, "request-1")),
                () -> assertFalse(service.approve(CUSTODIAN, "request-1")),
                () -> assertFalse(service.approve(null, "request-1")),
                () -> assertEquals(seed(), service.requestSnapshot()));
    }

    @Test
    void checkoutByACustodianCreatesALoanForTheRequest() {
        JourneyService service = approvedService();

        String loanId = service.checkout(CUSTODIAN, "request-1");

        assertNotNull(loanId);
        var loan = service.loanSnapshot().get(loanId);
        assertAll(
                () -> assertEquals("request-1", loan.requestId()),
                () -> assertEquals("borrower-a", loan.borrowerId()),
                () -> assertEquals("camera1", loan.equipmentId()),
                () -> assertEquals(LoanStatus.ACTIVE, loan.status()));
    }

    @Test
    void checkoutLeavesTheRequestAndItsLoanAgreeing() {
        JourneyService service = approvedService();

        String loanId = service.checkout(CUSTODIAN, "request-1");

        var request = service.requestSnapshot().get("request-1");
        assertAll(
                () -> assertEquals(RequestStatus.COLLECTED, request.status()),
                () -> assertEquals(loanId, request.loanId()));
    }

    @Test
    void checkoutRequiresAnApprovedRequest() {
        JourneyService service = service();

        assertAll(
                () -> assertNull(service.checkout(CUSTODIAN, "request-1")),
                () -> assertNull(service.checkout(CUSTODIAN, "request-missing")),
                () -> assertEquals(seed(), service.requestSnapshot()),
                () -> assertTrue(service.loanSnapshot().isEmpty()));
    }

    @Test
    void checkoutRequiresACustodianSession() {
        JourneyService service = approvedService();

        assertAll(
                () -> assertNull(service.checkout(BORROWER, "request-1")),
                () -> assertNull(service.checkout(SUPERVISOR, "request-1")),
                () -> assertNull(service.checkout(null, "request-1")),
                () -> assertTrue(service.loanSnapshot().isEmpty()));
    }

    @Test
    void recordingAReturnClosesOnlyThatLoan() {
        JourneyService service = approvedService();
        String loanId = service.checkout(CUSTODIAN, "request-1");

        assertTrue(service.recordReturn(CUSTODIAN, loanId));

        assertAll(
                () -> assertEquals(LoanStatus.RETURNED,
                        service.loanSnapshot().get(loanId).status()),
                () -> assertEquals(1, service.loanSnapshot().size()));
    }

    @Test
    void aReturnRequiresAnActiveLoanAndACustodian() {
        JourneyService service = approvedService();
        String loanId = service.checkout(CUSTODIAN, "request-1");

        assertAll(
                () -> assertFalse(service.recordReturn(BORROWER, loanId)),
                () -> assertFalse(service.recordReturn(CUSTODIAN, "loan-missing")),
                () -> assertTrue(service.recordReturn(CUSTODIAN, loanId)),
                () -> assertFalse(service.recordReturn(CUSTODIAN, loanId)));
    }
}
