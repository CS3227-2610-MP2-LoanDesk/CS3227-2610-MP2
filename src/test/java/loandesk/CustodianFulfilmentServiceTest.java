package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.AvailabilityService;
import loandesk.application.BorrowerEligibilityService;
import loandesk.application.CustodianFulfilmentService;
import loandesk.application.Session;
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
import loandesk.persistence.StaleDataException;

class CustodianFulfilmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-10T09:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void returnUpdatesLoanAndEquipmentTogetherAndPersistsEachCondition() throws Exception {
        for (EquipmentCondition condition : List.of(EquipmentCondition.GOOD,
                EquipmentCondition.DAMAGED, EquipmentCondition.UNDER_MAINTENANCE)) {
            DatabaseDataStore store = storeWith(activeLoan(), EquipmentCondition.GOOD);

            Loan returned = service(store, Role.CUSTODIAN).returnLoan("loan-1", condition);

            LoanDeskData reloaded = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"))
                    .loadOrSeed();
            assertEquals(LoanStatus.RETURNED, returned.status());
            assertEquals(TODAY, returned.returnedDate());
            assertEquals(condition, reloaded.equipment().getFirst().condition());
            AvailabilityStatus availability = new AvailabilityService().calculate(
                    reloaded.equipment().getFirst(), reloaded.requests(), reloaded.loans(), TODAY);
            assertEquals(condition == EquipmentCondition.GOOD
                    ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.UNAVAILABLE, availability);
        }
    }

    @Test
    void recoveryRestoresAnActiveLoanForACustodianToReturnLater() throws Exception {
        DatabaseDataStore store = storeWith(activeLoan(), EquipmentCondition.GOOD);
        CustodianFulfilmentService service = service(store, Role.CUSTODIAN);

        Loan lost = service.markLost("loan-1");
        LoanDeskData lostData = store.loadOrSeed();
        assertEquals(LoanStatus.LOST, lost.status());
        assertEquals(EquipmentCondition.LOST, lostData.equipment().getFirst().condition());
        assertEquals(AvailabilityStatus.UNAVAILABLE, new AvailabilityService().calculate(
                lostData.equipment().getFirst(), lostData.requests(), lostData.loans(), TODAY));
        assertTrue(BorrowerEligibilityService.evaluate(lostData, "borrower", TODAY)
                .blockers().contains(loandesk.application.EligibilityBlocker.LOST_LOAN));

        Loan recovered = service.recoverLost("loan-1");
        LoanDeskData reloaded = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"))
                .loadOrSeed();
        assertEquals(LoanStatus.ACTIVE, recovered.status());
        assertEquals(null, recovered.returnedDate());
        assertEquals(EquipmentCondition.GOOD, reloaded.equipment().getFirst().condition());
        assertEquals(AvailabilityStatus.ON_LOAN, new AvailabilityService().calculate(
                reloaded.equipment().getFirst(), reloaded.requests(), reloaded.loans(), TODAY));
        assertFalse(BorrowerEligibilityService.evaluate(reloaded, "borrower", TODAY)
                .blockers().contains(loandesk.application.EligibilityBlocker.LOST_LOAN));
        assertEquals(List.of("loan-1"), service.activeLoans().stream().map(Loan::loanId).toList());
    }

    @Test
    void onlyCustodiansCanViewOrChangeFulfilmentAndOnlyLegalStatesAreAccepted() throws Exception {
        DatabaseDataStore store = storeWith(activeLoan(), EquipmentCondition.GOOD);
        for (Role role : List.of(Role.BORROWER, Role.SUPERVISOR)) {
            CustodianFulfilmentService service = service(store, role);
            assertThrows(IllegalStateException.class, service::activeLoans);
            assertThrows(IllegalStateException.class,
                    () -> service.returnLoan("loan-1", EquipmentCondition.GOOD));
            assertThrows(IllegalStateException.class, () -> service.markLost("loan-1"));
            assertThrows(IllegalStateException.class, () -> service.recoverLost("loan-1"));
        }
        assertThrows(IllegalStateException.class,
                () -> new CustodianFulfilmentService(store, new Session(), CLOCK).activeLoans());
        assertThrows(IllegalArgumentException.class,
                () -> service(store, Role.CUSTODIAN).returnLoan("loan-1", EquipmentCondition.LOST));

        DatabaseDataStore returnedStore = storeWith(returnedLoan(), EquipmentCondition.GOOD);
        assertThrows(IllegalStateException.class,
                () -> service(returnedStore, Role.CUSTODIAN).returnLoan("loan-1", EquipmentCondition.GOOD));
        assertThrows(IllegalStateException.class,
                () -> service(returnedStore, Role.CUSTODIAN).markLost("loan-1"));
        assertThrows(IllegalStateException.class,
                () -> service(returnedStore, Role.CUSTODIAN).recoverLost("loan-1"));
    }

    @Test
    void failedOrStaleSavesLeaveReturnLossAndRecoveryUnchanged() throws Exception {
        LoanDeskData active = data(activeLoan(), EquipmentCondition.GOOD);
        DataStore failing = new FailingDataStore(active);
        assertThrows(IOException.class, () -> service(failing, Role.CUSTODIAN)
                .returnLoan("loan-1", EquipmentCondition.DAMAGED));
        assertEquals(active, failing.loadOrSeed());

        LoanDeskData lost = data(lostLoan(), EquipmentCondition.LOST);
        DataStore stale = new StaleDataStore(lost);
        assertThrows(StaleDataException.class,
                () -> service(stale, Role.CUSTODIAN).recoverLost("loan-1"));
        assertEquals(lost, stale.loadOrSeed());
    }

    @Test
    void databaseFailureDuringSnapshotWriteRollsBackTheReturn() throws Exception {
        DatabaseDataStore store = storeWith(activeLoan(), EquipmentCondition.GOOD);
        String jdbcUrl = "jdbc:h2:file:" + temporaryDirectory.resolve("loandesk")
                .toAbsolutePath().normalize().toString().replace('\\', '/');
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE equipment ADD CONSTRAINT reject_damaged_return "
                    + "CHECK (equipment_condition <> 'DAMAGED')");
        }

        assertThrows(IOException.class, () -> service(store, Role.CUSTODIAN)
                .returnLoan("loan-1", EquipmentCondition.DAMAGED));

        LoanDeskData reloaded = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"))
                .loadOrSeed();
        assertEquals(LoanStatus.ACTIVE, reloaded.loans().getFirst().status());
        assertEquals(EquipmentCondition.GOOD, reloaded.equipment().getFirst().condition());
        assertEquals(RequestStatus.COLLECTED, reloaded.requests().getFirst().status());
        assertEquals("loan-1", reloaded.requests().getFirst().loanId());
    }

    private DatabaseDataStore storeWith(Loan loan, EquipmentCondition condition) throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        store.save(data(loan, condition));
        return store;
    }

    private static LoanDeskData data(Loan loan, EquipmentCondition condition) {
        LoanRequest request = new LoanRequest("request-1", "borrower", "equipment-1", "Coursework",
                TODAY.minusDays(7), TODAY.plusDays(7), RequestStatus.COLLECTED, "loan-1",
                NOW, NOW, "supervisor", NOW, null, null, null, null);
        return new LoanDeskData(List.of(new User("borrower", Role.BORROWER),
                new User("supervisor", Role.SUPERVISOR), new User("custodian", Role.CUSTODIAN)), List.of(),
                List.of(new Equipment("equipment-1", "Camera", condition)), List.of(request), List.of(loan));
    }

    private static Loan activeLoan() {
        return new Loan("loan-1", "request-1", "borrower", "equipment-1", TODAY.minusDays(7),
                TODAY.plusDays(7), null, LoanStatus.ACTIVE);
    }

    private static Loan returnedLoan() {
        return new Loan("loan-1", "request-1", "borrower", "equipment-1", TODAY.minusDays(7),
                TODAY.plusDays(7), TODAY, LoanStatus.RETURNED);
    }

    private static Loan lostLoan() {
        return new Loan("loan-1", "request-1", "borrower", "equipment-1", TODAY.minusDays(7),
                TODAY.plusDays(7), null, LoanStatus.LOST);
    }

    private static CustodianFulfilmentService service(DataStore store, Role role) {
        Session session = new Session();
        session.start(new User(role.name().toLowerCase(), role));
        return new CustodianFulfilmentService(store, session, CLOCK);
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

    private static final class StaleDataStore implements DataStore {
        private final LoanDeskData data;

        private StaleDataStore(LoanDeskData data) {
            this.data = data;
        }

        @Override
        public LoanDeskData loadOrSeed() {
            return data;
        }

        @Override
        public void save(LoanDeskData ignored) throws StaleDataException {
            throw new StaleDataException("LoanDesk data changed; refresh and retry.");
        }
    }
}
