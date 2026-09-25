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
import loandesk.domain.Role;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;

public final class BorrowerEligibilityService {
    private static final int MAX_ACTIVE_LOANS_OR_RESERVATIONS = 3;

    private final DataStore dataStore;
    private final Session session;
    private final Clock clock;

    public BorrowerEligibilityService(DataStore dataStore, Session session) {
        this(dataStore, session, Clock.systemDefaultZone());
    }

    public BorrowerEligibilityService(DataStore dataStore, Session session, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.session = Objects.requireNonNull(session);
        this.clock = Objects.requireNonNull(clock);
    }

    public BorrowerEligibility currentEligibility() throws IOException {
        String borrowerUsername = session.requireRole(Role.BORROWER).username();
        LoanDeskData data = dataStore.loadOrSeed();
        LocalDate today = LocalDate.now(clock);
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
