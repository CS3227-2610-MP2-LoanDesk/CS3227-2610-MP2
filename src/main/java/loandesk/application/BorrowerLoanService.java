package loandesk.application;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import loandesk.domain.Loan;
import loandesk.domain.LoanStatus;
import loandesk.domain.Role;
import loandesk.persistence.DataStore;

/** Provides read-only loan history for the logged-in borrower. */
public final class BorrowerLoanService {
    private final DataStore dataStore;
    private final Session session;

    public BorrowerLoanService(DataStore dataStore, Session session) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.session = Objects.requireNonNull(session);
    }

    /** Returns only the current borrower's loans, with active loans first. */
    public List<Loan> listOwnLoans() throws IOException {
        String borrowerUsername = session.requireRole(Role.BORROWER).username();
        return dataStore.loadOrSeed().loans().stream()
                .filter(loan -> loan.borrowerUsername().equals(borrowerUsername))
                .sorted(Comparator
                        .comparingInt((Loan loan) -> isActive(loan.status()) ? 0 : 1)
                        .thenComparing(Loan::dueDate, Comparator.reverseOrder())
                        .thenComparing(Loan::loanId))
                .toList();
    }

    private static boolean isActive(LoanStatus status) {
        return status == LoanStatus.ACTIVE || status == LoanStatus.LOST;
    }
}
