package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.CatalogueService;
import loandesk.domain.Equipment;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class CatalogueServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsEquipmentFromTheSharedDatabase() throws Exception {
        CatalogueService service = new CatalogueService(
                new DatabaseDataStore(temporaryDirectory.resolve("loandesk")));

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

        CatalogueService recreatedService = new CatalogueService(new DatabaseDataStore(databasePath));

        assertEquals(expectedEquipment, recreatedService.loadCatalogue());
    }

    @Test
    void filtersNamesCaseInsensitivelyAndTrimsTheQuery() {
        CatalogueService service = new CatalogueService(
                new DatabaseDataStore(temporaryDirectory.resolve("loandesk")));
        List<Equipment> equipment = List.of(
                new Equipment("camera1", "Camera 1"),
                new Equipment("tripod1", "Tripod 1"));

        assertEquals(List.of(new Equipment("camera1", "Camera 1")),
                service.filterByName(equipment, "  CAMERA  "));
    }

    @Test
    void blankOrNullFilterReturnsAllEquipment() {
        CatalogueService service = new CatalogueService(
                new DatabaseDataStore(temporaryDirectory.resolve("loandesk")));
        List<Equipment> equipment = List.of(
                new Equipment("camera1", "Camera 1"),
                new Equipment("tripod1", "Tripod 1"));

        assertEquals(equipment, service.filterByName(equipment, "   "));
        assertEquals(equipment, service.filterByName(equipment, null));
    }

    @Test
    void unmatchedFilterReturnsNoEquipment() {
        CatalogueService service = new CatalogueService(
                new DatabaseDataStore(temporaryDirectory.resolve("loandesk")));

        assertTrue(service.filterByName(
                List.of(new Equipment("camera1", "Camera 1")), "microphone").isEmpty());
    }
}
