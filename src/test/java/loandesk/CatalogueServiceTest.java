package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.CatalogueService;
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
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class CatalogueServiceTest {
    private static final List<Equipment> SEEDED_EQUIPMENT = List.of(
            new Equipment("camera-dslr", "Canon EOS 90D DSLR Camera"),
            new Equipment("camera-mirrorless", "Sony Alpha a6400 Mirrorless Camera"),
            new Equipment("microphone-yeti", "Blue Yeti USB Microphone"),
            new Equipment("monitor-dell-u2723qe", "Dell UltraSharp U2723QE 27-inch Monitor"),
            new Equipment("monitor-lg-27up850", "LG UltraFine 27UP850 27-inch Monitor"),
            new Equipment("projector-epson-fh52", "Epson EB-FH52 Projector"),
            new Equipment("tablet-ipad-air", "Apple iPad Air 11-inch"),
            new Equipment("tripod-befree", "Manfrotto Befree Advanced Tripod"),
            new Equipment("webcam-brio", "Logitech Brio 4K Webcam"),
            new Equipment("webcam-c920", "Logitech C920 HD Pro Webcam"));

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsEquipmentFromTheSharedDatabase() throws Exception {
        CatalogueService service = catalogueService(temporaryDirectory.resolve("loandesk"));

        assertEquals(SEEDED_EQUIPMENT, service.loadCatalogue());
    }

    @Test
    void loadsBorrowerCatalogueWithDerivedAvailability() throws Exception {
        CatalogueService service = catalogueService(temporaryDirectory.resolve("loandesk"));

        assertEquals(SEEDED_EQUIPMENT.stream()
                        .map(equipment -> new CatalogueService.CatalogueItem(
                                equipment, AvailabilityStatus.AVAILABLE))
                        .toList(),
                service.loadCatalogueWithAvailability());
    }

    @Test
    void derivesEveryBorrowerVisibleAvailabilityStatus() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore store = new DatabaseDataStore(databasePath);
        LoanDeskData initialData = store.loadOrSeed();
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        LoanRequest reservation = new LoanRequest(
                "reservation", "testBorrower1", "reserved", "Coursework", today.plusDays(1),
                today.plusDays(2), RequestStatus.APPROVED, null,
                now, now, "supervisor", now, "Approved",
                null, null, null);
        LoanRequest collectedRequest = new LoanRequest(
                "request-on-loan", "testBorrower1", "on-loan", "Coursework", today,
                today.plusDays(2), RequestStatus.COLLECTED, "loan-on-loan",
                now, now, "supervisor", now, "Approved", null, null, null);
        Loan activeLoan = new Loan(
                "loan-on-loan", "request-on-loan", "testBorrower1", "on-loan", today,
                today.plusDays(2), null, LoanStatus.ACTIVE);
        store.save(new LoanDeskData(
                initialData.users(), initialData.credentials(), List.of(
                        new Equipment("available", "Available"),
                        new Equipment("reserved", "Reserved"),
                        new Equipment("on-loan", "On loan"),
                        new Equipment("unavailable", "Unavailable", EquipmentCondition.DAMAGED)),
                List.of(reservation, collectedRequest), List.of(activeLoan)));

        Map<String, AvailabilityStatus> availability = catalogueService(databasePath)
                .loadCatalogueWithAvailability().stream()
                .collect(java.util.stream.Collectors.toMap(
                        item -> item.equipment().id(), CatalogueService.CatalogueItem::availability));

        assertEquals(Map.of(
                "available", AvailabilityStatus.AVAILABLE,
                "reserved", AvailabilityStatus.RESERVED,
                "on-loan", AvailabilityStatus.ON_LOAN,
                "unavailable", AvailabilityStatus.UNAVAILABLE), availability);
    }

    @Test
    void rejectsAvailabilityLoadingWithoutBorrowerAccess() {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));

        assertThrows(IllegalStateException.class,
                () -> new CatalogueService(store, new Session()).loadCatalogueWithAvailability());
        Session supervisorSession = new Session();
        supervisorSession.start(new User("supervisor", Role.SUPERVISOR));
        assertThrows(IllegalStateException.class,
                () -> new CatalogueService(store, supervisorSession).loadCatalogueWithAvailability());
    }

    @Test
    void loadsTheCatalogueAfterDatabaseRecreation() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore firstStore = new DatabaseDataStore(databasePath);
        LoanDeskData initialData = firstStore.loadOrSeed();
        List<Equipment> expectedEquipment = List.of(
                new Equipment("tripod1", "Tripod 1"));
        firstStore.save(new LoanDeskData(
                initialData.users(), initialData.credentials(), expectedEquipment));

        CatalogueService recreatedService = catalogueService(databasePath);

        assertEquals(expectedEquipment, recreatedService.loadCatalogue());
    }

    @Test
    void rejectsCatalogueLoadingWithoutAnActiveSession() {
        CatalogueService service = new CatalogueService(
                new DatabaseDataStore(temporaryDirectory.resolve("loandesk")), new Session());

        assertThrows(IllegalStateException.class, service::loadCatalogue);
    }

    @Test
    void rejectsCatalogueLoadingForAnotherRole() {
        Session session = new Session();
        session.start(new User("supervisor", Role.SUPERVISOR));
        CatalogueService service = new CatalogueService(
                new DatabaseDataStore(temporaryDirectory.resolve("loandesk")), session);

        assertThrows(IllegalStateException.class, service::loadCatalogue);
    }

    @Test
    void rejectsCatalogueLoadingAfterTheBorrowerLogsOut() throws Exception {
        Session session = borrowerSession();
        CatalogueService service = new CatalogueService(
                new DatabaseDataStore(temporaryDirectory.resolve("loandesk")), session);
        service.loadCatalogue();

        session.clear();

        assertThrows(IllegalStateException.class, service::loadCatalogue);
    }

    @Test
    void filtersNamesCaseInsensitivelyAndTrimsTheQuery() {
        CatalogueService service = catalogueService(temporaryDirectory.resolve("loandesk"));
        List<Equipment> equipment = List.of(
                new Equipment("camera1", "Camera 1"),
                new Equipment("tripod1", "Tripod 1"));

        assertEquals(List.of(new Equipment("camera1", "Camera 1")),
                service.filterByName(equipment, "  CAMERA  "));
    }

    @Test
    void blankOrNullFilterReturnsAllEquipment() {
        CatalogueService service = catalogueService(temporaryDirectory.resolve("loandesk"));
        List<Equipment> equipment = List.of(
                new Equipment("camera1", "Camera 1"),
                new Equipment("tripod1", "Tripod 1"));

        assertEquals(equipment, service.filterByName(equipment, "   "));
        assertEquals(equipment, service.filterByName(equipment, null));
    }

    @Test
    void filtersAndSortsAvailableEquipmentBeforeOtherAvailabilityStatuses() {
        CatalogueService service = catalogueService(temporaryDirectory.resolve("loandesk"));
        CatalogueService.CatalogueItem availableCamera = new CatalogueService.CatalogueItem(
                new Equipment("camera1", "Camera"), AvailabilityStatus.AVAILABLE);
        CatalogueService.CatalogueItem availableTripod = new CatalogueService.CatalogueItem(
                new Equipment("tripod1", "Tripod"), AvailabilityStatus.AVAILABLE);
        CatalogueService.CatalogueItem onLoanAudio = new CatalogueService.CatalogueItem(
                new Equipment("audio1", "Audio recorder"), AvailabilityStatus.ON_LOAN);
        CatalogueService.CatalogueItem unavailableZebra = new CatalogueService.CatalogueItem(
                new Equipment("zebra1", "Zebra light"), AvailabilityStatus.UNAVAILABLE);

        assertEquals(List.of(availableCamera, availableTripod, onLoanAudio, unavailableZebra),
                service.filterAndSortByName(
                        List.of(unavailableZebra, onLoanAudio, availableTripod, availableCamera), ""));
        assertEquals(List.of(availableCamera), service.filterAndSortByName(
                List.of(onLoanAudio, availableTripod, availableCamera), " camera "));
    }

    @Test
    void unmatchedFilterReturnsNoEquipment() {
        CatalogueService service = catalogueService(temporaryDirectory.resolve("loandesk"));

        assertTrue(service.filterByName(
                List.of(new Equipment("camera1", "Camera 1")), "microphone").isEmpty());
    }

    private CatalogueService catalogueService(Path databasePath) {
        return new CatalogueService(new DatabaseDataStore(databasePath), borrowerSession());
    }

    private static Session borrowerSession() {
        Session session = new Session();
        session.start(new User("borrower", Role.BORROWER));
        return session;
    }
}
