package loandesk.persistence;

import java.util.List;

import loandesk.domain.Equipment;
import loandesk.domain.PasswordCredential;
import loandesk.domain.User;

public record LoanDeskData(List<User> users, List<PasswordCredential> credentials, List<Equipment> equipment) {
    public LoanDeskData {
        users = List.copyOf(users);
        credentials = credentials == null ? List.of() : List.copyOf(credentials);
        equipment = List.copyOf(equipment);
    }

    public LoanDeskData(List<User> users, List<Equipment> equipment) {
        this(users, List.of(), equipment);
    }
}
