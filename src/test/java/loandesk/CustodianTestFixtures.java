package loandesk;

import java.time.Instant;
import java.time.LocalDate;

import loandesk.application.Session;
import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.domain.User;

/** Shared synthetic records for focused custodian tests. */
final class CustodianTestFixtures {
    static final Instant TIMESTAMP = Instant.parse("2026-10-01T09:00:00Z");
    static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);
    static final LocalDate DUE_DATE = LocalDate.of(2026, 10, 8);

    private CustodianTestFixtures() {
    }

    static Session authenticatedSession(Role role) {
        Session session = new Session();
        session.start(new User(role.name().toLowerCase(), role));
        return session;
    }

    static LoanRequest approvedRequest() {
        return new LoanRequest("request-1", "borrower", "equipment-1", "Coursework",
                START_DATE, DUE_DATE, RequestStatus.APPROVED, null, TIMESTAMP, TIMESTAMP,
                "supervisor", TIMESTAMP, "Approved", null, null, null);
    }

    static Loan activeLoan() {
        return new Loan("loan-1", "request-1", "borrower", "equipment-1", START_DATE,
                DUE_DATE, null, LoanStatus.ACTIVE);
    }

    static Loan lostLoan() {
        return new Loan("loan-1", "request-1", "borrower", "equipment-1", START_DATE,
                DUE_DATE, null, LoanStatus.LOST);
    }

    static Equipment equipment(EquipmentCondition condition) {
        return new Equipment("equipment-1", "Camera", condition);
    }
}
