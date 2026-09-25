package loandesk.application;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;

/** Handles the borrower-side creation of one pending request. */
public final class BorrowerRequestService {
    private static final long MAX_REQUESTED_DAYS_AFTER_START = 14;

    private final DataStore dataStore;
    private final Session session;
    private final Clock clock;
    private final AvailabilityService availabilityService;

    public BorrowerRequestService(DataStore dataStore, Session session) {
        this(dataStore, session, Clock.systemDefaultZone());
    }

    public BorrowerRequestService(DataStore dataStore, Session session, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.session = Objects.requireNonNull(session);
        this.clock = Objects.requireNonNull(clock);
        this.availabilityService = new AvailabilityService();
    }

    public LoanRequest submitRequest(
            String equipmentId,
            String purpose,
            LocalDate startDate,
            LocalDate dueDate) throws IOException {
        var borrower = session.requireRole(Role.BORROWER);
        String normalizedEquipmentId = requireText(equipmentId, "Equipment ID");
        String normalizedPurpose = requireText(purpose, "Purpose");
        if (startDate == null || dueDate == null) {
            throw new IllegalArgumentException("Request dates are required.");
        }

        LocalDate today = LocalDate.now(clock);
        if (startDate.isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
        if (dueDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Due date cannot be before the start date.");
        }
        if (dueDate.isAfter(startDate.plusDays(MAX_REQUESTED_DAYS_AFTER_START))) {
            throw new IllegalArgumentException("Borrowing period cannot exceed 14 days.");
        }

        BorrowerEligibility eligibility = new BorrowerEligibilityService(dataStore, session, clock)
                .currentEligibility();
        if (!eligibility.canSubmitRequest()) {
            throw new IllegalStateException("Borrower cannot submit a request: "
                    + eligibility.blockers());
        }

        LoanDeskData data = dataStore.loadOrSeed();
        Equipment equipment = data.equipment().stream()
                .filter(candidate -> candidate.id().equals(normalizedEquipmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Equipment was not found."));

        AvailabilityStatus availability = availabilityService.calculate(
                equipment, data.requests(), data.loans(), today);
        if (availability != AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("Equipment is not currently available: " + availability);
        }
        if (data.requests().stream().anyMatch(request ->
                request.borrowerUsername().equals(borrower.username())
                        && request.equipmentId().equals(normalizedEquipmentId)
                        && request.status() == RequestStatus.PENDING)) {
            throw new IllegalStateException("A pending request already exists for this equipment.");
        }

        Instant now = clock.instant();
        LoanRequest request = new LoanRequest(
                UUID.randomUUID().toString(),
                borrower.username(),
                normalizedEquipmentId,
                normalizedPurpose,
                startDate,
                dueDate,
                RequestStatus.PENDING,
                null,
                now,
                now,
                null,
                null,
                null,
                null,
                null,
                null);

        List<LoanRequest> requests = new java.util.ArrayList<>(data.requests());
        requests.add(request);
        dataStore.save(new LoanDeskData(
                data.users(), data.credentials(), data.equipment(), requests, data.loans()));
        return request;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return value.trim();
    }
}
