package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import loandesk.application.Session;
import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.LoanRequest;
import loandesk.domain.Role;
import loandesk.domain.RequestStatus;
import loandesk.domain.User;
import loandesk.persistence.DataStore;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class BorrowerRequestServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-25T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void submitsAndPersistsPendingRequest() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));

        var request = service(store).submitRequest(
                "camera1", " Academic project ", TODAY, TODAY.plusDays(14));

        assertEquals("borrower", request.borrowerUsername());
        assertEquals("Academic project", request.purpose());
        assertEquals(loandesk.domain.RequestStatus.PENDING, request.status());
        assertEquals(List.of(request), store.loadOrSeed().requests());
    }

    @Test
    void allowsSameDayRequestButRejectsPeriodLongerThanFourteenDays() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        BorrowerRequestService service = service(store);

        service.submitRequest("camera1", "Academic project", TODAY, TODAY);

        assertThrows(IllegalArgumentException.class, () -> service.submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(16)));
    }

    @Test
    void rejectsPastStartDateBlankPurposeAndUnknownEquipment() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        BorrowerRequestService service = service(store);

        assertThrows(IllegalArgumentException.class, () -> service.submitRequest(
                "camera1", "Academic project", TODAY.minusDays(1), TODAY));
        assertThrows(IllegalArgumentException.class, () -> service.submitRequest(
                "camera1", "   ", TODAY, TODAY));
        assertThrows(IllegalArgumentException.class, () -> service.submitRequest(
                "missing", "Academic project", TODAY, TODAY));
    }

    @Test
    void rejectsDuplicatePendingRequestForTheSameEquipment() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        BorrowerRequestService service = service(store);
        service.submitRequest("camera1", "Academic project", TODAY, TODAY);

        assertThrows(IllegalStateException.class, () -> service.submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(2)));
    }

    @Test
    void rejectsEquipmentThatIsNotAvailable() throws Exception {
        DatabaseDataStore store = storeWith(List.of(
                new Equipment("camera1", "Camera 1", EquipmentCondition.DAMAGED)));

        assertThrows(IllegalStateException.class, () -> service(store).submitRequest(
                "camera1", "Academic project", TODAY, TODAY));
    }

    @Test
    void surfacesSaveFailureWithoutMutatingTheLoadedSnapshot() throws Exception {
        LoanDeskData original = new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")));
        FailingDataStore store = new FailingDataStore(original);

        assertThrows(IOException.class, () -> new BorrowerRequestService(
                store, borrowerSession(), CLOCK).submitRequest(
                        "camera1", "Academic project", TODAY, TODAY));
        assertEquals(original, store.loadOrSeed());
    }

    @Test
    void rejectsLoggedOutAndWrongRoleCalls() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        Session loggedOut = new Session();
        Session supervisor = new Session();
        supervisor.start(new User("supervisor", Role.SUPERVISOR));

        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, loggedOut, CLOCK).submitRequest("camera1", "Academic project", TODAY, TODAY));
        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, supervisor, CLOCK).submitRequest("camera1", "Academic project", TODAY, TODAY));
    }

    @Test
    void listsOnlyOwnRequestsWithActiveRequestsBeforeHistory() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        LoanRequest ownHistory = request(
                "history", "borrower", RequestStatus.REJECTED,
                NOW.minusSeconds(30), "supervisor", "Not available");
        LoanRequest foreignActive = request(
                "foreign", "other", RequestStatus.PENDING,
                NOW.plusSeconds(10), null, null);
        LoanRequest ownActive = request(
                "active", "borrower", RequestStatus.PENDING,
                NOW, null, null);
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER), new User("other", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(ownHistory, foreignActive, ownActive),
                List.of()));

        BorrowerRequestService service = service(store);
        assertEquals(List.of("active", "history"), service.listOwnRequests().stream()
                .map(LoanRequest::requestId).toList());
        assertEquals(ownActive, service.findOwnRequest("active"));
        assertThrows(IllegalArgumentException.class, () -> service.findOwnRequest("foreign"));
        assertThrows(IllegalArgumentException.class, () -> service.findOwnRequest("missing"));
    }

    @Test
    void reloadsOwnRequestsFromAFreshStoreInstance() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        LoanRequest saved = service(store).submitRequest(
                "camera1", "Academic project", TODAY, TODAY.plusDays(14));

        DatabaseDataStore restartedStore = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        assertEquals(List.of(saved), new BorrowerRequestService(
                restartedStore, borrowerSession(), CLOCK).listOwnRequests());
    }

    @Test
    void requestQueriesRequireBorrowerSession() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        Session loggedOut = new Session();
        Session supervisor = new Session();
        supervisor.start(new User("supervisor", Role.SUPERVISOR));

        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, loggedOut, CLOCK).listOwnRequests());
        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, supervisor, CLOCK).listOwnRequests());
    }

    private LoanRequest request(
            String id,
            String borrower,
            RequestStatus status,
            Instant updatedAt,
            String decisionBy,
            String decisionReason) {
        Instant createdAt = updatedAt.minusSeconds(1);
        return new LoanRequest(
                id,
                borrower,
                "camera1",
                "Academic project",
                TODAY,
                TODAY.plusDays(14),
                status,
                null,
                createdAt,
                updatedAt,
                decisionBy,
                decisionBy == null ? null : updatedAt,
                decisionReason,
                null,
                null,
                null);
    }

    private DatabaseDataStore storeWith(List<Equipment> equipment) throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                equipment));
        return store;
    }

    private BorrowerRequestService service(DatabaseDataStore store) {
        return new BorrowerRequestService(store, borrowerSession(), CLOCK);
    }

    private Session borrowerSession() {
        Session session = new Session();
        session.start(new User("borrower", Role.BORROWER));
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
