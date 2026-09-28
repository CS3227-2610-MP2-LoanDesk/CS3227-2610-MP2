package loandesk.application;

import java.time.LocalDate;
import java.util.Locale;

import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;

/**
 * Review-queue filter. Every field is optional; a null field matches anything.
 * Dates are compared inclusively against the requested start date.
 */
public record ReviewFilter(
        RequestStatus status,
        String borrowerUsername,
        LocalDate startDateFrom,
        LocalDate startDateTo) {

    public ReviewFilter {
        borrowerUsername = normalize(borrowerUsername);
        if (startDateFrom != null && startDateTo != null && startDateTo.isBefore(startDateFrom)) {
            throw new IllegalArgumentException("The filter end date cannot be before its start date.");
        }
    }

    /** A filter that keeps every request. */
    public static ReviewFilter none() {
        return new ReviewFilter(null, null, null, null);
    }

    /** A filter that keeps only requests in one status. */
    public static ReviewFilter ofStatus(RequestStatus status) {
        return new ReviewFilter(status, null, null, null);
    }

    public boolean matches(LoanRequest request) {
        if (status != null && request.status() != status) {
            return false;
        }
        if (borrowerUsername != null
                && !request.borrowerUsername().equalsIgnoreCase(borrowerUsername)) {
            return false;
        }
        if (startDateFrom != null && request.startDate().isBefore(startDateFrom)) {
            return false;
        }
        return startDateTo == null || !request.startDate().isAfter(startDateTo);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
