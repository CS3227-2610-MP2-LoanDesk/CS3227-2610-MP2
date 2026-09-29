package loandesk.application;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.persistence.DataStore;

public final class CatalogueService {
    private final DataStore dataStore;
    private final PermissionService permissions;
    private final AvailabilityService availabilityService;

    public CatalogueService(DataStore dataStore, Session session) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.permissions = new PermissionService(session);
        this.availabilityService = new AvailabilityService();
    }

    public List<Equipment> loadCatalogue() throws IOException {
        permissions.require(Permission.BROWSE_CATALOGUE);
        return dataStore.loadOrSeed().equipment();
    }

    /** Returns borrower-visible equipment together with its current derived availability. */
    public List<CatalogueItem> loadCatalogueWithAvailability() throws IOException {
        permissions.require(Permission.BROWSE_CATALOGUE);
        var data = dataStore.loadOrSeed();
        LocalDate today = LocalDate.now();
        return data.equipment().stream()
                .map(equipment -> new CatalogueItem(equipment, availabilityService.calculate(
                        equipment, data.requests(), data.loans(), today)))
                .toList();
    }

    public List<Equipment> filterByName(List<Equipment> equipment, String query) {
        Objects.requireNonNull(equipment);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return List.copyOf(equipment);
        }
        return equipment.stream()
                .filter(item -> item.name().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
    }

    /** Filters by name, placing requestable equipment before every unavailable status. */
    public List<CatalogueItem> filterAndSortByName(List<CatalogueItem> equipment, String query) {
        Objects.requireNonNull(equipment);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return equipment.stream()
                .filter(item -> item.equipment().name().toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery))
                .sorted(Comparator
                        .comparing((CatalogueItem item) -> item.availability()
                                != AvailabilityStatus.AVAILABLE)
                        .thenComparing(item -> item.equipment().name(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** A read-only borrower catalogue row. */
    public record CatalogueItem(Equipment equipment, AvailabilityStatus availability) {
        public CatalogueItem {
            Objects.requireNonNull(equipment);
            Objects.requireNonNull(availability);
        }
    }
}
