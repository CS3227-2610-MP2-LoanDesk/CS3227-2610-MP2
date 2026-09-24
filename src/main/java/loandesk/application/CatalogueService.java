package loandesk.application;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import loandesk.domain.Equipment;
import loandesk.domain.Role;
import loandesk.persistence.DataStore;

public final class CatalogueService {
    private final DataStore dataStore;
    private final Session session;

    public CatalogueService(DataStore dataStore, Session session) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.session = Objects.requireNonNull(session);
    }

    public List<Equipment> loadCatalogue() throws IOException {
        session.requireRole(Role.BORROWER);
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
