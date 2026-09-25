package loandesk.domain;

import java.time.Instant;
import java.time.LocalDate;

public record LoanRequest(
        String requestId,
        String borrowerUsername,
        String equipmentId,
        String purpose,
        LocalDate startDate,
        LocalDate dueDate,
        RequestStatus status,
        String loanId,
        Instant createdAt,
        Instant updatedAt,
        String decisionBy,
        Instant decisionAt,
        String decisionReason,
        String cancelledBy,
        Instant cancelledAt,
        String cancellationReason) {
    public LoanRequest {
        requireText(requestId, "Request ID");
        requireText(borrowerUsername, "Borrower username");
        requireText(equipmentId, "Equipment ID");
        requireText(purpose, "Purpose");
        if (startDate == null || dueDate == null) {
            throw new IllegalArgumentException("Request dates are required.");
        }
        if (dueDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Due date cannot be before the start date.");
        }
        if (status == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Request status and timestamps are required.");
        }
        if (status == RequestStatus.COLLECTED) {
            requireText(loanId, "Loan ID");
        } else if (loanId != null) {
            throw new IllegalArgumentException("Only collected requests may link to a loan.");
        }
        if (status == RequestStatus.APPROVED
                || status == RequestStatus.REJECTED
                || status == RequestStatus.COLLECTED) {
            requireText(decisionBy, "Decision owner");
            if (decisionAt == null) {
                throw new IllegalArgumentException("Decision time is required.");
            }
            requireText(decisionReason, "Decision reason");
        } else if (status == RequestStatus.PENDING
                && (decisionBy != null || decisionAt != null || decisionReason != null)) {
            throw new IllegalArgumentException("Pending requests cannot have decision metadata.");
        }
        if (status == RequestStatus.CANCELLED) {
            requireText(cancelledBy, "Cancellation owner");
            if (cancelledAt == null) {
                throw new IllegalArgumentException("Cancellation time is required.");
            }
            requireText(cancellationReason, "Cancellation reason");
        } else if (cancelledBy != null || cancelledAt != null || cancellationReason != null) {
            throw new IllegalArgumentException("Cancellation metadata requires a cancelled request.");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
    }
}
