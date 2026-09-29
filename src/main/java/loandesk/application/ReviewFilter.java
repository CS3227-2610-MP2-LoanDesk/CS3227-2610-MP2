package loandesk.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import loandesk.domain.Equipment;
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
        LocalDate startDateTo,
        String equipmentName) {

    public ReviewFilter {
        borrowerUsername = normalize(borrowerUsername);
        equipmentName = normalize(equipmentName);
        if (startDateFrom != null && startDateTo != null && startDateTo.isBefore(startDateFrom)) {
            throw new IllegalArgumentException("The filter end date cannot be before its start date.");
        }
    }

    /** Preserves the original filter shape for callers without an equipment-name query. */
    public ReviewFilter(RequestStatus status, String borrowerUsername,
            LocalDate startDateFrom, LocalDate startDateTo) {
        this(status, borrowerUsername, startDateFrom, startDateTo, null);
    }

    /** A filter that keeps every request. */
    public static ReviewFilter none() {
        return new ReviewFilter(null, null, null, null, null);
    }

    /** A filter that keeps only requests in one status. */
    public static ReviewFilter ofStatus(RequestStatus status) {
        return new ReviewFilter(status, null, null, null, null);
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

    /** Checks the optional equipment-name query against the equipment attached to a request. */
    public boolean matches(LoanRequest request, List<Equipment> equipment) {
        if (!matches(request)) {
            return false;
        }
        if (equipmentName == null) {
            return true;
        }
        return equipment.stream()
                .filter(item -> item.id().equals(request.equipmentId()))
                .anyMatch(item -> item.name().toLowerCase(Locale.ROOT).contains(equipmentName));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
