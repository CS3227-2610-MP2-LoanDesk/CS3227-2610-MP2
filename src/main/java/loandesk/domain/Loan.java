package loandesk.domain;

import java.time.LocalDate;

public record Loan(
        String loanId,
        String requestId,
        String borrowerUsername,
        String equipmentId,
        LocalDate checkoutDate,
        LocalDate dueDate,
        LocalDate returnedDate,
        LoanStatus status) {
    public Loan {
        requireText(loanId, "Loan ID");
        requireText(requestId, "Request ID");
        requireText(borrowerUsername, "Borrower username");
        requireText(equipmentId, "Equipment ID");
        if (checkoutDate == null || dueDate == null) {
            throw new IllegalArgumentException("Loan dates are required.");
        }
        if (dueDate.isBefore(checkoutDate)) {
            throw new IllegalArgumentException("Due date cannot be before checkout date.");
        }
        if (returnedDate != null && returnedDate.isBefore(checkoutDate)) {
            throw new IllegalArgumentException("Return date cannot be before checkout date.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Loan status is required.");
        }
        if (status == LoanStatus.RETURNED && returnedDate == null) {
            throw new IllegalArgumentException("Returned date is required for a returned loan.");
        }
        if (status != LoanStatus.RETURNED && returnedDate != null) {
            throw new IllegalArgumentException("Only returned loans may have a returned date.");
        }
    }

    public boolean isOverdue(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        return status == LoanStatus.ACTIVE && today.isAfter(dueDate);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
    }
}
