package loandesk.application;

import java.io.IOException;
import java.util.ArrayList;

import loandesk.domain.Role;
import loandesk.domain.PasswordCredential;
import loandesk.domain.User;
import loandesk.persistence.DataStore;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;
import loandesk.security.PasswordHasher;

public final class AuthenticationService {
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;

    private final DataStore dataStore;
    private LoanDeskData data;

    public AuthenticationService(DataStore dataStore) throws IOException {
        this.dataStore = dataStore;
        this.data = dataStore.loadOrSeed();
    }

    public User loginBorrower(String username, String password) {
        String normalized = UsernamePolicy.normalize(username);
        User borrower = data.users().stream()
                .filter(user -> user.role() == Role.BORROWER)
                .filter(user -> user.username().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
        PasswordCredential credential = data.credentials().stream()
                .filter(candidate -> candidate.username().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
        if (borrower == null || credential == null || !PasswordHasher.matches(password, credential)) {
            throw new IllegalArgumentException("Invalid borrower username or password.");
        }
        return borrower;
    }

    public User signUpBorrower(String username, String password) throws IOException {
        String normalized = UsernamePolicy.normalize(username);
        validatePassword(password);
        boolean exists = data.users().stream()
                .anyMatch(user -> user.username().equalsIgnoreCase(normalized));
        if (exists) {
            throw new IllegalArgumentException("That borrower username is already in use.");
        }
        User borrower = new User(normalized, Role.BORROWER);
        ArrayList<User> users = new ArrayList<>(data.users());
        users.add(borrower);
        ArrayList<PasswordCredential> credentials = new ArrayList<>(data.credentials());
        credentials.add(PasswordHasher.hash(normalized, password));
        LoanDeskData updatedData = new LoanDeskData(users, credentials, data.equipment());
        dataStore.save(updatedData);
        data = updatedData;
        return borrower;
    }

    private static void validatePassword(String password) {
        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Passwords must be between 8 and 128 characters.");
        }
    }

    /**
     * Authenticates the single supervisor account against its stored credential.
     *
     * @throws IllegalArgumentException when the account or password is wrong
     */
    public User loginSupervisor(String password) {
        User supervisor = data.users().stream()
                .filter(user -> user.role() == Role.SUPERVISOR)
                .findFirst()
                .orElse(null);
        if (supervisor == null) {
            throw new IllegalArgumentException(
                    "This database has no supervisor account. Remove the local data/loandesk "
                            + "files to reseed the demonstration accounts.");
        }
        PasswordCredential credential = data.credentials().stream()
                .filter(candidate -> candidate.username().equals(supervisor.username()))
                .findFirst()
                .orElse(null);
        if (credential == null || !PasswordHasher.matches(password, credential)) {
            throw new IllegalArgumentException("Invalid supervisor password.");
        }
        return supervisor;
    }

    /**
     * Authenticates the single custodian account against its stored credential.
     *
     * @throws IllegalArgumentException when the account or password is wrong
     */
    public User loginCustodian(String password) {
        User custodian = data.users().stream()
                .filter(user -> user.role() == Role.CUSTODIAN)
                .filter(user -> user.username().equals(DatabaseDataStore.CUSTODIAN_USERNAME))
                .findFirst()
                .orElse(null);
        if (custodian == null) {
            throw new IllegalArgumentException(
                    "This database has no custodian account. Remove the local data/loandesk "
                            + "files to reseed the demonstration accounts.");
        }
        PasswordCredential credential = data.credentials().stream()
                .filter(candidate -> candidate.username().equals(custodian.username()))
                .findFirst()
                .orElse(null);
        if (credential == null || !PasswordHasher.matches(password, credential)) {
            throw new IllegalArgumentException("Invalid custodian password.");
        }
        return custodian;
    }
}
