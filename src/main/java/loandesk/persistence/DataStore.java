package loandesk.persistence;

import java.io.IOException;

public interface DataStore {
    LoanDeskData loadOrSeed() throws IOException;

    void save(LoanDeskData data) throws IOException;
}
