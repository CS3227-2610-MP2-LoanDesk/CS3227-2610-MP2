package loandesk.persistence;

import java.util.List;

import loandesk.domain.Equipment;
import loandesk.domain.User;

public record LoanDeskData(List<User> users, List<Equipment> equipment) {
    public LoanDeskData {
        users = List.copyOf(users);
        equipment = List.copyOf(equipment);
    }
}