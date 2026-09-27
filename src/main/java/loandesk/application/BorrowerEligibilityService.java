package loandesk.application;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import loandesk.domain.Loan;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;

public final class BorrowerEligibilityService {
    private static final int MAX_ACTIVE_LOANS_OR_RESERVATIONS = 3;

    private final DataStore dataStore;
    private final PermissionService permissions;
    private final Clock clock;

    public BorrowerEligibilityService(DataStore dataStore, Session session) {
        this(dataStore, session, Clock.systemDefaultZone());
    }

    public BorrowerEligibilityService(DataStore dataStore, Session session, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.permissions = new PermissionService(session);
        this.clock = Objects.requireNonNull(clock);
    }

    public BorrowerEligibility currentEligibility() throws IOException {
        String borrowerUsername = permissions.require(Permission.SUBMIT_REQUEST).username();
        return evaluate(dataStore.loadOrSeed(), borrowerUsername, LocalDate.now(clock));
    }

    /**
     * Applies the borrowing rules to one borrower without needing their session,
     * so the supervisor can recheck the same rules at approval time.
     */
    public static BorrowerEligibility evaluate(
            LoanDeskData data, String borrowerUsername, LocalDate today) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(borrowerUsername);
        Objects.requireNonNull(today);
        List<EligibilityBlocker> blockers = new ArrayList<>();

        List<Loan> borrowerLoans = data.loans().stream()
                .filter(loan -> loan.borrowerUsername().equals(borrowerUsername))
                .toList();
        if (borrowerLoans.stream().anyMatch(loan -> loan.isOverdue(today))) {
            blockers.add(EligibilityBlocker.OVERDUE_LOAN);
        }
        if (borrowerLoans.stream().anyMatch(loan -> loan.status() == LoanStatus.LOST)) {
            blockers.add(EligibilityBlocker.LOST_LOAN);
        }

        long activeLoans = borrowerLoans.stream()
                .filter(loan -> loan.status() == LoanStatus.ACTIVE
                        || loan.status() == LoanStatus.LOST)
                .count();
        long approvedReservations = data.requests().stream()
                .filter(request -> request.borrowerUsername().equals(borrowerUsername))
                .filter(request -> request.status() == RequestStatus.APPROVED)
                .filter(request -> !request.startDate().isBefore(today))
                .count();
        if (activeLoans + approvedReservations >= MAX_ACTIVE_LOANS_OR_RESERVATIONS) {
            blockers.add(EligibilityBlocker.LOAN_OR_RESERVATION_LIMIT);
        }
        return new BorrowerEligibility(blockers.isEmpty(), blockers);
    }
}
