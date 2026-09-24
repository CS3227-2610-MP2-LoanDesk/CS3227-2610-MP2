package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.CatalogueService;
import loandesk.application.Session;
import loandesk.domain.Equipment;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class CatalogueServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsEquipmentFromTheSharedDatabase() throws Exception {
        CatalogueService service = catalogueService(temporaryDirectory.resolve("loandesk"));

        assertEquals(List.of(
                new Equipment("camera1", "Camera 1"),
                new Equipment("camera2", "Camera 2")), service.loadCatalogue());
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
