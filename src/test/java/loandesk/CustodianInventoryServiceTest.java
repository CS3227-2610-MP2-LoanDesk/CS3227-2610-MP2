package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.CustodianInventoryService;
import loandesk.application.Session;
import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DataStore;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class CustodianInventoryServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-10T09:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void addsAndEditsEquipmentWithoutChangingExistingReferencesAndPersistsAfterReload()
            throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore store = new DatabaseDataStore(databasePath);
        LoanDeskData initial = store.loadOrSeed();
        List<User> users = new ArrayList<>(initial.users());
        users.add(new User("borrower", Role.BORROWER));
        LoanDeskData referenced = new LoanDeskData(users, initial.credentials(),
                List.of(new Equipment("equipment-1", "Original camera")),
                List.of(CustodianTestFixtures.approvedRequest()), List.of());
        store.save(referenced);
        CustodianInventoryService service = service(store, Role.CUSTODIAN);

        Equipment added = service.addEquipment("  Tripod  ");
        Equipment updated = service.updateEquipment("equipment-1", "Updated camera",
                EquipmentCondition.UNDER_MAINTENANCE);
        LoanDeskData reloaded = new DatabaseDataStore(databasePath).loadOrSeed();

        assertEquals("Tripod", added.name());
        assertEquals(new Equipment("equipment-1", "Updated camera", EquipmentCondition.UNDER_MAINTENANCE),
                updated);
        assertEquals(List.of(new Equipment("equipment-1", "Updated camera",
                EquipmentCondition.UNDER_MAINTENANCE), added),
                reloaded.equipment());
        assertEquals("equipment-1", reloaded.requests().getFirst().equipmentId());
    }

    @Test
    void rejectsInvalidInventoryChangesWithoutMutation() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        LoanDeskData before = store.loadOrSeed();
        CustodianInventoryService service = service(store, Role.CUSTODIAN);

        assertThrows(IllegalArgumentException.class, () -> service.addEquipment(" "));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateEquipment("unknown", "Camera", EquipmentCondition.GOOD));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateEquipment("camera1", " ", EquipmentCondition.GOOD));
        assertEquals(before, store.loadOrSeed());
    }

    @Test
    void generatedIdsAllowSeparateRecordsWithTheSameName() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        CustodianInventoryService service = service(store, Role.CUSTODIAN);

        Equipment first = service.addEquipment("Tripod");
        Equipment second = service.addEquipment("Tripod");

        assertNotEquals(first.id(), second.id());
        assertEquals(2, service.inventory().stream()
                .map(CustodianInventoryService.InventoryItem::equipment)
                .filter(equipment -> equipment.name().equals("Tripod"))
                .count());
    }

    @Test
    void onlyCustodiansCanReadOrChangeInventory() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        for (Role role : List.of(Role.BORROWER, Role.SUPERVISOR)) {
            CustodianInventoryService service = service(store, role);
            assertThrows(IllegalStateException.class, service::inventory);
            assertThrows(IllegalStateException.class, () -> service.addEquipment("New item"));
            assertThrows(IllegalStateException.class,
                    () -> service.updateEquipment("camera1", "Camera", EquipmentCondition.GOOD));
        }
        CustodianInventoryService signedOut = new CustodianInventoryService(store, new Session(), CLOCK);
        assertThrows(IllegalStateException.class, signedOut::inventory);
    }

    @Test
    void conditionChangesImmediatelyUpdateDerivedAvailability() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        CustodianInventoryService service = service(store, Role.CUSTODIAN);

        service.updateEquipment("camera1", "Camera 1", EquipmentCondition.DAMAGED);
        assertEquals(AvailabilityStatus.UNAVAILABLE, availabilityOf(service, "camera1"));
        service.updateEquipment("camera1", "Camera 1", EquipmentCondition.GOOD);
        assertEquals(AvailabilityStatus.AVAILABLE, availabilityOf(service, "camera1"));
    }

    @Test
    void failedSaveLeavesInventoryUnchanged() throws Exception {
        LoanDeskData before = new LoanDeskData(List.of(new User("custodian", Role.CUSTODIAN)), List.of(),
                List.of(new Equipment("camera-1", "Camera")), List.of(), List.of());
        DataStore failing = new FailingDataStore(before);
        CustodianInventoryService service = service(failing, Role.CUSTODIAN);

        assertThrows(IOException.class, () -> service.addEquipment("Tripod"));
        assertThrows(IOException.class, () -> service.updateEquipment("camera-1", "Changed",
                EquipmentCondition.DAMAGED));
        assertEquals(before, failing.loadOrSeed());
    }

    private static AvailabilityStatus availabilityOf(CustodianInventoryService service, String equipmentId)
            throws Exception {
        return service.inventory().stream()
                .filter(item -> item.equipment().id().equals(equipmentId))
                .findFirst()
                .orElseThrow()
                .availability();
    }

    private static CustodianInventoryService service(DataStore store, Role role) {
        Session session = new Session();
        session.start(new User(role.name().toLowerCase(), role));
        return new CustodianInventoryService(store, session, CLOCK);
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
