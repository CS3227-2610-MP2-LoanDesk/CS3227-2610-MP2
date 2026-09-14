package loandesk.application;

import java.io.IOException;
import java.util.ArrayList;

import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.JsonDataStore;
import loandesk.persistence.LoanDeskData;

public final class AuthenticationService {
    private final JsonDataStore dataStore;
    private LoanDeskData data;

    public AuthenticationService(JsonDataStore dataStore) throws IOException {
        this.dataStore = dataStore;
        this.data = dataStore.loadOrSeed();
    }

    public User loginBorrower(String username) {
        String normalized = UsernamePolicy.normalize(username);
        return data.users().stream()
                .filter(user -> user.role() == Role.BORROWER)
                .filter(user -> user.username().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Borrower username was not found."));
    }

    public User signUpBorrower(String username) throws IOException {
        String normalized = UsernamePolicy.normalize(username);
        boolean exists = data.users().stream()
                .anyMatch(user -> user.username().equalsIgnoreCase(normalized));
        if (exists) {
            throw new IllegalArgumentException("That borrower username is already in use.");
        }
        User borrower = new User(normalized, Role.BORROWER);
        ArrayList<User> users = new ArrayList<>(data.users());
        users.add(borrower);
        data = new LoanDeskData(users, data.equipment());
        dataStore.save(data);
        return borrower;
    }

    public User loginStaff(Role role) {
        if (role == Role.BORROWER) {
            throw new IllegalArgumentException("Borrowers must use borrower login.");
        }
        return new User(role.name().toLowerCase(), role);
    }
}