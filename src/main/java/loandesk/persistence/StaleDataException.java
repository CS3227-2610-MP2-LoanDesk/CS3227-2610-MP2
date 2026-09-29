package loandesk.persistence;

import java.io.IOException;

/** Indicates that a complete snapshot was changed by another writer before it could be saved. */
public final class StaleDataException extends IOException {
    public StaleDataException(String message) {
        super(message);
    }
}
