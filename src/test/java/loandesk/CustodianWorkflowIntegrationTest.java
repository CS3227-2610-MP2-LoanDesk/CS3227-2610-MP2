package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.BorrowerLoanService;
import loandesk.application.BorrowerRequestService;
import loandesk.application.CustodianCollectionService;
import loandesk.application.CustodianFulfilmentService;
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
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

/** Cross-role acceptance coverage using a fresh database and a restart. */
class CustodianWorkflowIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-28T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void recoveredEquipmentReturnsToActiveUntilItsConditionIsRecordedAndSurvivesRestart() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore store = initialStore(databasePath);

        LoanRequest submitted = borrowerRequests(store).submitRequest(
                "camera1", "Academic project", TODAY, TODAY.plusDays(3));
        LoanRequest approved = supervisor(store).approve(submitted.requestId(), "Approved");
        assertEquals(RequestStatus.APPROVED, approved.status());

        CustodianCollectionService collections = new CustodianCollectionService(
                store, sessionFor("custodian", Role.CUSTODIAN), CLOCK);
        assertEquals(List.of(submitted.requestId()), collections.collectionsQueue().stream()
                .map(LoanRequest::requestId).toList());
        Loan checkedOut = collections.checkout(submitted.requestId());
        assertEquals(LoanStatus.ACTIVE, checkedOut.status());
        assertEquals(List.of(checkedOut.loanId()), borrowerLoans(store).listOwnLoans().stream()
                .map(Loan::loanId).toList());

        CustodianFulfilmentService fulfilment = new CustodianFulfilmentService(
                store, sessionFor("custodian", Role.CUSTODIAN), CLOCK);
        assertEquals(LoanStatus.LOST, fulfilment.markLost(checkedOut.loanId()).status());
        assertEquals(LoanStatus.LOST, borrowerLoans(store).listOwnLoans().getFirst().status());

        Loan recovered = fulfilment.recoverLost(checkedOut.loanId());
        assertEquals(LoanStatus.ACTIVE, recovered.status());
        assertEquals(null, recovered.returnedDate());
        assertEquals(EquipmentCondition.GOOD, store.loadOrSeed().equipment().getFirst().condition());
        assertEquals(AvailabilityStatus.ON_LOAN, supervisor(store).availabilityOf("camera1"));
        assertEquals(List.of(checkedOut.loanId()), fulfilment.activeLoans().stream()
                .map(Loan::loanId).toList());

        Loan returned = fulfilment.returnLoan(checkedOut.loanId(), EquipmentCondition.UNDER_MAINTENANCE);
        assertEquals(LoanStatus.RETURNED, returned.status());
        assertEquals(TODAY, returned.returnedDate());

        DatabaseDataStore restarted = new DatabaseDataStore(databasePath);
        LoanDeskData data = restarted.loadOrSeed();
        assertEquals(RequestStatus.COLLECTED, data.requests().getFirst().status());
        assertEquals(checkedOut.loanId(), data.requests().getFirst().loanId());
        assertEquals(LoanStatus.RETURNED, borrowerLoans(restarted).listOwnLoans().getFirst().status());
        assertEquals(EquipmentCondition.UNDER_MAINTENANCE, data.equipment().getFirst().condition());
        assertEquals(AvailabilityStatus.UNAVAILABLE, supervisor(restarted).availabilityOf("camera1"));
        assertTrue(new CustodianFulfilmentService(restarted,
                sessionFor("custodian", Role.CUSTODIAN), CLOCK).activeLoans().isEmpty());
    }

    private static DatabaseDataStore initialStore(Path databasePath) throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(databasePath);
        store.loadOrSeed();
        store.save(new LoanDeskData(List.of(
                new User("borrower", Role.BORROWER),
                new User("supervisor", Role.SUPERVISOR),
                new User("custodian", Role.CUSTODIAN)), List.of(),
                List.of(new Equipment("camera1", "Camera 1"))));
        return store;
    }

    private static BorrowerRequestService borrowerRequests(DatabaseDataStore store) {
        return new BorrowerRequestService(store, sessionFor("borrower", Role.BORROWER), CLOCK);
    }

    private static BorrowerLoanService borrowerLoans(DatabaseDataStore store) {
        return new BorrowerLoanService(store, sessionFor("borrower", Role.BORROWER));
    }

    private static SupervisorRequestService supervisor(DatabaseDataStore store) {
        return new SupervisorRequestService(store, sessionFor("supervisor", Role.SUPERVISOR), CLOCK);
    }

    private static Session sessionFor(String username, Role role) {
        Session session = new Session();
        session.start(new User(username, role));
        return session;
    }
}
