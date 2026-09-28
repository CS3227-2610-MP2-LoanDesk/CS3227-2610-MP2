package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.AuthenticationService;
import loandesk.domain.Equipment;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class SupervisorAuthenticationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void signsInTheSeededSupervisorWithItsPassword() throws Exception {
        AuthenticationService service = new AuthenticationService(store());

        User supervisor = service.loginSupervisor(
                DatabaseDataStore.SUPERVISOR_DEMONSTRATION_PASSWORD);

        assertEquals(Role.SUPERVISOR, supervisor.role());
        assertEquals(DatabaseDataStore.SUPERVISOR_USERNAME, supervisor.username());
    }

    @Test
    void rejectsAWrongOrEmptySupervisorPassword() throws Exception {
        AuthenticationService service = new AuthenticationService(store());

        assertThrows(IllegalArgumentException.class, () -> service.loginSupervisor("wrong"));
        assertThrows(IllegalArgumentException.class, () -> service.loginSupervisor(""));
        assertThrows(IllegalArgumentException.class, () -> service.loginSupervisor(null));
    }

    @Test
    void supervisorsCanNoLongerEnterWithoutAPassword() throws Exception {
        AuthenticationService service = new AuthenticationService(store());

        assertThrows(IllegalArgumentException.class,
                () -> service.loginStaff(Role.SUPERVISOR));
        assertEquals(Role.CUSTODIAN, service.loginStaff(Role.CUSTODIAN).role());
    }

    @Test
    void reportsAnActionableErrorWhenNoSupervisorAccountExists() throws Exception {
        DatabaseDataStore store = store();
        store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1"))));

        AuthenticationService service = new AuthenticationService(store);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> service.loginSupervisor(
                        DatabaseDataStore.SUPERVISOR_DEMONSTRATION_PASSWORD));
        assertTrue(failure.getMessage().contains("no supervisor account"), failure.getMessage());
    }

    @Test
    void theSupervisorPasswordIsNotStoredInClearText() throws Exception {
        LoanDeskData seeded = store().loadOrSeed();

        var credential = seeded.credentials().stream()
                .filter(candidate -> candidate.username()
                        .equals(DatabaseDataStore.SUPERVISOR_USERNAME))
                .findFirst()
                .orElseThrow();
        assertEquals("PBKDF2WithHmacSHA256", credential.algorithm());
        assertFalse(credential.hash()
                .contains(DatabaseDataStore.SUPERVISOR_DEMONSTRATION_PASSWORD));
    }

    private DatabaseDataStore store() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        return store;
    }
}
