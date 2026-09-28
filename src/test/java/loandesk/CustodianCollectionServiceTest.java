package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import loandesk.application.CustodianCollectionService;
import loandesk.application.Session;
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

class CustodianCollectionServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-02T09:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void checkoutCreatesOneReciprocallyLinkedActiveLoanAndPersistsIt() throws Exception {
        DatabaseDataStore store = storeWith(EquipmentCondition.GOOD, approved("request-1", TODAY), List.of());

        Loan loan = service(store, Role.CUSTODIAN).checkout("request-1");

        LoanDeskData reloaded = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"))
                .loadOrSeed();
        LoanRequest request = reloaded.requests().getFirst();
        assertEquals(LoanStatus.ACTIVE, loan.status());
        assertEquals(TODAY, loan.checkoutDate());
        assertEquals(RequestStatus.COLLECTED, request.status());
        assertEquals(loan.loanId(), request.loanId());
        assertEquals(List.of(loan), reloaded.loans());
        assertThrows(IllegalStateException.class, () -> service(store, Role.CUSTODIAN).checkout("request-1"));
    }

    @Test
    void queueShowsOnlyApprovedRequestsInsideTheCollectionWindowAndSweepsExpiry() throws Exception {
        DatabaseDataStore store = storeWith(EquipmentCondition.GOOD,
                approved("on-time", TODAY), List.of(approved("expired", TODAY.minusDays(4))));

        List<LoanRequest> queue = service(store, Role.CUSTODIAN).collectionsQueue();

        assertEquals(List.of("on-time"), queue.stream().map(LoanRequest::requestId).toList());
        assertEquals(RequestStatus.EXPIRED, new DatabaseDataStore(temporaryDirectory.resolve("loandesk"))
                .loadOrSeed().requests().stream().filter(request -> request.requestId().equals("expired"))
                .findFirst().orElseThrow().status());
    }

    @Test
    void rejectsEarlyLateAndNonApprovedRequestsWithoutCreatingALoan() throws Exception {
        for (LoanRequest request : List.of(
                approved("early", TODAY.plusDays(1)),
                approved("late", TODAY.minusDays(4)),
                request("pending", RequestStatus.PENDING),
                request("rejected", RequestStatus.REJECTED),
                request("cancelled", RequestStatus.CANCELLED))) {
            DatabaseDataStore store = storeWith(EquipmentCondition.GOOD, request, List.of());
            assertThrows(IllegalStateException.class,
                    () -> service(store, Role.CUSTODIAN).checkout(request.requestId()));
            assertTrue(new DatabaseDataStore(temporaryDirectory.resolve("loandesk"))
                    .loadOrSeed().loans().isEmpty());
        }
    }

    @Test
    void rejectsNonGoodReservedAndOnLoanEquipment() throws Exception {
        for (EquipmentCondition condition : List.of(EquipmentCondition.DAMAGED,
                EquipmentCondition.UNDER_MAINTENANCE, EquipmentCondition.LOST)) {
            DatabaseDataStore store = storeWith(condition, approved("request-1", TODAY), List.of());
            assertThrows(IllegalStateException.class,
                    () -> service(store, Role.CUSTODIAN).checkout("request-1"));
        }

        DatabaseDataStore reserved = storeWith(EquipmentCondition.GOOD, approved("request-1", TODAY),
                List.of(approved("other", TODAY)));
        assertThrows(IllegalStateException.class, () -> service(reserved, Role.CUSTODIAN).checkout("request-1"));

        LoanRequest collected = collectedRequest("collected", "loan-1");
        Loan active = new Loan("loan-1", "collected", "other", "equipment-1", TODAY,
                TODAY.plusDays(7), null, LoanStatus.ACTIVE);
        DatabaseDataStore onLoan = storeWith(EquipmentCondition.GOOD, approved("request-1", TODAY),
                List.of(collected), List.of(active));
        assertThrows(IllegalStateException.class, () -> service(onLoan, Role.CUSTODIAN).checkout("request-1"));
    }

    @Test
    void loggedOutAndOtherRolesCannotReadOrMutateCollections() throws Exception {
        DatabaseDataStore store = storeWith(EquipmentCondition.GOOD, approved("request-1", TODAY), List.of());
        for (Role role : List.of(Role.BORROWER, Role.SUPERVISOR)) {
            CustodianCollectionService service = service(store, role);
            assertThrows(IllegalStateException.class, service::collectionsQueue);
            assertThrows(IllegalStateException.class, () -> service.checkout("request-1"));
        }
        CustodianCollectionService signedOut = new CustodianCollectionService(store, new Session(), CLOCK);
        assertThrows(IllegalStateException.class, signedOut::collectionsQueue);
        assertTrue(store.loadOrSeed().loans().isEmpty());
    }

    @Test
    void failedAndStaleSavesLeaveNoPartialCheckout() throws Exception {
        LoanDeskData original = data(EquipmentCondition.GOOD, List.of(approved("request-1", TODAY)), List.of());
        DataStore failing = new FailingDataStore(original);
        assertThrows(IOException.class, () -> service(failing, Role.CUSTODIAN).checkout("request-1"));
        assertEquals(original, failing.loadOrSeed());

        DataStore stale = new StaleDataStore(original);
        assertThrows(StaleDataException.class, () -> service(stale, Role.CUSTODIAN).checkout("request-1"));
        assertEquals(original, stale.loadOrSeed());
    }

    private DatabaseDataStore storeWith(
            EquipmentCondition condition, LoanRequest request, List<LoanRequest> additional) throws Exception {
        return storeWith(condition, request, additional, List.of());
    }

    private DatabaseDataStore storeWith(
            EquipmentCondition condition, LoanRequest request, List<LoanRequest> additional, List<Loan> loans)
            throws Exception {
        List<LoanRequest> requests = new java.util.ArrayList<>();
        requests.add(request);
        requests.addAll(additional);
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        store.save(data(condition, requests, loans));
        return store;
    }

    private static LoanDeskData data(
            EquipmentCondition condition, List<LoanRequest> requests, List<Loan> loans) {
        return new LoanDeskData(List.of(new User("borrower", Role.BORROWER),
                new User("other", Role.BORROWER), new User("supervisor", Role.SUPERVISOR),
                new User("custodian", Role.CUSTODIAN)), List.of(),
                List.of(new Equipment("equipment-1", "Camera", condition)), requests, loans);
    }

    private static CustodianCollectionService service(DataStore store, Role role) {
        Session session = new Session();
        if (role != null) {
            session.start(new User(switch (role) {
                case BORROWER -> "borrower";
                case SUPERVISOR -> "supervisor";
                case CUSTODIAN -> "custodian";
            }, role));
        }
        return new CustodianCollectionService(store, session, CLOCK);
    }

    private static LoanRequest approved(String id, LocalDate startDate) {
        return new LoanRequest(id, id.equals("other") ? "other" : "borrower", "equipment-1",
                "Coursework", startDate, startDate.plusDays(7), RequestStatus.APPROVED, null,
                NOW, NOW, "supervisor", NOW, null, null, null, null);
    }

    private static LoanRequest request(String id, RequestStatus status) {
        if (status == RequestStatus.PENDING) {
            return new LoanRequest(id, "borrower", "equipment-1", "Coursework", TODAY,
                    TODAY.plusDays(7), status, null, NOW, NOW, null, null, null, null, null, null);
        }
        if (status == RequestStatus.CANCELLED) {
            return new LoanRequest(id, "borrower", "equipment-1", "Coursework", TODAY,
                    TODAY.plusDays(7), status, null, NOW, NOW, null, null, null, "borrower", NOW,
                    "Changed plans");
        }
        return new LoanRequest(id, "borrower", "equipment-1", "Coursework", TODAY,
                TODAY.plusDays(7), status, null, NOW, NOW, "supervisor", NOW, "No", null, null, null);
    }

    private static LoanRequest collectedRequest(String id, String loanId) {
        return new LoanRequest(id, "other", "equipment-1", "Coursework", TODAY,
                TODAY.plusDays(7), RequestStatus.COLLECTED, loanId, NOW, NOW, "supervisor", NOW,
                null, null, null, null);
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
