package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.domain.Equipment;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class DatabaseDataStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void oneSharedDatabaseStoresUsersForAllRoles() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        store.save(new LoanDeskData(
                List.of(
                        new User("borrower", Role.BORROWER),
                        new User("supervisor", Role.SUPERVISOR),
                        new User("custodian", Role.CUSTODIAN)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1"))));

        LoanDeskData loaded = store.loadOrSeed();

        assertEquals(List.of(
                new User("borrower", Role.BORROWER),
                new User("custodian", Role.CUSTODIAN),
                new User("supervisor", Role.SUPERVISOR)), loaded.users());
        assertEquals(1, loaded.equipment().size());
    }

    @Test
    void databaseRejectsCaseInsensitiveDuplicateUsernames() {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        assertDoesNotThrow(store::loadOrSeed);

        assertThrows(java.io.IOException.class, () -> store.save(new LoanDeskData(
                List.of(
                        new User("Borrower", Role.BORROWER),
                        new User("borrower", Role.BORROWER)),
                List.of(),
                List.of())));
    }

    @Test
    void failedSaveDoesNotPartiallyReplaceExistingState() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        LoanDeskData original = new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")));
        store.loadOrSeed();
        store.save(original);

        assertThrows(java.io.IOException.class, () -> store.save(new LoanDeskData(
                List.of(
                        new User("borrower", Role.BORROWER),
                        new User("BORROWER", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera2", "Camera 2")))));

        assertEquals(original, store.loadOrSeed());
    }

    @Test
    void staleSnapshotCannotReplaceNewerSharedDatabaseState() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore firstStore = new DatabaseDataStore(databasePath);
        DatabaseDataStore secondStore = new DatabaseDataStore(databasePath);
        firstStore.loadOrSeed();
        firstStore.save(new LoanDeskData(
                List.of(new User("first", Role.BORROWER)), List.of(), List.of()));
        secondStore.loadOrSeed();

        firstStore.save(new LoanDeskData(
                List.of(
                        new User("first", Role.BORROWER),
                        new User("newer", Role.SUPERVISOR)),
                List.of(), List.of()));

        assertThrows(java.io.IOException.class, () -> secondStore.save(new LoanDeskData(
                List.of(
                        new User("first", Role.BORROWER),
                        new User("stale", Role.CUSTODIAN)),
                List.of(), List.of())));

        assertEquals(List.of(
                new User("first", Role.BORROWER),
                new User("newer", Role.SUPERVISOR)), firstStore.loadOrSeed().users());
    }

    @Test
    void concurrentSnapshotsAllowOnlyOneWriterToCommit() throws Exception {
        Path databasePath = temporaryDirectory.resolve("loandesk");
        DatabaseDataStore firstStore = new DatabaseDataStore(databasePath);
        DatabaseDataStore secondStore = new DatabaseDataStore(databasePath);
        firstStore.loadOrSeed();
        firstStore.save(new LoanDeskData(
                List.of(new User("first", Role.BORROWER)), List.of(), List.of()));
        secondStore.loadOrSeed();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> saveAfterStart(start, firstStore, "one"));
            var second = executor.submit(() -> saveAfterStart(start, secondStore, "two"));
            start.countDown();

            int failures = 0;
            failures += failureCount(first);
            failures += failureCount(second);
            assertEquals(1, failures);
        } finally {
            executor.shutdownNow();
        }

        LoanDeskData loaded = new DatabaseDataStore(databasePath).loadOrSeed();
        assertEquals(2, loaded.users().size());
        assertEquals(1, loaded.users().stream()
                .filter(user -> user.username().equals("one") || user.username().equals("two"))
                .count());
    }

    private static Void saveAfterStart(CountDownLatch start, DatabaseDataStore store, String username)
            throws Exception {
        start.await();
        store.save(new LoanDeskData(
                List.of(
                        new User("first", Role.BORROWER),
                        new User(username, Role.SUPERVISOR)),
                List.of(), List.of()));
        return null;
    }

    private static int failureCount(java.util.concurrent.Future<Void> result) throws Exception {
        try {
            result.get();
            return 0;
        } catch (ExecutionException exception) {
            assertEquals(java.io.IOException.class, exception.getCause().getClass());
            return 1;
        }
    }
}
