package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.AuthenticationService;
import loandesk.application.Permission;
import loandesk.application.PermissionService;
import loandesk.application.Session;
import loandesk.domain.Equipment;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class CustodianAuthenticationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void signsInTheSeededCustodianWithItsPassword() throws Exception {
        AuthenticationService service = new AuthenticationService(store());

        User custodian = service.loginCustodian(
                DatabaseDataStore.CUSTODIAN_DEMONSTRATION_PASSWORD);

        assertEquals(Role.CUSTODIAN, custodian.role());
        assertEquals(DatabaseDataStore.CUSTODIAN_USERNAME, custodian.username());
    }

    @Test
    void rejectsWrongOrMissingCustodianPassword() throws Exception {
        AuthenticationService service = new AuthenticationService(store());

        assertThrows(IllegalArgumentException.class, () -> service.loginCustodian("wrong"));
        assertThrows(IllegalArgumentException.class, () -> service.loginCustodian(""));
        assertThrows(IllegalArgumentException.class, () -> service.loginCustodian(null));
    }

    @Test
    void upgradesAnExistingDatabaseThatPredatesTheCustodianAccount() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore store = new DatabaseDataStore(databasePath);
        store.loadOrSeed();
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)), List.of(),
                List.of(new Equipment("camera1", "Camera 1"))));
        removeCustodianMigrationMarker(databasePath);

        User custodian = new AuthenticationService(new DatabaseDataStore(databasePath))
                .loginCustodian(DatabaseDataStore.CUSTODIAN_DEMONSTRATION_PASSWORD);

        assertEquals(Role.CUSTODIAN, custodian.role());
        assertTrue(new DatabaseDataStore(databasePath).loadOrSeed().users().stream()
                .anyMatch(user -> user.username().equals(DatabaseDataStore.CUSTODIAN_USERNAME)
                        && user.role() == Role.CUSTODIAN));
    }

    @Test
    void reloadRecognizesThePersistedSeededCustodian() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        new AuthenticationService(new DatabaseDataStore(databasePath));

        User custodian = new AuthenticationService(new DatabaseDataStore(databasePath))
                .loginCustodian(DatabaseDataStore.CUSTODIAN_DEMONSTRATION_PASSWORD);

        assertEquals(Role.CUSTODIAN, custodian.role());
    }

    @Test
    void storesOnlyAPbkdf2CredentialForTheSeededCustodian() throws Exception {
        var credential = store().loadOrSeed().credentials().stream()
                .filter(candidate -> candidate.username()
                        .equals(DatabaseDataStore.CUSTODIAN_USERNAME))
                .findFirst()
                .orElseThrow();

        assertEquals("PBKDF2WithHmacSHA256", credential.algorithm());
        assertFalse(credential.hash().contains(DatabaseDataStore.CUSTODIAN_DEMONSTRATION_PASSWORD));
    }

    @Test
    void doesNotExposeTheFormerPasswordlessStaffLogin() {
        assertFalse(Arrays.stream(AuthenticationService.class.getMethods())
                .anyMatch(method -> method.getName().equals("loginStaff")));
    }

    @Test
    void loggedOutBorrowerAndSupervisorSessionsCannotUseCustodianPermissions() {
        for (Session session : List.of(new Session(), authenticated(Role.BORROWER),
                authenticated(Role.SUPERVISOR))) {
            PermissionService permissions = new PermissionService(session);
            for (Permission permission : List.of(Permission.MANAGE_EQUIPMENT,
                    Permission.CHECK_OUT_LOAN, Permission.RECORD_RETURN,
                    Permission.RECORD_MAINTENANCE)) {
                assertThrows(IllegalStateException.class, () -> permissions.require(permission));
            }
        }
    }

    @Test
    void reportsAnActionableErrorWhenAnUnmigratableStoreHasNoCustodianAccount() throws Exception {
        LoanDeskData data = new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)), List.of(), List.of());
        AuthenticationService service = new AuthenticationService(new InMemoryDataStore(data));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.loginCustodian(DatabaseDataStore.CUSTODIAN_DEMONSTRATION_PASSWORD));

        assertTrue(failure.getMessage().contains("no custodian account"), failure.getMessage());
    }

    private DatabaseDataStore store() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        return store;
    }

    private static Session authenticated(Role role) {
        Session session = new Session();
        session.start(new User(role.name().toLowerCase(), role));
        return session;
    }

    private static void removeCustodianMigrationMarker(Path databasePath) throws Exception {
        String jdbcUrl = "jdbc:h2:file:" + databasePath.toAbsolutePath().normalize()
                .toString().replace('\\', '/');
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM custodian_auth_migration");
        }
    }

    private record InMemoryDataStore(LoanDeskData data) implements loandesk.persistence.DataStore {
        @Override
        public LoanDeskData loadOrSeed() {
            return data;
        }

        @Override
        public void save(LoanDeskData ignored) {
            throw new UnsupportedOperationException();
        }
    }
}
