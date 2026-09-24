package loandesk.persistence;

import java.util.List;

import loandesk.domain.Equipment;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.PasswordCredential;
import loandesk.domain.User;

public record LoanDeskData(
        List<User> users,
        List<PasswordCredential> credentials,
        List<Equipment> equipment,
        List<LoanRequest> requests,
        List<Loan> loans) {
    public LoanDeskData {
        users = List.copyOf(users);
        credentials = credentials == null ? List.of() : List.copyOf(credentials);
        equipment = List.copyOf(equipment);
        requests = requests == null ? List.of() : List.copyOf(requests);
        loans = loans == null ? List.of() : List.copyOf(loans);
    }

    public LoanDeskData(List<User> users, List<Equipment> equipment) {
        this(users, List.of(), equipment, List.of(), List.of());
    }

    public LoanDeskData(
            List<User> users,
            List<PasswordCredential> credentials,
            List<Equipment> equipment) {
        this(users, credentials, equipment, List.of(), List.of());
    }
}
