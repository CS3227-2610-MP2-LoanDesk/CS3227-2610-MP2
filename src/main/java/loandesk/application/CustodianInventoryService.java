package loandesk.application;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;
import loandesk.persistence.StaleDataException;

/** Custodian-only management of the minimal shared equipment inventory. */
public final class CustodianInventoryService {
    private final DataStore dataStore;
    private final PermissionService permissions;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    public CustodianInventoryService(DataStore dataStore, Session session) {
        this(dataStore, session, Clock.systemDefaultZone());
    }

    public CustodianInventoryService(DataStore dataStore, Session session, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.permissions = new PermissionService(Objects.requireNonNull(session));
        this.clock = Objects.requireNonNull(clock);
        this.availabilityService = new AvailabilityService();
    }

    /** Returns equipment with its availability calculated from the shared state. */
    public List<InventoryItem> inventory() throws IOException {
        permissions.require(Permission.MANAGE_EQUIPMENT);
        LoanDeskData data = dataStore.loadOrSeed();
        LocalDate today = LocalDate.now(clock);
        return data.equipment().stream()
                .sorted(Comparator.comparing(Equipment::id))
                .map(equipment -> new InventoryItem(equipment, availabilityService.calculate(
                        equipment, data.requests(), data.loans(), today)))
                .toList();
    }

    /** Adds one equipment item with a generated immutable ID in GOOD condition. */
    public Equipment addEquipment(String name) throws IOException {
        return addEquipment(name, EquipmentCondition.GOOD);
    }

    /** Adds one equipment item with a generated immutable ID and observed initial condition. */
    public Equipment addEquipment(String name, EquipmentCondition condition) throws IOException {
        permissions.require(Permission.MANAGE_EQUIPMENT);
        if (condition == null) {
            throw new IllegalArgumentException("Equipment condition is required.");
        }
        LoanDeskData data = dataStore.loadOrSeed();
        Equipment added = new Equipment(nextEquipmentId(data), requireText(name, "Equipment name"), condition);
        List<Equipment> equipment = new ArrayList<>(data.equipment());
        equipment.add(added);
        save(new LoanDeskData(data.users(), data.credentials(), equipment, data.requests(), data.loans()));
        return added;
    }

    /** Changes only mutable inventory fields, retaining the equipment ID and all references to it. */
    public Equipment updateEquipment(String id, String name, EquipmentCondition condition) throws IOException {
        permissions.require(Permission.MANAGE_EQUIPMENT);
        String normalizedId = requireText(id, "Equipment ID");
        if (condition == null) {
            throw new IllegalArgumentException("Equipment condition is required.");
        }
        Equipment updated = new Equipment(normalizedId, requireText(name, "Equipment name"), condition);
        LoanDeskData data = dataStore.loadOrSeed();
        int index = equipmentIndex(data.equipment(), normalizedId);
        if (index < 0) {
            throw new IllegalArgumentException("Equipment was not found.");
        }
        Equipment existing = data.equipment().get(index);
        if (existing.condition() != condition && availabilityService.calculate(
                existing, data.requests(), data.loans(), LocalDate.now(clock)) == AvailabilityStatus.RESERVED) {
            throw new IllegalStateException(
                    "Condition cannot be changed while this item has an active reservation.");
        }
        List<Equipment> equipment = new ArrayList<>(data.equipment());
        equipment.set(index, updated);
        save(new LoanDeskData(data.users(), data.credentials(), equipment, data.requests(), data.loans()));
        return updated;
    }

    private void save(LoanDeskData data) throws IOException {
        try {
            dataStore.save(data);
        } catch (IOException exception) {
            if (isStaleSnapshotFailure(exception)) {
                throw new StaleDataException("LoanDesk data changed; refresh and retry.");
            }
            throw exception;
        }
    }

    private static int equipmentIndex(List<Equipment> equipment, String id) {
        for (int index = 0; index < equipment.size(); index++) {
            if (equipment.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static String nextEquipmentId(LoanDeskData data) {
        String id;
        do {
            id = "equipment-" + UUID.randomUUID();
        } while (equipmentIndex(data.equipment(), id) >= 0);
        return id;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return value.trim();
    }

    private static boolean isStaleSnapshotFailure(IOException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("changed since it was loaded")
                    || message.contains("changed during save"))) {
                return true;
            }
        }
        return false;
    }

    /** An equipment row and its read-only availability derived from shared state. */
    public record InventoryItem(Equipment equipment, AvailabilityStatus availability) {
        public InventoryItem {
            Objects.requireNonNull(equipment);
            Objects.requireNonNull(availability);
        }
    }
}
