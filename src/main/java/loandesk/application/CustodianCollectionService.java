package loandesk.application;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Loan;
import loandesk.domain.LoanStatus;
import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;
import loandesk.persistence.StaleDataException;

/** Custodian-facing collection queue and the one-time checkout operation. */
public final class CustodianCollectionService {
    private final DataStore dataStore;
    private final PermissionService permissions;
    private final RequestLifecycleService lifecycle;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    public CustodianCollectionService(DataStore dataStore, Session session) {
        this(dataStore, session, Clock.systemDefaultZone());
    }

    public CustodianCollectionService(DataStore dataStore, Session session, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.permissions = new PermissionService(Objects.requireNonNull(session));
        this.clock = Objects.requireNonNull(clock);
        this.lifecycle = new RequestLifecycleService(dataStore, clock);
        this.availabilityService = new AvailabilityService();
    }

    /**
     * Returns approved requests that may be collected today. Expired approvals are swept before
     * the queue is shown.
     */
    public List<LoanRequest> collectionsQueue() throws IOException {
        permissions.require(Permission.CHECK_OUT_LOAN);
        LocalDate today = LocalDate.now(clock);
        return lifecycle.loadWithExpiredApprovals().requests().stream()
                .filter(request -> request.status() == RequestStatus.APPROVED)
                .filter(request -> RequestLifecycleService.isWithinCollectionWindow(request, today))
                .sorted(Comparator.comparing(LoanRequest::startDate)
                        .thenComparing(LoanRequest::createdAt)
                        .thenComparing(LoanRequest::requestId))
                .toList();
    }

    /**
     * Checks out one approved request. Request and loan changes are persisted together as one
     * snapshot; a {@code StaleDataException} tells callers to refresh the collection queue and
     * retry rather than risk overwriting another checkout.
     */
    public Loan checkout(String requestId) throws IOException {
        permissions.require(Permission.CHECK_OUT_LOAN);
        String normalizedRequestId = requireText(requestId, "Request ID");
        LoanDeskData data = lifecycle.loadWithExpiredApprovals();
        LocalDate today = LocalDate.now(clock);

        int requestIndex = requestIndex(data.requests(), normalizedRequestId);
        if (requestIndex < 0) {
            throw new IllegalArgumentException("Request was not found.");
        }
        LoanRequest request = data.requests().get(requestIndex);
        if (request.status() != RequestStatus.APPROVED) {
            throw new IllegalStateException("Only an approved request can be checked out.");
        }
        if (!RequestLifecycleService.isWithinCollectionWindow(request, today)) {
            throw new IllegalStateException("This request is outside its collection window.");
        }

        Equipment equipment = data.equipment().stream()
                .filter(candidate -> candidate.id().equals(request.equipmentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Equipment was not found."));
        requireCollectableEquipment(data, request, equipment, today);

        String loanId = "loan-" + UUID.randomUUID();
        Loan loan = new Loan(loanId, request.requestId(), request.borrowerUsername(),
                request.equipmentId(), today, request.dueDate(), null, LoanStatus.ACTIVE);
        Instant now = clock.instant();
        RequestLifecycleService.requireLegalTransition(request.status(), RequestStatus.COLLECTED);
        LoanRequest collected = new LoanRequest(request.requestId(), request.borrowerUsername(),
                request.equipmentId(), request.purpose(), request.startDate(), request.dueDate(),
                RequestStatus.COLLECTED, loanId, request.createdAt(), now, request.decisionBy(),
                request.decisionAt(), request.decisionReason(), request.cancelledBy(),
                request.cancelledAt(), request.cancellationReason());

        List<LoanRequest> requests = new ArrayList<>(data.requests());
        requests.set(requestIndex, collected);
        List<Loan> loans = new ArrayList<>(data.loans());
        loans.add(loan);
        try {
            dataStore.save(new LoanDeskData(data.users(), data.credentials(), data.equipment(),
                    requests, loans));
        } catch (IOException exception) {
            if (isStaleSnapshotFailure(exception)) {
                throw new StaleDataException("LoanDesk data changed; refresh and retry.");
            }
            throw exception;
        }
        return loan;
    }

    private void requireCollectableEquipment(
            LoanDeskData data, LoanRequest request, Equipment equipment, LocalDate today) {
        if (equipment.condition() != EquipmentCondition.GOOD) {
            throw new IllegalStateException("Equipment is no longer available: "
                    + equipment.condition());
        }
        AvailabilityStatus availability = availabilityService.calculate(
                equipment, data.requests(), data.loans(), today);
        if (availability == AvailabilityStatus.ON_LOAN || availability == AvailabilityStatus.UNAVAILABLE) {
            throw new IllegalStateException("Equipment is no longer available: " + availability);
        }
        boolean reservedElsewhere = data.requests().stream()
                .anyMatch(other -> !other.requestId().equals(request.requestId())
                        && other.equipmentId().equals(equipment.id())
                        && RequestLifecycleService.isActiveApprovedReservation(other, today));
        if (reservedElsewhere) {
            throw new IllegalStateException("Equipment is no longer available: RESERVED");
        }
    }

    private static int requestIndex(List<LoanRequest> requests, String requestId) {
        for (int index = 0; index < requests.size(); index++) {
            if (requests.get(index).requestId().equals(requestId)) {
                return index;
            }
        }
        return -1;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return value.trim();
    }

    private static boolean isStaleSnapshotFailure(IOException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("changed since it was loaded")
                    || message.contains("changed during save"))) {
                return true;
            }
        }
        return false;
    }
}
