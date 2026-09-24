package loandesk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;

class WorkflowDomainTest {
    @Test
    void dueDateIsInclusiveForOverdueCalculation() {
        Loan loan = new Loan(
                "loan-1",
                "request-1",
                "borrower",
                "camera1",
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 10, 9),
                null,
                LoanStatus.ACTIVE);

        assertFalse(loan.isOverdue(LocalDate.of(2026, 10, 9)));
        assertTrue(loan.isOverdue(LocalDate.of(2026, 10, 10)));
    }

    @Test
    void returnedLoanIsNotOverdueAfterItsDueDate() {
        Loan loan = new Loan(
                "loan-1",
                "request-1",
                "borrower",
                "camera1",
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 10, 9),
                LocalDate.of(2026, 10, 9),
                LoanStatus.RETURNED);

        assertFalse(loan.isOverdue(LocalDate.of(2026, 10, 10)));
    }

    @Test
    void requestRejectsDueDateBeforeStartDate() {
        assertThrows(IllegalArgumentException.class, () -> new LoanRequest(
                "request-1",
                "borrower",
                "camera1",
                "Academic project",
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 9, 24),
                RequestStatus.PENDING,
                null,
                java.time.Instant.parse("2026-09-25T08:00:00Z"),
                java.time.Instant.parse("2026-09-25T08:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                null));
    }

    @Test
    void requestRejectsWhitespaceOnlyPurpose() {
        assertThrows(IllegalArgumentException.class, () -> new LoanRequest(
                "request-1",
                "borrower",
                "camera1",
                "   ",
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 10, 9),
                RequestStatus.PENDING,
                null,
                java.time.Instant.parse("2026-09-25T08:00:00Z"),
                java.time.Instant.parse("2026-09-25T08:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                null));
    }
}
