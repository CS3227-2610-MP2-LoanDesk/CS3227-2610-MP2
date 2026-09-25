package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.BorrowerLoanService;
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

class BorrowerLoanServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);

    @TempDir
    Path temporaryDirectory;

    @Test
    void listsOnlyOwnLoansWithActiveLoansBeforeHistory() throws Exception {
        DatabaseDataStore store = storeWithLoans(List.of(
                loan("returned", "borrower", LoanStatus.RETURNED, TODAY.minusDays(5),
                        TODAY.minusDays(1), TODAY.minusDays(2)),
                loan("foreign", "other", LoanStatus.ACTIVE, TODAY.minusDays(1),
                        TODAY.plusDays(5), null),
                loan("active", "borrower", LoanStatus.ACTIVE, TODAY.minusDays(1),
                        TODAY.plusDays(5), null),
                loan("lost", "borrower", LoanStatus.LOST, TODAY.minusDays(2),
                        TODAY.plusDays(1), null)));

        assertEquals(List.of("active", "lost", "returned"),
                new BorrowerLoanService(store, borrowerSession()).listOwnLoans().stream()
                        .map(Loan::loanId).toList());
    }

    @Test
    void requiresLoggedInBorrowerSession() throws Exception {
        DatabaseDataStore store = storeWithLoans(List.of());
        Session loggedOut = new Session();
        Session supervisor = new Session();
        supervisor.start(new User("supervisor", Role.SUPERVISOR));
        Session custodian = new Session();
        custodian.start(new User("custodian", Role.CUSTODIAN));

        assertThrows(IllegalStateException.class, () -> new BorrowerLoanService(
                store, loggedOut).listOwnLoans());
        assertThrows(IllegalStateException.class, () -> new BorrowerLoanService(
                store, supervisor).listOwnLoans());
        assertThrows(IllegalStateException.class, () -> new BorrowerLoanService(
                store, custodian).listOwnLoans());
    }

    @Test
    void returnsEmptyResultForBorrowerWithoutLoans() throws Exception {
        assertEquals(List.of(), new BorrowerLoanService(
                storeWithLoans(List.of()), borrowerSession()).listOwnLoans());
    }

    @Test
    void listingLoansDoesNotSaveData() throws Exception {
        LoanDeskData data = storeWithLoans(List.of(
                loan("read-only", "borrower", LoanStatus.ACTIVE,
                        TODAY, TODAY.plusDays(1), null))).loadOrSeed();
        assertEquals(1, new BorrowerLoanService(
                new ReadOnlyDataStore(data), borrowerSession()).listOwnLoans().size());
    }

    @Test
    void reloadsOwnLoansFromFreshStoreAndPreservesOverdueBoundary() throws Exception {
        Loan overdue = loan("overdue", "borrower", LoanStatus.ACTIVE,
                TODAY.minusDays(5), TODAY.minusDays(1), null);
        DatabaseDataStore store = storeWithLoans(List.of(overdue));
        DatabaseDataStore restarted = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));

        Loan reloaded = new BorrowerLoanService(restarted, borrowerSession())
                .listOwnLoans().get(0);
        assertEquals(overdue, reloaded);
        assertEquals(true, reloaded.isOverdue(TODAY));
        assertEquals(false, reloaded.isOverdue(TODAY.minusDays(1)));
    }

    private DatabaseDataStore storeWithLoans(List<Loan> loans) throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        List<LoanRequest> requests = loans.stream()
                .map(loan -> new LoanRequest(
                        loan.requestId(),
                        loan.borrowerUsername(),
                        loan.equipmentId(),
                        "Academic project",
                        loan.checkoutDate(),
                        loan.dueDate(),
                        RequestStatus.COLLECTED,
                        loan.loanId(),
                        java.time.Instant.parse("2026-09-25T08:00:00Z"),
                        java.time.Instant.parse("2026-09-25T08:00:00Z"),
                        "supervisor",
                        java.time.Instant.parse("2026-09-25T08:00:00Z"),
                        "Approved",
                        null,
                        null,
                        null))
                .toList();
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER), new User("other", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                requests,
                loans));
        return store;
    }

    private Loan loan(
            String id,
            String borrower,
            LoanStatus status,
            LocalDate checkoutDate,
            LocalDate dueDate,
            LocalDate returnedDate) {
        return new Loan(id, "request-" + id, borrower, "camera1",
                checkoutDate, dueDate, returnedDate, status);
    }

    private Session borrowerSession() {
        Session session = new Session();
        session.start(new User("borrower", Role.BORROWER));
        return session;
    }

    private static final class ReadOnlyDataStore implements loandesk.persistence.DataStore {
        private final LoanDeskData data;

        private ReadOnlyDataStore(LoanDeskData data) {
            this.data = data;
        }

        @Override
        public LoanDeskData loadOrSeed() {
            return data;
        }

        @Override
        public void save(LoanDeskData ignored) {
            throw new AssertionError("borrower loan listing must not save data");
        }
    }
}
