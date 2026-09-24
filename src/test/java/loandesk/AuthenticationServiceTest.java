package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.AuthenticationService;
import loandesk.domain.PasswordCredential;
import loandesk.domain.Role;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;

class AuthenticationServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void seedsAccountsAndEquipmentOnFirstLoad() throws Exception {
        Path databasePath = databasePath();
        AuthenticationService service = new AuthenticationService(new DatabaseDataStore(databasePath));

        assertEquals("testBorrower1", service.loginBorrower(" TESTBORROWER1 ", "password1").username());
        LoanDeskData loaded = new DatabaseDataStore(databasePath).loadOrSeed();
        assertEquals(2, loaded.equipment().size());
        assertTrue(Files.exists(Path.of(databasePath + ".mv.db")));
        assertFalse(Files.exists(temporaryDirectory.resolve("loandesk.json")));
        assertEquals(Role.SUPERVISOR, service.loginStaff(Role.SUPERVISOR).role());
        assertEquals(Role.CUSTODIAN, service.loginStaff(Role.CUSTODIAN).role());
    }

    @Test
    void signsUpAndPersistsNewBorrower() throws Exception {
        Path databasePath = databasePath();
        AuthenticationService service = new AuthenticationService(new DatabaseDataStore(databasePath));

        service.signUpBorrower("NewBorrower", "newpassword");
        AuthenticationService reloaded = new AuthenticationService(new DatabaseDataStore(databasePath));

        assertEquals("newborrower", reloaded.loginBorrower("NEWBORROWER", "newpassword").username());
    }

    @Test
    void rejectsDuplicateBorrowerUsername() throws Exception {
        AuthenticationService service = new AuthenticationService(
                new DatabaseDataStore(databasePath()));

        assertThrows(IllegalArgumentException.class,
                () -> service.signUpBorrower("testborrower1", "newpassword"));
    }

    @Test
    void createsParentDirectoryForDatabase() throws Exception {
        Path databasePath = temporaryDirectory.resolve("nested/data/loandesk");

        new AuthenticationService(new DatabaseDataStore(databasePath));

        assertTrue(Files.exists(Path.of(databasePath + ".mv.db")));
    }

    @Test
    void rejectsWrongBorrowerPassword() throws Exception {
        AuthenticationService service = new AuthenticationService(
                new DatabaseDataStore(databasePath()));

        assertThrows(IllegalArgumentException.class,
                () -> service.loginBorrower("testBorrower1", "wrongpass"));
    }

    @Test
    void rejectsNullBorrowerPassword() throws Exception {
        AuthenticationService service = new AuthenticationService(
                new DatabaseDataStore(databasePath()));

        assertThrows(IllegalArgumentException.class,
                () -> service.loginBorrower("testBorrower1", null));
    }

    @Test
    void rejectsPasswordsOutsideTheAgreedLengthRange() throws Exception {
        AuthenticationService service = new AuthenticationService(
                new DatabaseDataStore(databasePath()));

        assertThrows(IllegalArgumentException.class,
                () -> service.signUpBorrower("newBorrower", "short"));
        assertThrows(IllegalArgumentException.class,
                () -> service.signUpBorrower("newBorrower", "a".repeat(129)));
    }

    @Test
    void acceptsThePasswordLengthBoundaries() throws Exception {
        AuthenticationService service = new AuthenticationService(
                new DatabaseDataStore(databasePath()));

        service.signUpBorrower("eightChars", "12345678");
        service.signUpBorrower("oneTwentyEightChars", "a".repeat(128));

        assertEquals("eightchars", service.loginBorrower("eightChars", "12345678").username());
        assertEquals("onetwentyeightchars",
                service.loginBorrower("oneTwentyEightChars", "a".repeat(128)).username());
    }

    @Test
    void persistsAHashInsteadOfThePlaintextPassword() throws Exception {
        Path databasePath = databasePath();
        AuthenticationService service = new AuthenticationService(new DatabaseDataStore(databasePath));

        service.signUpBorrower("NewBorrower", "newpassword");

        PasswordCredential credential = new DatabaseDataStore(databasePath).loadOrSeed().credentials().stream()
                .filter(candidate -> candidate.username().equals("newborrower"))
                .findFirst()
                .orElse(null);
        assertNotNull(credential);
        assertNotEquals("newpassword", credential.hash());
        assertFalse(credential.hash().contains("newpassword"));
    }

    @Test
    void failedSignUpDoesNotChangeServiceStateAndCanBeRetried() throws Exception {
        FailingSaveDataStore store = new FailingSaveDataStore(databasePath());
        AuthenticationService service = new AuthenticationService(store);
        store.failSaves = true;

        assertThrows(IOException.class, () -> service.signUpBorrower("retryUser", "newpassword"));
        assertThrows(IllegalArgumentException.class,
                () -> service.loginBorrower("retryUser", "newpassword"));

        store.failSaves = false;
        service.signUpBorrower("retryUser", "newpassword");
        assertEquals("retryuser", service.loginBorrower("retryUser", "newpassword").username());

        AuthenticationService reloaded = new AuthenticationService(new DatabaseDataStore(databasePath()));
        assertEquals("retryuser", reloaded.loginBorrower("retryUser", "newpassword").username());
    }

    private Path databasePath() {
        return temporaryDirectory.resolve("loandesk");
    }

    private static final class FailingSaveDataStore implements DataStore {
        private final DatabaseDataStore delegate;
        private boolean failSaves;

        private FailingSaveDataStore(Path databasePath) {
            this.delegate = new DatabaseDataStore(databasePath);
        }

        @Override
        public LoanDeskData loadOrSeed() throws IOException {
            return delegate.loadOrSeed();
        }

        @Override
        public void save(LoanDeskData data) throws IOException {
            if (failSaves) {
                throw new IOException("Synthetic save failure");
            }
            delegate.save(data);
        }
    }
}
