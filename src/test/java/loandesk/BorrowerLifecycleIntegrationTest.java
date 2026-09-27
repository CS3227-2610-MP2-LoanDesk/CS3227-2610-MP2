package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.AvailabilityService;
import loandesk.application.BorrowerLoanService;
import loandesk.application.BorrowerRequestService;
import loandesk.application.Session;
import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

/** Verifies borrower behaviour against synthetic shared lifecycle snapshots. */
class BorrowerLifecycleIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-26T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsSyntheticLifecycleStatesAfterEachPersistedTransition() throws Exception {
        DatabaseDataStore store = initialStore();
        BorrowerRequestService requestService = new BorrowerRequestService(
                store, borrowerSession(), CLOCK);
        AvailabilityService availability = new AvailabilityService();
        Equipment camera = store.loadOrSeed().equipment().get(0);

        LoanRequest pending = requestService.submitRequest(
                camera.id(), "Academic project", TODAY.plusDays(1), TODAY.plusDays(3));
        assertEquals(RequestStatus.PENDING, pending.status());
        assertEquals(AvailabilityStatus.AVAILABLE, availability.calculate(
                camera, store.loadOrSeed().requests(), store.loadOrSeed().loans(), TODAY));

        LoanRequest approved = decidedRequest(pending, RequestStatus.APPROVED, null);
        store.save(snapshot(store, List.of(approved), List.of()));
        assertEquals(AvailabilityStatus.RESERVED, availability.calculate(
                camera, store.loadOrSeed().requests(), store.loadOrSeed().loans(), TODAY));
        assertEquals(RequestStatus.APPROVED, requestService.listOwnRequests().get(0).status());

        Loan activeLoan = new Loan(
                "loan-camera1", approved.requestId(), "borrower", camera.id(),
                TODAY.plusDays(1), TODAY.plusDays(3), null, LoanStatus.ACTIVE);
        LoanRequest collected = decidedRequest(
                approved, RequestStatus.COLLECTED, activeLoan.loanId());
        store.save(snapshot(store, List.of(collected), List.of(activeLoan)));
        assertEquals(AvailabilityStatus.ON_LOAN, availability.calculate(
                camera, store.loadOrSeed().requests(), store.loadOrSeed().loans(), TODAY));

        DatabaseDataStore afterCheckout = new DatabaseDataStore(
                temporaryDirectory.resolve("loandesk"));
        assertEquals(RequestStatus.COLLECTED, new BorrowerRequestService(
                afterCheckout, borrowerSession(), CLOCK).listOwnRequests().get(0).status());
        assertEquals(List.of(activeLoan), new BorrowerLoanService(
                afterCheckout, borrowerSession()).listOwnLoans());

        Loan returnedLoan = new Loan(
                activeLoan.loanId(), activeLoan.requestId(), activeLoan.borrowerUsername(),
                activeLoan.equipmentId(), activeLoan.checkoutDate(), activeLoan.dueDate(),
                TODAY.plusDays(2), LoanStatus.RETURNED);
        store.save(snapshot(store, List.of(collected), List.of(returnedLoan)));
        assertEquals(AvailabilityStatus.AVAILABLE, availability.calculate(
                camera, store.loadOrSeed().requests(), store.loadOrSeed().loans(), TODAY));

        DatabaseDataStore afterReturn = new DatabaseDataStore(
                temporaryDirectory.resolve("loandesk"));
        assertEquals(List.of(returnedLoan), new BorrowerLoanService(
                afterReturn, borrowerSession()).listOwnLoans());
    }

    @Test
    void filtersForeignLifecycleRecordsAndRejectsOtherRoles() throws Exception {
        DatabaseDataStore store = initialStore();
        LoanRequest ownRequest = collectedRequest(
                "own-request", "borrower", "own-loan", "camera1");
        LoanRequest foreignRequest = collectedRequest(
                "foreign-request", "other", "foreign-loan", "camera1");
        Loan ownLoan = loan("own-loan", ownRequest);
        Loan foreignLoan = loan("foreign-loan", foreignRequest);
        store.save(snapshot(
                store, List.of(ownRequest, foreignRequest), List.of(ownLoan, foreignLoan)));

        assertEquals(List.of("own-request"), new BorrowerRequestService(
                store, borrowerSession(), CLOCK).listOwnRequests().stream()
                .map(LoanRequest::requestId).toList());
        assertEquals(List.of("own-loan"), new BorrowerLoanService(
                store, borrowerSession()).listOwnLoans().stream()
                .map(Loan::loanId).toList());

        Session loggedOut = new Session();
        Session supervisor = sessionFor("supervisor", Role.SUPERVISOR);
        Session custodian = sessionFor("custodian", Role.CUSTODIAN);
        for (Session invalidSession : List.of(loggedOut, supervisor, custodian)) {
            assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                    store, invalidSession, CLOCK).listOwnRequests());
            assertThrows(IllegalStateException.class, () -> new BorrowerLoanService(
                    store, invalidSession).listOwnLoans());
        }

        assertEquals(2, store.loadOrSeed().requests().size());
        assertEquals(2, store.loadOrSeed().loans().size());
    }

    private DatabaseDataStore initialStore() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        store.save(new LoanDeskData(
                List.of(
                        new User("borrower", Role.BORROWER),
                        new User("other", Role.BORROWER),
                        new User("supervisor", Role.SUPERVISOR),
                        new User("custodian", Role.CUSTODIAN)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1"))));
        return store;
    }

    private LoanDeskData snapshot(
            DatabaseDataStore store,
            List<LoanRequest> requests,
            List<Loan> loans) throws Exception {
        LoanDeskData current = store.loadOrSeed();
        return new LoanDeskData(
                current.users(), current.credentials(), current.equipment(), requests, loans);
    }

    private LoanRequest decidedRequest(
            LoanRequest current,
            RequestStatus status,
            String loanId) {
        return new LoanRequest(
                current.requestId(), current.borrowerUsername(), current.equipmentId(),
                current.purpose(), current.startDate(), current.dueDate(), status, loanId,
                current.createdAt(), NOW, "supervisor", NOW, "Approved for project",
                null, null, null);
    }

    private Session borrowerSession() {
        return sessionFor("borrower", Role.BORROWER);
    }

    private Session sessionFor(String username, Role role) {
        Session session = new Session();
        session.start(new User(username, role));
        return session;
    }

    private LoanRequest collectedRequest(
            String requestId,
            String borrowerUsername,
            String loanId,
            String equipmentId) {
        return new LoanRequest(
                requestId, borrowerUsername, equipmentId, "Academic project",
                TODAY.plusDays(1), TODAY.plusDays(3), RequestStatus.COLLECTED, loanId,
                NOW.minusSeconds(2), NOW, "supervisor", NOW, "Approved for project",
                null, null, null);
    }

    private Loan loan(String loanId, LoanRequest request) {
        return new Loan(
                loanId, request.requestId(), request.borrowerUsername(), request.equipmentId(),
                TODAY.plusDays(1), TODAY.plusDays(3), null, LoanStatus.ACTIVE);
    }
}
