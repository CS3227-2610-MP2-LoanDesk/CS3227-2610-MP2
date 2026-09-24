package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.BorrowerEligibility;
import loandesk.application.BorrowerEligibilityService;
import loandesk.application.EligibilityBlocker;
import loandesk.application.Session;
import loandesk.domain.Equipment;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class BorrowerEligibilityServiceTest {
    @TempDir
    Path temporaryDirectory;

    private static final Instant NOW = Instant.parse("2026-09-25T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void eligibleBorrowerWithNoBlockingStateCanSubmit() throws Exception {
        DatabaseDataStore store = storeWith(List.of(), List.of());
        Session session = borrowerSession();

        BorrowerEligibility eligibility = new BorrowerEligibilityService(store, session, CLOCK)
                .currentEligibility();

        assertTrue(eligibility.canSubmitRequest());
        assertTrue(eligibility.blockers().isEmpty());
    }

    @Test
    void overdueLostAndLimitBlockersAreAllReported() throws Exception {
        Loan overdue = new Loan(
                "loan-request-overdue", "request-overdue", "borrower", "camera1",
                TODAY.minusDays(14), TODAY.minusDays(1), null, LoanStatus.ACTIVE);
        Loan lost = new Loan(
                "loan-request-lost", "request-lost", "borrower", "camera2",
                TODAY.minusDays(4), TODAY.plusDays(10), null, LoanStatus.LOST);
        LoanRequest approvedReservation = request(
                "request-approved", "camera3", RequestStatus.APPROVED);
        LoanRequest overdueRequest = request(
                "request-overdue", "camera1", RequestStatus.COLLECTED);
        LoanRequest lostRequest = request(
                "request-lost", "camera2", RequestStatus.COLLECTED);
        DatabaseDataStore store = storeWith(
                List.of(approvedReservation, overdueRequest, lostRequest), List.of(overdue, lost));

        BorrowerEligibility eligibility = new BorrowerEligibilityService(
                store, borrowerSession(), CLOCK).currentEligibility();

        assertFalse(eligibility.canSubmitRequest());
        assertEquals(List.of(
                EligibilityBlocker.OVERDUE_LOAN,
                EligibilityBlocker.LOST_LOAN,
                EligibilityBlocker.LOAN_OR_RESERVATION_LIMIT), eligibility.blockers());
    }

    @Test
    void loggedOutAndWrongRoleCallsAreRejected() throws Exception {
        DatabaseDataStore store = storeWith(List.of(), List.of());
        Session loggedOut = new Session();
        Session supervisor = new Session();
        supervisor.start(new User("supervisor", Role.SUPERVISOR));

        assertThrows(IllegalStateException.class,
                () -> new BorrowerEligibilityService(store, loggedOut, CLOCK).currentEligibility());
        assertThrows(IllegalStateException.class,
                () -> new BorrowerEligibilityService(store, supervisor, CLOCK).currentEligibility());
    }

    @Test
    void anotherBorrowersLoanDoesNotBlockTheLoggedInBorrower() throws Exception {
        Loan otherBorrowersOverdueLoan = new Loan(
                "loan-request-other", "request-other", "other", "camera1",
                TODAY.minusDays(14), TODAY.minusDays(1), null, LoanStatus.ACTIVE);
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("other-owner"));
        store.loadOrSeed();
        store.save(new LoanDeskData(
                List.of(
                        new User("borrower", Role.BORROWER),
                        new User("other", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(requestFor("request-other", "other", "camera1", RequestStatus.COLLECTED)),
                List.of(otherBorrowersOverdueLoan)));

        BorrowerEligibility eligibility = new BorrowerEligibilityService(
                store, borrowerSession(), CLOCK).currentEligibility();

        assertTrue(eligibility.canSubmitRequest());
        assertTrue(eligibility.blockers().isEmpty());
    }

    @Test
    void expiredApprovedReservationsNoLongerCountTowardsTheLimit() throws Exception {
        List<LoanRequest> expiredRequests = List.of(
                requestWithDates("expired-1", "camera1", TODAY.minusDays(1), RequestStatus.APPROVED),
                requestWithDates("expired-2", "camera2", TODAY.minusDays(1), RequestStatus.APPROVED),
                requestWithDates("expired-3", "camera3", TODAY.minusDays(1), RequestStatus.APPROVED));

        BorrowerEligibility eligibility = new BorrowerEligibilityService(
                storeWith(expiredRequests, List.of()), borrowerSession(), CLOCK).currentEligibility();

        assertTrue(eligibility.canSubmitRequest());
    }

    private DatabaseDataStore storeWith(List<LoanRequest> requests, List<Loan> loans) throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(
                        new Equipment("camera1", "Camera 1"),
                        new Equipment("camera2", "Camera 2"),
                        new Equipment("camera3", "Camera 3")),
                requests,
                loans));
        return store;
    }

    private Session borrowerSession() {
        Session session = new Session();
        session.start(new User("borrower", Role.BORROWER));
        return session;
    }

    private static LoanRequest request(String requestId, String equipmentId, RequestStatus status) {
        return requestFor(requestId, "borrower", equipmentId, status);
    }

    private static LoanRequest requestFor(
            String requestId, String borrower, String equipmentId, RequestStatus status) {
        String loanId = status == RequestStatus.COLLECTED ? "loan-" + requestId : null;
        String decisionBy = status == RequestStatus.APPROVED || status == RequestStatus.COLLECTED
                ? "supervisor" : null;
        Instant decisionAt = decisionBy == null ? null : NOW;
        String decisionReason = decisionBy == null ? null : "Approved";
        return new LoanRequest(
                requestId, borrower, equipmentId, "Academic project", TODAY,
                TODAY.plusDays(14), status, loanId, NOW, NOW,
                decisionBy, decisionAt, decisionReason, null, null, null);
    }

    private static LoanRequest requestWithDates(
            String requestId, String equipmentId, LocalDate startDate, RequestStatus status) {
        return new LoanRequest(
                requestId, "borrower", equipmentId, "Academic project", startDate,
                startDate.plusDays(14), status, null, NOW, NOW,
                "supervisor", NOW, "Approved", null, null, null);
    }
}
