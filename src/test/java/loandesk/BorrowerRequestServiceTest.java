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
import loandesk.application.AvailabilityService;
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
        Session custodian = new Session();
        custodian.start(new User("custodian", Role.CUSTODIAN));

        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, loggedOut, CLOCK).submitRequest("camera1", "Academic project", TODAY, TODAY));
        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, supervisor, CLOCK).submitRequest("camera1", "Academic project", TODAY, TODAY));
        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, loggedOut, CLOCK).cancelRequest("missing", "Plans changed"));
        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, supervisor, CLOCK).cancelRequest("missing", "Plans changed"));
        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, loggedOut, CLOCK).editRequest(
                        "missing", "Changed", TODAY.plusDays(1), TODAY.plusDays(2)));
        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, supervisor, CLOCK).editRequest(
                        "missing", "Changed", TODAY.plusDays(1), TODAY.plusDays(2)));
        assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                store, custodian, CLOCK).editRequest(
                        "missing", "Changed", TODAY.plusDays(1), TODAY.plusDays(2)));
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

    @Test
    void cancelsEligibleFuturePendingRequestAndPersistsReason() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        BorrowerRequestService service = service(store);
        LoanRequest submitted = service.submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(2));

        LoanRequest cancelled = service.cancelRequest(submitted.requestId(), "Plans changed");

        assertEquals(RequestStatus.CANCELLED, cancelled.status());
        assertEquals("borrower", cancelled.cancelledBy());
        assertEquals("Plans changed", cancelled.cancellationReason());
        assertEquals(List.of(cancelled), store.loadOrSeed().requests());
        assertThrows(IllegalStateException.class, () -> service.cancelRequest(
                submitted.requestId(), "Plans changed"));
    }

    @Test
    void rejectsSameDayTerminalForeignUnknownAndBlankCancellation() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        BorrowerRequestService service = service(store);
        LoanRequest sameDay = service.submitRequest("camera1", "Academic project", TODAY, TODAY);

        assertThrows(IllegalStateException.class, () -> service.cancelRequest(
                sameDay.requestId(), "No longer needed"));
        assertThrows(IllegalArgumentException.class, () -> service.cancelRequest(
                sameDay.requestId(), "   "));
        assertThrows(IllegalArgumentException.class, () -> service.cancelRequest(
                "missing", "No longer needed"));

        Session otherBorrower = new Session();
        otherBorrower.start(new User("other", Role.BORROWER));
        assertThrows(IllegalArgumentException.class, () -> new BorrowerRequestService(
                store, otherBorrower, CLOCK).cancelRequest(
                        sameDay.requestId(), "No longer needed"));
    }

    @Test
    void cancelsEligibleFutureApprovedRequest() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        LoanRequest approved = new LoanRequest(
                "approved",
                "borrower",
                "camera1",
                "Academic project",
                TODAY.plusDays(1),
                TODAY.plusDays(2),
                RequestStatus.APPROVED,
                null,
                NOW.minusSeconds(2),
                NOW.minusSeconds(1),
                "supervisor",
                NOW.minusSeconds(1),
                "Approved for project",
                null,
                null,
                null);
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(approved),
                List.of()));

        LoanRequest cancelled = service(store).cancelRequest(
                "approved", "No longer needed");

        assertEquals(RequestStatus.CANCELLED, cancelled.status());
        assertEquals("supervisor", cancelled.decisionBy());
        assertEquals("Approved for project", cancelled.decisionReason());

        DatabaseDataStore restartedStore = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        LoanRequest reloaded = restartedStore.loadOrSeed().requests().get(0);
        assertEquals(cancelled, reloaded);
        assertEquals(loandesk.domain.AvailabilityStatus.AVAILABLE,
                new AvailabilityService().calculate(
                        restartedStore.loadOrSeed().equipment().get(0),
                        restartedStore.loadOrSeed().requests(),
                        restartedStore.loadOrSeed().loans(),
                        TODAY));
    }

    @Test
    void rejectsRejectedAndExpiredRequests() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        LoanRequest rejected = request(
                "rejected", "borrower", RequestStatus.REJECTED,
                NOW, "supervisor", "Not available");
        LoanRequest expired = request(
                "expired", "borrower", RequestStatus.EXPIRED,
                NOW, null, null);
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(rejected, expired),
                List.of()));

        BorrowerRequestService service = service(store);
        assertThrows(IllegalStateException.class, () -> service.cancelRequest(
                "rejected", "Plans changed"));
        assertThrows(IllegalStateException.class, () -> service.cancelRequest(
                "expired", "Plans changed"));
    }

    @Test
    void editsOwnedPendingRequestAndReloadsUpdatedFields() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        LoanRequest submitted = service(store).submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(14));

        LoanRequest edited = service(store).editRequest(
                submitted.requestId(), "Research or lab work",
                TODAY.plusDays(2), TODAY.plusDays(5));

        assertEquals(submitted.requestId(), edited.requestId());
        assertEquals(submitted.equipmentId(), edited.equipmentId());
        assertEquals(RequestStatus.PENDING, edited.status());
        assertEquals("Research or lab work", edited.purpose());
        assertEquals(TODAY.plusDays(2), edited.startDate());
        assertEquals(TODAY.plusDays(5), edited.dueDate());
        assertEquals(List.of(edited), store.loadOrSeed().requests());

        DatabaseDataStore restartedStore = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        assertEquals(List.of(edited), new BorrowerRequestService(
                restartedStore, borrowerSession(), CLOCK).listOwnRequests());
    }

    @Test
    void rejectsEditsForForeignUnknownTerminalAndStartedRequests() throws Exception {
        DatabaseDataStore store = storeWith(List.of(new Equipment("camera1", "Camera 1")));
        LoanRequest pending = service(store).submitRequest(
                "camera1", "Academic project", TODAY.plusDays(1), TODAY.plusDays(2));

        Session otherBorrower = new Session();
        otherBorrower.start(new User("other", Role.BORROWER));
        assertThrows(IllegalArgumentException.class, () -> new BorrowerRequestService(
                store, otherBorrower, CLOCK).editRequest(
                        pending.requestId(), "Changed", TODAY.plusDays(1), TODAY.plusDays(2)));
        assertThrows(IllegalArgumentException.class, () -> service(store).editRequest(
                "missing", "Changed", TODAY.plusDays(1), TODAY.plusDays(2)));
        assertThrows(IllegalArgumentException.class, () -> service(store).editRequest(
                pending.requestId(), "Changed", TODAY.minusDays(1), TODAY));
        assertThrows(IllegalArgumentException.class, () -> service(store).editRequest(
                pending.requestId(), "Changed", TODAY.plusDays(1), TODAY.plusDays(16)));

        LoanRequest started = new LoanRequest(
                "started",
                "borrower",
                "camera1",
                "Academic project",
                TODAY,
                TODAY.plusDays(1),
                RequestStatus.PENDING,
                null,
                NOW.minusSeconds(2),
                NOW.minusSeconds(1),
                null,
                null,
                null,
                null,
                null,
                null);
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(started),
                List.of()));
        assertThrows(IllegalStateException.class, () -> service(store).editRequest(
                "started", "Changed", TODAY, TODAY.plusDays(1)));

        LoanRequest rejected = request(
                "rejected", "borrower", RequestStatus.REJECTED,
                NOW, "supervisor", "Not available");
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(rejected),
                List.of()));
        assertThrows(IllegalStateException.class, () -> service(store).editRequest(
                "rejected", "Changed", TODAY.plusDays(1), TODAY.plusDays(2)));
    }

    @Test
    void rejectsEveryNonPendingEditState() throws Exception {
        for (RequestStatus status : List.of(
                RequestStatus.APPROVED,
                RequestStatus.REJECTED,
                RequestStatus.CANCELLED,
                RequestStatus.EXPIRED,
                RequestStatus.COLLECTED)) {
            LoanRequest request = requestForEditState(status);
            LoanDeskData original = new LoanDeskData(
                    List.of(new User("borrower", Role.BORROWER)),
                    List.of(),
                    List.of(new Equipment("camera1", "Camera 1")),
                    List.of(request),
                    List.of());
            FailingDataStore store = new FailingDataStore(original);

            assertThrows(IllegalStateException.class, () -> new BorrowerRequestService(
                    store, borrowerSession(), CLOCK).editRequest(
                            request.requestId(), "Changed", TODAY.plusDays(1), TODAY.plusDays(2)),
                    "Expected edit rejection for " + status);
            assertEquals(original, store.loadOrSeed());
        }
    }

    @Test
    void failedEditSavePreservesPreviousRequestState() throws Exception {
        LoanRequest pending = new LoanRequest(
                "pending",
                "borrower",
                "camera1",
                "Academic project",
                TODAY.plusDays(1),
                TODAY.plusDays(2),
                RequestStatus.PENDING,
                null,
                NOW.minusSeconds(1),
                NOW,
                null,
                null,
                null,
                null,
                null,
                null);
        LoanDeskData original = new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(pending),
                List.of());
        FailingDataStore store = new FailingDataStore(original);

        assertThrows(IOException.class, () -> new BorrowerRequestService(
                store, borrowerSession(), CLOCK).editRequest(
                        "pending", "Changed", TODAY.plusDays(2), TODAY.plusDays(3)));
        assertEquals(original, store.loadOrSeed());
    }

    @Test
    void failedCancellationSavePreservesPreviousRequestState() throws Exception {
        LoanRequest pending = new LoanRequest(
                "pending",
                "borrower",
                "camera1",
                "Academic project",
                TODAY.plusDays(1),
                TODAY.plusDays(2),
                RequestStatus.PENDING,
                null,
                NOW.minusSeconds(1),
                NOW,
                null,
                null,
                null,
                null,
                null,
                null);
        LoanDeskData original = new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(pending),
                List.of());
        FailingDataStore store = new FailingDataStore(original);

        assertThrows(IOException.class, () -> new BorrowerRequestService(
                store, borrowerSession(), CLOCK).cancelRequest(
                        "pending", "Plans changed"));
        assertEquals(original, store.loadOrSeed());
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

    private LoanRequest requestForEditState(RequestStatus status) {
        Instant updatedAt = NOW.minusSeconds(1);
        return switch (status) {
            case APPROVED -> new LoanRequest(
                    "approved", "borrower", "camera1", "Academic project",
                    TODAY.plusDays(1), TODAY.plusDays(2), status, null,
                    NOW.minusSeconds(2), updatedAt, "supervisor", updatedAt,
                    "Approved", null, null, null);
            case REJECTED -> request(
                    "rejected", "borrower", status, updatedAt,
                    "supervisor", "Not available");
            case CANCELLED -> new LoanRequest(
                    "cancelled", "borrower", "camera1", "Academic project",
                    TODAY.plusDays(1), TODAY.plusDays(2), status, null,
                    NOW.minusSeconds(2), updatedAt, null, null, null,
                    "borrower", updatedAt, "Plans changed");
            case EXPIRED -> request(
                    "expired", "borrower", status, updatedAt, null, null);
            case COLLECTED -> new LoanRequest(
                    "collected", "borrower", "camera1", "Academic project",
                    TODAY.plusDays(1), TODAY.plusDays(2), status, "loan-1",
                    NOW.minusSeconds(2), updatedAt, "supervisor", updatedAt,
                    "Approved", null, null, null);
            case PENDING -> throw new IllegalArgumentException("Pending is an editable state.");
        };
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
