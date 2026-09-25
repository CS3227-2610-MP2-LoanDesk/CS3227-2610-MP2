package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.Role;
import loandesk.domain.RequestStatus;
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
    void failedWorkflowSaveDoesNotPartiallyReplaceExistingState() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        Instant timestamp = Instant.parse("2026-09-25T08:00:00Z");
        LoanRequest originalRequest = new LoanRequest(
                "request-original", "borrower", "camera1", "Academic project",
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 9), RequestStatus.PENDING,
                null, timestamp, timestamp, null, null, null, null, null, null);
        LoanDeskData original = new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(originalRequest),
                List.of());
        store.loadOrSeed();
        store.save(original);

        LoanRequest invalidRequest = new LoanRequest(
                "request-invalid", "borrower", "missing-equipment", "Academic project",
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 9), RequestStatus.PENDING,
                null, timestamp, timestamp, null, null, null, null, null, null);

        assertThrows(java.io.IOException.class, () -> store.save(new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(originalRequest, invalidRequest),
                List.of())));

        assertEquals(original, store.loadOrSeed());
    }

    @Test
    void rejectsCancellationByAnotherBorrower() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        Instant timestamp = Instant.parse("2026-09-25T08:00:00Z");
        LoanRequest cancelledByAnotherBorrower = new LoanRequest(
                "request-cancelled", "alice", "camera1", "Academic project",
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 9),
                RequestStatus.CANCELLED, null, timestamp, timestamp, null, null, null,
                "bob", timestamp, "No longer needed");

        store.loadOrSeed();

        assertThrows(java.io.IOException.class, () -> store.save(new LoanDeskData(
                List.of(
                        new User("alice", Role.BORROWER),
                        new User("bob", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(cancelledByAnotherBorrower),
                List.of())));
    }

    @Test
    void rejectsCollectedRequestsSharingOneLoan() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        Instant timestamp = Instant.parse("2026-09-25T08:00:00Z");
        LoanRequest firstRequest = new LoanRequest(
                "request-a", "alice", "camera1", "Academic project",
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 9),
                RequestStatus.COLLECTED, "loan-shared", timestamp, timestamp,
                "supervisor", timestamp, "Approved", null, null, null);
        LoanRequest secondRequest = new LoanRequest(
                "request-b", "bob", "camera2", "Academic project",
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 9),
                RequestStatus.COLLECTED, "loan-shared", timestamp, timestamp,
                "supervisor", timestamp, "Approved", null, null, null);
        Loan loan = new Loan(
                "loan-shared", "request-a", "alice", "camera1",
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 9), null,
                LoanStatus.ACTIVE);

        store.loadOrSeed();

        assertThrows(java.io.IOException.class, () -> store.save(new LoanDeskData(
                List.of(
                        new User("alice", Role.BORROWER),
                        new User("bob", Role.BORROWER)),
                List.of(),
                List.of(
                        new Equipment("camera1", "Camera 1"),
                        new Equipment("camera2", "Camera 2")),
                List.of(firstRequest, secondRequest),
                List.of(loan))));
    }

    @Test
    void additiveSchemaUpdatePreservesExistingLegacyEquipment() throws Exception {
        Path databasePath = temporaryDirectory.resolve("legacy");
        String jdbcUrl = "jdbc:h2:file:" + databasePath.toAbsolutePath().normalize()
                .toString().replace('\\', '/');
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE users ("
                    + "username VARCHAR(30) PRIMARY KEY, "
                    + "username_key VARCHAR(30) NOT NULL UNIQUE, "
                    + "role VARCHAR(20) NOT NULL)");
            statement.executeUpdate("CREATE TABLE credentials ("
                    + "username VARCHAR(30) PRIMARY KEY, algorithm VARCHAR(100) NOT NULL, "
                    + "iterations INT NOT NULL, salt VARCHAR(255) NOT NULL, "
                    + "password_hash VARCHAR(255) NOT NULL)");
            statement.executeUpdate("CREATE TABLE equipment ("
                    + "id VARCHAR(100) PRIMARY KEY, name VARCHAR(255) NOT NULL)");
            statement.executeUpdate("CREATE TABLE database_state ("
                    + "id INT PRIMARY KEY, revision BIGINT NOT NULL)");
            statement.executeUpdate("INSERT INTO database_state (id, revision) VALUES (1, 7)");
            statement.executeUpdate("INSERT INTO users (username, username_key, role) "
                    + "VALUES ('legacy', 'legacy', 'BORROWER')");
            statement.executeUpdate("INSERT INTO equipment (id, name) "
                    + "VALUES ('legacy-camera', 'Legacy Camera')");
        }

        LoanDeskData loaded = new DatabaseDataStore(databasePath).loadOrSeed();

        assertEquals(List.of(new User("legacy", Role.BORROWER)), loaded.users());
        assertEquals(List.of(new Equipment("legacy-camera", "Legacy Camera")), loaded.equipment());
        assertTrue(loaded.requests().isEmpty());
        assertTrue(loaded.loans().isEmpty());
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

    @Test
    void persistsWorkflowRecordsAndEquipmentCondition() throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        Instant createdAt = Instant.parse("2026-09-25T08:00:00Z");
        LoanRequest request = new LoanRequest(
                "request-1",
                "borrower",
                "camera1",
                "Academic project",
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 10, 9),
                RequestStatus.COLLECTED,
                "loan-1",
                createdAt,
                createdAt,
                "supervisor",
                createdAt,
                "Approved",
                null,
                null,
                null);
        Loan loan = new Loan(
                "loan-1",
                "request-1",
                "borrower",
                "camera1",
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 10, 9),
                null,
                LoanStatus.ACTIVE);
        LoanDeskData expected = new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1", EquipmentCondition.DAMAGED)),
                List.of(request),
                List.of(loan));

        store.loadOrSeed();
        store.save(expected);

        assertEquals(expected, new DatabaseDataStore(temporaryDirectory.resolve("loandesk"))
                .loadOrSeed());
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
