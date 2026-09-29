package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.BorrowerRequestService;
import loandesk.application.EligibilityBlocker;
import loandesk.application.ReviewFilter;
import loandesk.application.Session;
import loandesk.application.SupervisorRequestService;
import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DataStore;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class SupervisorRequestServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-28T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void approvesAPendingRequestAndRecordsTheDecision() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));

        LoanRequest approved = supervisorService(store).approve("request1", " Lab project ");

        assertEquals(RequestStatus.APPROVED, approved.status());
        assertEquals("supervisor", approved.decisionBy());
        assertEquals(NOW, approved.decisionAt());
        assertEquals("Lab project", approved.decisionReason());
        assertEquals(RequestStatus.APPROVED, reload(store, "request1").status());
    }

    @Test
    void approvalWithoutAReasonIsAllowedButRejectionRequiresOne() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));

        LoanRequest approved = supervisorService(store).approve("request1", "   ");
        assertNull(approved.decisionReason());

        DatabaseDataStore other = storeWith(pending("request2", "borrower"));
        SupervisorRequestService service = supervisorService(other);
        assertThrows(IllegalArgumentException.class, () -> service.reject("request2", "  "));
        assertThrows(IllegalArgumentException.class, () -> service.reject("request2", null));
    }

    @Test
    void rejectsWithARecordedReason() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));

        LoanRequest rejected = supervisorService(store).reject("request1", "Item reserved for class");

        assertEquals(RequestStatus.REJECTED, rejected.status());
        assertEquals("supervisor", rejected.decisionBy());
        assertEquals("Item reserved for class", rejected.decisionReason());
    }

    @Test
    void overlappingApprovalsCannotBothSucceed() throws Exception {
        DatabaseDataStore store = storeWith(
                pending("first", "borrower"),
                pending("second", "otherBorrower"));
        SupervisorRequestService service = supervisorService(store);

        service.approve("first", null);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.approve("second", null));
        assertTrue(failure.getMessage().contains("RESERVED"), failure.getMessage());
        assertEquals(RequestStatus.PENDING, reload(store, "second").status());
    }

    @Test
    void approvalRechecksAvailabilityAgainstEquipmentCondition() throws Exception {
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Camera 1", EquipmentCondition.DAMAGED)),
                List.of(pending("request1", "borrower")),
                List.of());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> supervisorService(store).approve("request1", null));

        assertTrue(failure.getMessage().contains("UNAVAILABLE"), failure.getMessage());
        assertEquals(RequestStatus.PENDING, reload(store, "request1").status());
    }

    @Test
    void approvalIsRefusedWhileTheItemIsOnLoan() throws Exception {
        LoanRequest collected = collected("collected", "otherBorrower", "loan-1");
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(collected, pending("request1", "borrower")),
                List.of(activeLoan("loan-1", "collected", "otherBorrower", TODAY.plusDays(3))));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> supervisorService(store).approve("request1", null));

        assertTrue(failure.getMessage().contains("ON_LOAN"), failure.getMessage());
    }

    @Test
    void approvalIsRefusedForABorrowerWhoBecameIneligible() throws Exception {
        LoanRequest collected = collected("collected", "borrower", "loan-1");
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Camera 1"), new Equipment("camera2", "Camera 2")),
                List.of(collected, pendingFor("request1", "borrower", "camera2")),
                List.of(activeLoan("loan-1", "collected", "borrower", TODAY.minusDays(1))));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> supervisorService(store).approve("request1", null));

        assertTrue(failure.getMessage().contains(EligibilityBlocker.OVERDUE_LOAN.name()),
                failure.getMessage());
        assertEquals(RequestStatus.PENDING, reload(store, "request1").status());
    }

    @Test
    void decisionsOnAlreadyDecidedRequestsAreRejected() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));
        SupervisorRequestService service = supervisorService(store);
        service.reject("request1", "Not this term");

        assertThrows(IllegalStateException.class, () -> service.approve("request1", null));
        assertThrows(IllegalStateException.class, () -> service.reject("request1", "Again"));
        assertEquals(RequestStatus.REJECTED, reload(store, "request1").status());
    }

    @Test
    void anApprovedRequestCannotBeApprovedAgain() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));
        SupervisorRequestService service = supervisorService(store);
        service.approve("request1", null);

        assertThrows(IllegalStateException.class, () -> service.approve("request1", null));
    }

    @Test
    void cancellingAnApprovedBookingReleasesTheReservation() throws Exception {
        DatabaseDataStore store = storeWith(
                pending("first", "borrower"),
                pending("second", "otherBorrower"));
        SupervisorRequestService service = supervisorService(store);
        service.approve("first", null);

        LoanRequest cancelled = service.cancelApproved("first", "Equipment needed for open day");

        assertEquals(RequestStatus.CANCELLED, cancelled.status());
        assertEquals("supervisor", cancelled.cancelledBy());
        assertEquals("Equipment needed for open day", cancelled.cancellationReason());
        assertEquals("supervisor", cancelled.decisionBy());
        assertEquals(AvailabilityStatus.AVAILABLE, service.availabilityOf("camera1"));
        assertEquals(RequestStatus.APPROVED, service.approve("second", null).status());
    }

    @Test
    void onlyApprovedBookingsCanBeCancelledAndAReasonIsRequired() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));
        SupervisorRequestService service = supervisorService(store);

        assertThrows(IllegalStateException.class,
                () -> service.cancelApproved("request1", "Changed plans"));
        service.approve("request1", null);
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelApproved("request1", "  "));
    }

    @Test
    void aCollectedBookingCannotBeCancelled() throws Exception {
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(collected("collected", "borrower", "loan-1")),
                List.of(activeLoan("loan-1", "collected", "borrower", TODAY.plusDays(3))));

        assertThrows(IllegalStateException.class,
                () -> supervisorService(store).cancelApproved("collected", "Too late"));
    }

    @Test
    void borrowersAndCustodiansCannotDecideRequests() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));

        for (Role role : List.of(Role.BORROWER, Role.CUSTODIAN)) {
            SupervisorRequestService service = new SupervisorRequestService(
                    store, sessionFor(role), CLOCK);
            assertThrows(IllegalStateException.class, () -> service.approve("request1", null));
            assertThrows(IllegalStateException.class, () -> service.reject("request1", "No"));
            assertThrows(IllegalStateException.class,
                    () -> service.cancelApproved("request1", "No"));
            assertThrows(IllegalStateException.class,
                    () -> service.reviewQueue(ReviewFilter.none()));
            assertThrows(IllegalStateException.class, () -> service.equipmentNameOf("camera1"));
            assertThrows(IllegalStateException.class, service::decisionHistory);
        }
        assertEquals(RequestStatus.PENDING, reload(store, "request1").status());
    }

    @Test
    void aSignedOutSessionCannotReachTheReviewQueue() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));
        SupervisorRequestService service = new SupervisorRequestService(
                store, new Session(), CLOCK);

        assertThrows(IllegalStateException.class, () -> service.reviewQueue(ReviewFilter.none()));
        assertThrows(IllegalStateException.class, () -> service.equipmentNameOf("camera1"));
        assertThrows(IllegalStateException.class, () -> service.approve("request1", null));
    }

    @Test
    void theQueueShowsPendingRequestsFirstAndFiltersByStatusBorrowerAndDate() throws Exception {
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Camera 1"), new Equipment("camera2", "Camera 2")),
                List.of(
                        pendingFor("early", "borrower", "camera1"),
                        laterPending("late", "otherBorrower", "camera2", TODAY.plusDays(5))),
                List.of());
        SupervisorRequestService service = supervisorService(store);
        service.approve("early", null);

        List<LoanRequest> all = service.reviewQueue(ReviewFilter.none());
        assertEquals(List.of("late", "early"), ids(all));

        assertEquals(List.of("late"),
                ids(service.reviewQueue(ReviewFilter.ofStatus(RequestStatus.PENDING))));
        assertEquals(List.of("early"),
                ids(service.reviewQueue(new ReviewFilter(null, " BORROWER ", null, null))));
        assertEquals(List.of("late"), ids(service.reviewQueue(
                new ReviewFilter(null, null, TODAY.plusDays(1), null))));
        assertEquals(List.of("early"), ids(service.reviewQueue(
                new ReviewFilter(null, null, null, TODAY))));
    }

    @Test
    void theQueueFiltersRequestsByEquipmentName() throws Exception {
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Studio Camera"),
                        new Equipment("camera2", "Portable Projector")),
                List.of(pendingFor("camera-request", "borrower", "camera1"),
                        pendingFor("projector-request", "otherBorrower", "camera2")),
                List.of());

        List<LoanRequest> queue = supervisorService(store).reviewQueue(
                new ReviewFilter(null, null, null, null, "  projector  "));

        assertEquals(List.of("projector-request"), ids(queue));
    }

    @Test
    void theQueueExpiresApprovalsThatWereNeverCollected() throws Exception {
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(approvedStartingOn("lapsed", "borrower", TODAY.minusDays(4))),
                List.of());
        SupervisorRequestService service = supervisorService(store);

        List<LoanRequest> queue = service.reviewQueue(ReviewFilter.none());

        assertEquals(RequestStatus.EXPIRED, queue.get(0).status());
        assertEquals(AvailabilityStatus.AVAILABLE, service.availabilityOf("camera1"));
    }

    @Test
    void inspectionExposesTheBorrowerOutstandingLoansAndEligibility() throws Exception {
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Camera 1"), new Equipment("camera2", "Camera 2")),
                List.of(
                        collected("collected", "borrower", "loan-1"),
                        pendingFor("request1", "borrower", "camera2")),
                List.of(activeLoan("loan-1", "collected", "borrower", TODAY.minusDays(2))));
        SupervisorRequestService service = supervisorService(store);

        LoanRequest inspected = service.findRequest("request1");
        assertEquals("borrower", inspected.borrowerUsername());
        assertEquals("Academic project", inspected.purpose());

        assertEquals(List.of("loan-1"),
                service.outstandingLoans("borrower").stream().map(Loan::loanId).toList());
        assertEquals("Camera 1", service.equipmentNameOf("camera1"));
        assertFalse(service.eligibilityOf("borrower").canSubmitRequest());
        assertTrue(service.eligibilityOf("otherBorrower").canSubmitRequest());
    }

    @Test
    void decisionHistoryKeepsWhoDecidedWhenAndWhy() throws Exception {
        DatabaseDataStore store = storeWith(
                List.of(new Equipment("camera1", "Camera 1"), new Equipment("camera2", "Camera 2")),
                List.of(
                        pendingFor("approved", "borrower", "camera1"),
                        pendingFor("rejected", "otherBorrower", "camera2")),
                List.of());
        SupervisorRequestService service = supervisorService(store);
        service.approve("approved", "Coursework");
        service.reject("rejected", "Item reserved for class");

        List<LoanRequest> history = service.decisionHistory();

        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(request -> "supervisor".equals(request.decisionBy())));
        assertTrue(history.stream().allMatch(request -> request.decisionAt() != null));
        assertEquals("Item reserved for class", history.stream()
                .filter(request -> request.requestId().equals("rejected"))
                .findFirst().orElseThrow().decisionReason());
        assertTrue(ids(service.reviewQueue(ReviewFilter.ofStatus(RequestStatus.PENDING))).isEmpty());
    }

    @Test
    void anUnknownRequestOrEquipmentIsReported() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));
        SupervisorRequestService service = supervisorService(store);

        assertThrows(IllegalArgumentException.class, () -> service.findRequest("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.findRequest("  "));
        assertThrows(IllegalArgumentException.class, () -> service.approve("missing", null));
        assertThrows(IllegalArgumentException.class, () -> service.availabilityOf("missing"));
        assertThrows(IllegalArgumentException.class, () -> service.equipmentNameOf("missing"));
    }

    @Test
    void aFailedSaveLeavesTheRequestUndecided() throws Exception {
        LoanDeskData original = dataWith(
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(pending("request1", "borrower")),
                List.of());
        FailingDataStore store = new FailingDataStore(original);

        assertThrows(IOException.class,
                () -> new SupervisorRequestService(store, supervisorSession(), CLOCK)
                        .approve("request1", "Coursework"));

        assertEquals(original, store.loadOrSeed());
    }

    @Test
    void anApprovalIsVisibleToTheBorrowerWithItsReason() throws Exception {
        DatabaseDataStore store = storeWith(pending("request1", "borrower"));
        supervisorService(store).reject("request1", "Item reserved for class");

        List<LoanRequest> borrowerView = new BorrowerRequestService(
                store, sessionFor(Role.BORROWER), CLOCK).listOwnRequests();

        assertEquals(1, borrowerView.size());
        assertEquals(RequestStatus.REJECTED, borrowerView.get(0).status());
        assertEquals("Item reserved for class", borrowerView.get(0).decisionReason());
    }

    private static List<String> ids(List<LoanRequest> requests) {
        return requests.stream().map(LoanRequest::requestId).toList();
    }

    private static LoanRequest reload(DatabaseDataStore store, String requestId) throws Exception {
        return store.loadOrSeed().requests().stream()
                .filter(request -> request.requestId().equals(requestId))
                .findFirst()
                .orElseThrow();
    }

    private static LoanRequest pending(String requestId, String borrower) {
        return pendingFor(requestId, borrower, "camera1");
    }

    private static LoanRequest pendingFor(String requestId, String borrower, String equipmentId) {
        return new LoanRequest(
                requestId, borrower, equipmentId, "Academic project",
                TODAY, TODAY.plusDays(7), RequestStatus.PENDING, null,
                NOW.minusSeconds(600), NOW.minusSeconds(600),
                null, null, null, null, null, null);
    }

    private static LoanRequest laterPending(
            String requestId, String borrower, String equipmentId, LocalDate startDate) {
        return new LoanRequest(
                requestId, borrower, equipmentId, "Academic project",
                startDate, startDate.plusDays(3), RequestStatus.PENDING, null,
                NOW.minusSeconds(300), NOW.minusSeconds(300),
                null, null, null, null, null, null);
    }

    private static LoanRequest approvedStartingOn(
            String requestId, String borrower, LocalDate startDate) {
        return new LoanRequest(
                requestId, borrower, "camera1", "Academic project",
                startDate, startDate.plusDays(3), RequestStatus.APPROVED, null,
                NOW.minusSeconds(600), NOW.minusSeconds(300),
                "supervisor", NOW.minusSeconds(300), "Approved", null, null, null);
    }

    private static LoanRequest collected(String requestId, String borrower, String loanId) {
        return new LoanRequest(
                requestId, borrower, "camera1", "Academic project",
                TODAY.minusDays(1), TODAY.plusDays(3), RequestStatus.COLLECTED, loanId,
                NOW.minusSeconds(900), NOW.minusSeconds(600),
                "supervisor", NOW.minusSeconds(600), "Approved", null, null, null);
    }

    private static Loan activeLoan(
            String loanId, String requestId, String borrower, LocalDate dueDate) {
        return new Loan(loanId, requestId, borrower, "camera1",
                dueDate.minusDays(3), dueDate, null, LoanStatus.ACTIVE);
    }

    private static LoanDeskData dataWith(
            List<Equipment> equipment, List<LoanRequest> requests, List<Loan> loans) {
        return new LoanDeskData(
                List.of(
                        new User("borrower", Role.BORROWER),
                        new User("otherBorrower", Role.BORROWER),
                        new User("supervisor", Role.SUPERVISOR)),
                List.of(),
                equipment,
                requests,
                loans);
    }

    private DatabaseDataStore storeWith(LoanRequest... requests) throws Exception {
        return storeWith(
                List.of(new Equipment("camera1", "Camera 1")), List.of(requests), List.of());
    }

    private DatabaseDataStore storeWith(
            List<Equipment> equipment, List<LoanRequest> requests, List<Loan> loans)
            throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        store.save(dataWith(equipment, requests, loans));
        return store;
    }

    private SupervisorRequestService supervisorService(DataStore store) {
        return new SupervisorRequestService(store, supervisorSession(), CLOCK);
    }

    private static Session supervisorSession() {
        return sessionFor(Role.SUPERVISOR);
    }

    private static Session sessionFor(Role role) {
        Session session = new Session();
        session.start(new User(switch (role) {
            case BORROWER -> "borrower";
            case SUPERVISOR -> "supervisor";
            case CUSTODIAN -> "custodian";
        }, role));
        return session;
    }

    private static final class FailingDataStore implements DataStore {
        private final LoanDeskData data;

        private FailingDataStore(LoanDeskData data) {
            this.data = data;
        }

        @Override
        public LoanDeskData loadOrSeed() {
            return data;
        }

        @Override
        public void save(LoanDeskData ignored) throws IOException {
            throw new IOException("synthetic save failure");
        }
    }
}
