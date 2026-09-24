package loandesk.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;

import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.PasswordCredential;
import loandesk.domain.Role;
import loandesk.domain.RequestStatus;
import loandesk.domain.User;
import loandesk.security.PasswordHasher;

public final class DatabaseDataStore implements DataStore {
    private static final String CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(30) PRIMARY KEY,
                username_key VARCHAR(30) NOT NULL UNIQUE,
                role VARCHAR(20) NOT NULL
            )
            """;
    private static final String CREATE_CREDENTIALS = """
            CREATE TABLE IF NOT EXISTS credentials (
                username VARCHAR(30) PRIMARY KEY,
                algorithm VARCHAR(100) NOT NULL,
                iterations INT NOT NULL,
                salt VARCHAR(255) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                CONSTRAINT credentials_user_fk FOREIGN KEY (username)
                    REFERENCES users(username)
            )
            """;
    private static final String CREATE_EQUIPMENT = """
            CREATE TABLE IF NOT EXISTS equipment (
                id VARCHAR(100) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                equipment_condition VARCHAR(30) NOT NULL DEFAULT 'GOOD'
            )
            """;
    private static final String ADD_EQUIPMENT_CONDITION = """
            ALTER TABLE equipment ADD COLUMN IF NOT EXISTS
                equipment_condition VARCHAR(30) NOT NULL DEFAULT 'GOOD'
            """;
    private static final String CREATE_LOAN_REQUESTS = """
            CREATE TABLE IF NOT EXISTS loan_requests (
                request_id VARCHAR(100) PRIMARY KEY,
                borrower_username VARCHAR(30) NOT NULL,
                equipment_id VARCHAR(100) NOT NULL,
                purpose VARCHAR(500) NOT NULL,
                start_date DATE NOT NULL,
                due_date DATE NOT NULL,
                status VARCHAR(30) NOT NULL,
                loan_id VARCHAR(100),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                decision_by VARCHAR(30),
                decision_at TIMESTAMP,
                decision_reason VARCHAR(500),
                cancelled_by VARCHAR(30),
                cancelled_at TIMESTAMP,
                cancellation_reason VARCHAR(500),
                CONSTRAINT loan_requests_borrower_fk FOREIGN KEY (borrower_username)
                    REFERENCES users(username),
                CONSTRAINT loan_requests_equipment_fk FOREIGN KEY (equipment_id)
                    REFERENCES equipment(id)
            )
            """;
    private static final String CREATE_LOANS = """
            CREATE TABLE IF NOT EXISTS loans (
                loan_id VARCHAR(100) PRIMARY KEY,
                request_id VARCHAR(100) NOT NULL,
                borrower_username VARCHAR(30) NOT NULL,
                equipment_id VARCHAR(100) NOT NULL,
                checkout_date DATE NOT NULL,
                due_date DATE NOT NULL,
                returned_date DATE,
                status VARCHAR(30) NOT NULL,
                CONSTRAINT loans_request_fk FOREIGN KEY (request_id)
                    REFERENCES loan_requests(request_id),
                CONSTRAINT loans_borrower_fk FOREIGN KEY (borrower_username)
                    REFERENCES users(username),
                CONSTRAINT loans_equipment_fk FOREIGN KEY (equipment_id)
                    REFERENCES equipment(id)
            )
            """;
    private static final String CREATE_DATABASE_STATE = """
            CREATE TABLE IF NOT EXISTS database_state (
                id INT PRIMARY KEY,
                revision BIGINT NOT NULL
            )
            """;
    private static final String INSERT_INITIAL_STATE =
            "INSERT INTO database_state (id, revision) "
                    + "SELECT 1, 0 WHERE NOT EXISTS "
                    + "(SELECT 1 FROM database_state WHERE id = 1)";

    private final Path databasePath;
    private final String jdbcUrl;
    private Long loadedRevision;

    public DatabaseDataStore(Path databasePath) {
        this.databasePath = databasePath.toAbsolutePath().normalize();
        this.jdbcUrl = "jdbc:h2:file:" + this.databasePath.toString().replace('\\', '/');
    }

    @Override
    public LoanDeskData loadOrSeed() throws IOException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            initializeSchema(connection);
            if (isEmpty(connection)) {
                writeData(connection, seededData());
                incrementRevision(connection);
            }
            connection.commit();
            loadedRevision = readRevision(connection);
            return readData(connection);
        } catch (SQLException exception) {
            throw databaseException("Unable to load LoanDesk database", exception);
        }
    }

    @Override
    public void save(LoanDeskData data) throws IOException {
        if (loadedRevision == null) {
            throw new IOException("Load the LoanDesk database before saving it");
        }
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                initializeSchema(connection);
                long currentRevision = readRevision(connection);
                if (currentRevision != loadedRevision) {
                    throw new SQLException("LoanDesk database changed since it was loaded");
                }
                advanceRevisionIfExpected(connection, currentRevision);
                writeData(connection, data);
                connection.commit();
                loadedRevision = currentRevision + 1;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseException("Unable to save LoanDesk database", exception);
        }
    }

    private Connection openConnection() throws IOException, SQLException {
        Path parent = databasePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    private static void initializeSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_USERS);
            statement.executeUpdate(CREATE_CREDENTIALS);
            statement.executeUpdate(CREATE_EQUIPMENT);
            statement.executeUpdate(ADD_EQUIPMENT_CONDITION);
            statement.executeUpdate(CREATE_LOAN_REQUESTS);
            statement.executeUpdate(CREATE_LOANS);
            statement.executeUpdate(CREATE_DATABASE_STATE);
            statement.executeUpdate(INSERT_INITIAL_STATE);
        }
    }

    private static boolean isEmpty(Connection connection) throws SQLException {
        return count(connection, "users") == 0
                && count(connection, "credentials") == 0
                && count(connection, "equipment") == 0;
    }

    private static int count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static long readRevision(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT revision FROM database_state WHERE id = 1");
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("LoanDesk database revision is missing");
            }
            return resultSet.getLong(1);
        }
    }

    private static void incrementRevision(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE database_state SET revision = revision + 1 WHERE id = 1")) {
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Unable to update LoanDesk database revision");
            }
        }
    }

    private static void advanceRevisionIfExpected(Connection connection, long expectedRevision)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE database_state SET revision = revision + 1 "
                        + "WHERE id = 1 AND revision = ?")) {
            statement.setLong(1, expectedRevision);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("LoanDesk database changed during save");
            }
        }
    }

    private static void writeData(Connection connection, LoanDeskData data) throws SQLException {
        validateWorkflowData(data);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM loans");
            statement.executeUpdate("DELETE FROM loan_requests");
            statement.executeUpdate("DELETE FROM credentials");
            statement.executeUpdate("DELETE FROM users");
            statement.executeUpdate("DELETE FROM equipment");
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, username_key, role) VALUES (?, ?, ?)")) {
            for (User user : data.users()) {
                statement.setString(1, user.username());
                statement.setString(2, user.username().toLowerCase(Locale.ROOT));
                statement.setString(3, user.role().name());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO credentials "
                        + "(username, algorithm, iterations, salt, password_hash) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            for (PasswordCredential credential : data.credentials()) {
                statement.setString(1, credential.username());
                statement.setString(2, credential.algorithm());
                statement.setInt(3, credential.iterations());
                statement.setString(4, credential.salt());
                statement.setString(5, credential.hash());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO equipment (id, name, equipment_condition) VALUES (?, ?, ?)")) {
            for (Equipment equipment : data.equipment()) {
                statement.setString(1, equipment.id());
                statement.setString(2, equipment.name());
                statement.setString(3, equipment.condition().name());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO loan_requests "
                        + "(request_id, borrower_username, equipment_id, purpose, "
                        + "start_date, due_date, status, loan_id, created_at, updated_at, "
                        + "decision_by, decision_at, decision_reason, cancelled_by, "
                        + "cancelled_at, cancellation_reason) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (LoanRequest request : data.requests()) {
                statement.setString(1, request.requestId());
                statement.setString(2, request.borrowerUsername());
                statement.setString(3, request.equipmentId());
                statement.setString(4, request.purpose());
                statement.setObject(5, request.startDate());
                statement.setObject(6, request.dueDate());
                statement.setString(7, request.status().name());
                statement.setString(8, request.loanId());
                setInstant(statement, 9, request.createdAt());
                setInstant(statement, 10, request.updatedAt());
                statement.setString(11, request.decisionBy());
                setInstant(statement, 12, request.decisionAt());
                statement.setString(13, request.decisionReason());
                statement.setString(14, request.cancelledBy());
                setInstant(statement, 15, request.cancelledAt());
                statement.setString(16, request.cancellationReason());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO loans "
                        + "(loan_id, request_id, borrower_username, equipment_id, "
                        + "checkout_date, due_date, returned_date, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Loan loan : data.loans()) {
                statement.setString(1, loan.loanId());
                statement.setString(2, loan.requestId());
                statement.setString(3, loan.borrowerUsername());
                statement.setString(4, loan.equipmentId());
                statement.setObject(5, loan.checkoutDate());
                statement.setObject(6, loan.dueDate());
                statement.setObject(7, loan.returnedDate());
                statement.setString(8, loan.status().name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void validateWorkflowData(LoanDeskData data) throws SQLException {
        Map<String, LoanRequest> requestsById = new HashMap<>();
        Map<String, Loan> loansById = new HashMap<>();
        for (LoanRequest request : data.requests()) {
            if (requestsById.put(request.requestId(), request) != null) {
                throw new SQLException("Duplicate loan request ID: " + request.requestId());
            }
            if (!hasUser(data, request.borrowerUsername(), Role.BORROWER)) {
                throw new SQLException("Loan request borrower does not exist: "
                        + request.borrowerUsername());
            }
            if (!hasEquipment(data, request.equipmentId())) {
                throw new SQLException("Loan request equipment does not exist: "
                        + request.equipmentId());
            }
        }
        for (Loan loan : data.loans()) {
            if (loansById.put(loan.loanId(), loan) != null) {
                throw new SQLException("Duplicate loan ID: " + loan.loanId());
            }
            LoanRequest request = requestsById.get(loan.requestId());
            if (request == null || request.status() != RequestStatus.COLLECTED
                    || !loan.loanId().equals(request.loanId())) {
                throw new SQLException("Loan must reference its collected request: " + loan.loanId());
            }
            if (!loan.borrowerUsername().equals(request.borrowerUsername())
                    || !loan.equipmentId().equals(request.equipmentId())) {
                throw new SQLException("Loan owner or equipment differs from its request: "
                        + loan.loanId());
            }
            if (!hasUser(data, loan.borrowerUsername(), Role.BORROWER)) {
                throw new SQLException("Loan borrower does not exist: " + loan.borrowerUsername());
            }
            if (!hasEquipment(data, loan.equipmentId())) {
                throw new SQLException("Loan equipment does not exist: " + loan.equipmentId());
            }
        }
        for (LoanRequest request : data.requests()) {
            if (request.status() == RequestStatus.COLLECTED
                    && !loansById.containsKey(request.loanId())) {
                throw new SQLException("Collected request loan does not exist: " + request.requestId());
            }
        }
    }

    private static boolean hasUser(LoanDeskData data, String username, Role role) {
        return data.users().stream()
                .anyMatch(user -> user.username().equals(username) && user.role() == role);
    }

    private static boolean hasEquipment(LoanDeskData data, String equipmentId) {
        return data.equipment().stream().anyMatch(equipment -> equipment.id().equals(equipmentId));
    }

    private static LoanDeskData readData(Connection connection) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT username, role FROM users ORDER BY username");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new User(resultSet.getString("username"),
                        Role.valueOf(resultSet.getString("role"))));
            }
        }

        List<PasswordCredential> credentials = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT username, algorithm, iterations, salt, password_hash "
                        + "FROM credentials ORDER BY username");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                credentials.add(new PasswordCredential(
                        resultSet.getString("username"),
                        resultSet.getString("algorithm"),
                        resultSet.getInt("iterations"),
                        resultSet.getString("salt"),
                        resultSet.getString("password_hash")));
            }
        }

        List<Equipment> equipment = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, equipment_condition FROM equipment ORDER BY id");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                equipment.add(new Equipment(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        EquipmentCondition.valueOf(resultSet.getString("equipment_condition"))));
            }
        }

        List<LoanRequest> requests = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT request_id, borrower_username, equipment_id, purpose, start_date, "
                        + "due_date, status, loan_id, created_at, updated_at, decision_by, "
                        + "decision_at, decision_reason, cancelled_by, cancelled_at, "
                        + "cancellation_reason FROM loan_requests ORDER BY request_id");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                requests.add(new LoanRequest(
                        resultSet.getString("request_id"),
                        resultSet.getString("borrower_username"),
                        resultSet.getString("equipment_id"),
                        resultSet.getString("purpose"),
                        resultSet.getObject("start_date", java.time.LocalDate.class),
                        resultSet.getObject("due_date", java.time.LocalDate.class),
                        RequestStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("loan_id"),
                        readInstant(resultSet, "created_at"),
                        readInstant(resultSet, "updated_at"),
                        resultSet.getString("decision_by"),
                        readInstant(resultSet, "decision_at"),
                        resultSet.getString("decision_reason"),
                        resultSet.getString("cancelled_by"),
                        readInstant(resultSet, "cancelled_at"),
                        resultSet.getString("cancellation_reason")));
            }
        }

        List<Loan> loans = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT loan_id, request_id, borrower_username, equipment_id, checkout_date, "
                        + "due_date, returned_date, status FROM loans ORDER BY loan_id");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                loans.add(new Loan(
                        resultSet.getString("loan_id"),
                        resultSet.getString("request_id"),
                        resultSet.getString("borrower_username"),
                        resultSet.getString("equipment_id"),
                        resultSet.getObject("checkout_date", java.time.LocalDate.class),
                        resultSet.getObject("due_date", java.time.LocalDate.class),
                        resultSet.getObject("returned_date", java.time.LocalDate.class),
                        LoanStatus.valueOf(resultSet.getString("status"))));
            }
        }
        return new LoanDeskData(users, credentials, equipment, requests, loans);
    }

    private static void setInstant(PreparedStatement statement, int index, Instant value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.from(value));
        }
    }

    private static Instant readInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static LoanDeskData seededData() {
        User firstBorrower = new User("testBorrower1", Role.BORROWER);
        User secondBorrower = new User("testBorrower2", Role.BORROWER);
        return new LoanDeskData(
                List.of(firstBorrower, secondBorrower),
                List.of(
                        PasswordHasher.hash(firstBorrower.username(), "password1"),
                        PasswordHasher.hash(secondBorrower.username(), "password2")),
                List.of(
                        new Equipment("camera1", "Camera 1"),
                        new Equipment("camera2", "Camera 2")));
    }

    private static IOException databaseException(String message, SQLException cause) {
        return new IOException(message, cause);
    }
}
