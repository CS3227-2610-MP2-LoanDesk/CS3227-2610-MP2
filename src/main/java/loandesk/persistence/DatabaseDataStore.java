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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import loandesk.domain.Equipment;
import loandesk.domain.PasswordCredential;
import loandesk.domain.Role;
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
                name VARCHAR(255) NOT NULL
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
        try (Statement statement = connection.createStatement()) {
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
                "INSERT INTO equipment (id, name) VALUES (?, ?)")) {
            for (Equipment equipment : data.equipment()) {
                statement.setString(1, equipment.id());
                statement.setString(2, equipment.name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
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
                "SELECT id, name FROM equipment ORDER BY id");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                equipment.add(new Equipment(
                        resultSet.getString("id"),
                        resultSet.getString("name")));
            }
        }
        return new LoanDeskData(users, credentials, equipment);
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
