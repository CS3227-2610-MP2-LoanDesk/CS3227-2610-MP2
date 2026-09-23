package loandesk.application;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import loandesk.domain.Equipment;
import loandesk.persistence.DataStore;

public final class CatalogueService {
    private final DataStore dataStore;

    public CatalogueService(DataStore dataStore) {
        this.dataStore = Objects.requireNonNull(dataStore);
    }

    public List<Equipment> loadCatalogue() throws IOException {
        return dataStore.loadOrSeed().equipment();
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
}
