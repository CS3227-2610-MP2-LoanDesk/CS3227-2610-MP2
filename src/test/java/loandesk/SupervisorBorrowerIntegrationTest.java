package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import loandesk.application.BorrowerRequestService;
import loandesk.application.ReviewFilter;
import loandesk.application.Session;
import loandesk.application.SupervisorRequestService;
import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

/**
 * Exercises the borrower and supervisor halves of the shared workflow against
 * one real database, including restart. The custodian half of the acceptance
 * journey, checkout and return, does not exist yet and is not simulated here.
 */
class SupervisorBorrowerIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-28T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void aSubmittedRequestIsApprovedReservedAndSurvivesRestart() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore store = initialStore(databasePath);

        LoanRequest submitted = borrowerService(store).submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(4));
        assertEquals(RequestStatus.PENDING, submitted.status());

        SupervisorRequestService supervisor = supervisorService(store);
        assertEquals(List.of(submitted.requestId()),
                supervisor.reviewQueue(ReviewFilter.ofStatus(RequestStatus.PENDING)).stream()
                        .map(LoanRequest::requestId).toList());

        LoanRequest approved = supervisor.approve(submitted.requestId(), "Coursework");
        assertEquals(RequestStatus.APPROVED, approved.status());
        assertEquals(AvailabilityStatus.RESERVED, supervisor.availabilityOf("camera1"));

        DatabaseDataStore restarted = new DatabaseDataStore(databasePath);
        restarted.loadOrSeed();
        LoanRequest afterRestart = borrowerService(restarted).listOwnRequests().get(0);
        assertEquals(RequestStatus.APPROVED, afterRestart.status());
        assertEquals("supervisor", afterRestart.decisionBy());
        assertEquals("Coursework", afterRestart.decisionReason());
        assertEquals(1, supervisorService(restarted).decisionHistory().size());
    }

    @Test
    void aRejectedRequestReleasesTheItemAndTheBorrowerCanRequestAgain() throws Exception {
        DatabaseDataStore store = initialStore(temporaryDirectory.resolve("loandesk"));
        BorrowerRequestService borrower = borrowerService(store);
        SupervisorRequestService supervisor = supervisorService(store);

        LoanRequest first = borrower.submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(4));
        supervisor.reject(first.requestId(), "Item reserved for a class that week");

        LoanRequest visible = borrower.listOwnRequests().get(0);
        assertEquals(RequestStatus.REJECTED, visible.status());
        assertEquals("Item reserved for a class that week", visible.decisionReason());
        assertEquals(AvailabilityStatus.AVAILABLE, supervisor.availabilityOf("camera1"));

        LoanRequest second = borrower.submitRequest(
                "camera1", "Academic project", TODAY.plusDays(6), TODAY.plusDays(8));
        assertEquals(RequestStatus.PENDING, second.status());
    }

    @Test
    void aBorrowerCannotCancelARequestAfterTheSupervisorRejectedIt() throws Exception {
        DatabaseDataStore store = initialStore(temporaryDirectory.resolve("loandesk"));
        BorrowerRequestService borrower = borrowerService(store);

        LoanRequest submitted = borrower.submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(4));
        supervisorService(store).reject(submitted.requestId(), "Not this term");

        assertThrows(IllegalStateException.class,
                () -> borrower.cancelRequest(submitted.requestId(), "Plans changed"));
    }

    @Test
    void aSupervisorCancellationIsVisibleToTheBorrowerWithItsReason() throws Exception {
        DatabaseDataStore store = initialStore(temporaryDirectory.resolve("loandesk"));
        BorrowerRequestService borrower = borrowerService(store);
        SupervisorRequestService supervisor = supervisorService(store);

        LoanRequest submitted = borrower.submitRequest(
                "camera1", "Academic project", TODAY.plusDays(2), TODAY.plusDays(4));
        supervisor.approve(submitted.requestId(), null);
        supervisor.cancelApproved(submitted.requestId(), "Equipment needed for open day");

        LoanRequest visible = borrower.listOwnRequests().get(0);
        assertEquals(RequestStatus.CANCELLED, visible.status());
        assertEquals("supervisor", visible.cancelledBy());
        assertEquals("Equipment needed for open day", visible.cancellationReason());
        assertEquals(AvailabilityStatus.AVAILABLE, supervisor.availabilityOf("camera1"));
    }

    @Test
    void aBorrowerSessionCannotReachSupervisorOperationsOnTheSameStore() throws Exception {
        DatabaseDataStore store = initialStore(temporaryDirectory.resolve("loandesk"));
        LoanRequest submitted = borrowerService(store).submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(4));

        SupervisorRequestService asBorrower = new SupervisorRequestService(
                store, sessionFor("borrower", Role.BORROWER), CLOCK);

        assertThrows(IllegalStateException.class,
                () -> asBorrower.approve(submitted.requestId(), null));
        assertEquals(RequestStatus.PENDING,
                borrowerService(store).listOwnRequests().get(0).status());
    }

    @Test
    void anApprovalThatWasNeverCollectedExpiresOnTheNextDay() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore store = initialStore(databasePath);
        BorrowerRequestService borrower = borrowerService(store);

        LoanRequest submitted = borrower.submitRequest(
                "camera1", "Academic project", TODAY, TODAY.plusDays(2));
        supervisorService(store).approve(submitted.requestId(), null);

        Clock nextDay = Clock.fixed(NOW.plusSeconds(86_400), ZoneOffset.UTC);
        DatabaseDataStore restarted = new DatabaseDataStore(databasePath);
        restarted.loadOrSeed();
        SupervisorRequestService tomorrow = new SupervisorRequestService(
                restarted, supervisorSession(), nextDay);

        List<LoanRequest> queue = tomorrow.reviewQueue(ReviewFilter.none());

        assertEquals(RequestStatus.EXPIRED, queue.get(0).status());
        assertEquals(AvailabilityStatus.AVAILABLE, tomorrow.availabilityOf("camera1"));
        assertTrue(new DatabaseDataStore(databasePath).loadOrSeed().requests().stream()
                .allMatch(request -> request.status() == RequestStatus.EXPIRED));
    }

    private DatabaseDataStore initialStore(Path databasePath) throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(databasePath);
        store.loadOrSeed();
        store.save(new LoanDeskData(
                List.of(
                        new User("borrower", Role.BORROWER),
                        new User("supervisor", Role.SUPERVISOR)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1"))));
        return store;
    }

    private BorrowerRequestService borrowerService(DatabaseDataStore store) {
        return new BorrowerRequestService(store, sessionFor("borrower", Role.BORROWER), CLOCK);
    }

    private SupervisorRequestService supervisorService(DatabaseDataStore store) {
        return new SupervisorRequestService(store, supervisorSession(), CLOCK);
    }

    private static Session supervisorSession() {
        return sessionFor("supervisor", Role.SUPERVISOR);
    }

    private static Session sessionFor(String username, Role role) {
        Session session = new Session();
        session.start(new User(username, role));
        return session;
    }
}
