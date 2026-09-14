package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.AuthenticationService;
import loandesk.domain.Role;
import loandesk.persistence.JsonDataStore;

class AuthenticationServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void seedsAccountsAndEquipmentOnFirstLoad() throws Exception {
        Path dataFile = temporaryDirectory.resolve("loandesk.json");
        AuthenticationService service = new AuthenticationService(new JsonDataStore(dataFile));

        assertEquals("testBorrower1", service.loginBorrower(" TESTBORROWER1 ").username());
        assertEquals(2, new JsonDataStore(dataFile).loadOrSeed().equipment().size());
        assertEquals(Role.SUPERVISOR, service.loginStaff(Role.SUPERVISOR).role());
        assertEquals(Role.CUSTODIAN, service.loginStaff(Role.CUSTODIAN).role());
        assertEquals("loandesk.json", dataFile.getFileName().toString());
    }

    @Test
    void signsUpAndPersistsNewBorrower() throws Exception {
        Path dataFile = temporaryDirectory.resolve("loandesk.json");
        AuthenticationService service = new AuthenticationService(new JsonDataStore(dataFile));

        service.signUpBorrower("NewBorrower");
        AuthenticationService reloaded = new AuthenticationService(new JsonDataStore(dataFile));

        assertEquals("newborrower", reloaded.loginBorrower("NEWBORROWER").username());
    }

    @Test
    void rejectsDuplicateBorrowerUsername() throws Exception {
        AuthenticationService service = new AuthenticationService(
                new JsonDataStore(temporaryDirectory.resolve("loandesk.json")));

        assertThrows(IllegalArgumentException.class,
                () -> service.signUpBorrower("testborrower1"));
    }

    @Test
    void createsParentDirectoryForDataFile() throws Exception {
        Path dataFile = temporaryDirectory.resolve("nested/data/loandesk.json");

        new AuthenticationService(new JsonDataStore(dataFile));

        assertEquals(true, Files.exists(dataFile));
    }
}
