package loandesk.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import loandesk.domain.Equipment;
import loandesk.domain.PasswordCredential;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.security.PasswordHasher;

public final class JsonDataStore {
    private final Path dataFile;
    private final ObjectMapper objectMapper;

    public JsonDataStore(Path dataFile) {
        this.dataFile = dataFile;
        this.objectMapper = new ObjectMapper();
    }

    public LoanDeskData loadOrSeed() throws IOException {
        if (Files.notExists(dataFile)) {
            User firstBorrower = new User("testBorrower1", Role.BORROWER);
            User secondBorrower = new User("testBorrower2", Role.BORROWER);
            LoanDeskData seeded = new LoanDeskData(
                    List.of(
                            firstBorrower,
                            secondBorrower),
                    List.of(
                            PasswordHasher.hash(firstBorrower.username(), "password1"),
                            PasswordHasher.hash(secondBorrower.username(), "password2")),
                    List.of(
                            new Equipment("camera1", "Camera 1"),
                            new Equipment("camera2", "Camera 2")));
            save(seeded);
            return seeded;
        }
        return objectMapper.readValue(dataFile.toFile(), LoanDeskData.class);
    }

    public void save(LoanDeskData data) throws IOException {
        Path parent = dataFile.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "loandesk-", ".tmp");
        try {
            objectMapper.writeValue(temporaryFile.toFile(), data);
            try {
                Files.move(temporaryFile, dataFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
