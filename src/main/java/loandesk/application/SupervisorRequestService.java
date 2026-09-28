package loandesk.application;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;

/**
 * Supervisor review and authorization of borrower requests.
 *
 * <p>Every decision rechecks equipment availability and borrower eligibility at
 * decision time rather than trusting the state captured at submission, and
 * records who decided, when and why. Physical equipment condition is read but
 * never written here: that remains the custodian's responsibility.
 */
public final class SupervisorRequestService {
    private final DataStore dataStore;
    private final PermissionService permissions;
    private final RequestLifecycleService lifecycle;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    public SupervisorRequestService(DataStore dataStore, Session session) {
        this(dataStore, session, Clock.systemDefaultZone());
    }

    public SupervisorRequestService(DataStore dataStore, Session session, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.permissions = new PermissionService(session);
        this.clock = Objects.requireNonNull(clock);
        this.lifecycle = new RequestLifecycleService(dataStore, clock);
        this.availabilityService = new AvailabilityService();
    }

    /**
     * Returns the requests matching a filter, with those still awaiting a
     * decision first and the longest-waiting request at the top.
     */
    public List<LoanRequest> reviewQueue(ReviewFilter filter) throws IOException {
        permissions.require(Permission.REVIEW_REQUESTS);
        Objects.requireNonNull(filter);
        return lifecycle.loadWithExpiredApprovals().requests().stream()
                .filter(filter::matches)
                .sorted(Comparator
                        .comparingInt((LoanRequest request) ->
                                request.status() == RequestStatus.PENDING ? 0 : 1)
                        .thenComparing(LoanRequest::createdAt)
                        .thenComparing(LoanRequest::requestId))
                .toList();
    }

    /** Returns one request for inspection, regardless of which borrower owns it. */
    public LoanRequest findRequest(String requestId) throws IOException {
        permissions.require(Permission.REVIEW_REQUESTS);
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request was not found.");
        }
        return lifecycle.loadWithExpiredApprovals().requests().stream()
                .filter(request -> request.requestId().equals(requestId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Request was not found."));
    }

    /** Returns the loans a borrower has not yet returned, for decision context. */
    public List<Loan> outstandingLoans(String borrowerUsername) throws IOException {
        permissions.require(Permission.REVIEW_REQUESTS);
        String normalized = requireText(borrowerUsername, "Borrower username");
        return dataStore.loadOrSeed().loans().stream()
                .filter(loan -> loan.borrowerUsername().equals(normalized))
                .filter(loan -> loan.status() != LoanStatus.RETURNED)
                .sorted(Comparator.comparing(Loan::dueDate).thenComparing(Loan::loanId))
                .toList();
    }

    /** Reports the borrowing rules a borrower currently fails, if any. */
    public BorrowerEligibility eligibilityOf(String borrowerUsername) throws IOException {
        permissions.require(Permission.REVIEW_REQUESTS);
        String normalized = requireText(borrowerUsername, "Borrower username");
        return BorrowerEligibilityService.evaluate(
                lifecycle.loadWithExpiredApprovals(), normalized, LocalDate.now(clock));
    }

    /** Reports the shared availability of one equipment item. */
    public AvailabilityStatus availabilityOf(String equipmentId) throws IOException {
        permissions.require(Permission.REVIEW_REQUESTS);
        LoanDeskData data = lifecycle.loadWithExpiredApprovals();
        return availability(data, requireText(equipmentId, "Equipment ID"));
    }

    /** Returns every decided request, most recently decided first. */
    public List<LoanRequest> decisionHistory() throws IOException {
        permissions.require(Permission.VIEW_DECISION_HISTORY);
        return lifecycle.loadWithExpiredApprovals().requests().stream()
                .filter(request -> request.decisionAt() != null || request.cancelledAt() != null)
                .sorted(Comparator
                        .comparing(SupervisorRequestService::lastActionAt,
                                Comparator.reverseOrder())
                        .thenComparing(LoanRequest::requestId))
                .toList();
    }

    /**
     * Approves a pending request, reserving the item until collection.
     *
     * <p>The reason is optional for an approval. Availability and borrower
     * eligibility are rechecked here, so a request that was submittable earlier
     * is still refused when the item was taken or the borrower fell behind.
     */
    public LoanRequest approve(String requestId, String reason) throws IOException {
        String supervisor = permissions.require(Permission.APPROVE_REQUEST).username();
        String normalizedReason = optionalText(reason);
        return decide(requestId, (data, current) -> {
            AvailabilityStatus availability = availability(data, current.equipmentId());
            if (availability != AvailabilityStatus.AVAILABLE) {
                throw new IllegalStateException(
                        "Equipment is no longer available: " + availability);
            }
            BorrowerEligibility eligibility = BorrowerEligibilityService.evaluate(
                    data, current.borrowerUsername(), LocalDate.now(clock));
            if (!eligibility.canSubmitRequest()) {
                throw new IllegalStateException(
                        "Borrower is no longer eligible: " + eligibility.blockers());
            }
            return decided(current, RequestStatus.APPROVED, supervisor, normalizedReason);
        });
    }

    /** Rejects a pending request. A reason is required and is shown to the borrower. */
    public LoanRequest reject(String requestId, String reason) throws IOException {
        String supervisor = permissions.require(Permission.REJECT_REQUEST).username();
        String normalizedReason = requireText(reason, "Rejection reason");
        return decide(requestId, (data, current) ->
                decided(current, RequestStatus.REJECTED, supervisor, normalizedReason));
    }

    /**
     * Cancels an approved booking that has not been collected, releasing the
     * reservation. A reason is required and is recorded against the supervisor.
     */
    public LoanRequest cancelApproved(String requestId, String reason) throws IOException {
        String supervisor = permissions.require(Permission.CANCEL_APPROVED_REQUEST).username();
        String normalizedReason = requireText(reason, "Cancellation reason");
        return decide(requestId, (data, current) -> {
            if (current.status() != RequestStatus.APPROVED) {
                throw new IllegalStateException(
                        "Only an approved booking can be cancelled by a supervisor.");
            }
            RequestLifecycleService.requireLegalTransition(
                    current.status(), RequestStatus.CANCELLED);
            Instant now = clock.instant();
            return new LoanRequest(
                    current.requestId(),
                    current.borrowerUsername(),
                    current.equipmentId(),
                    current.purpose(),
                    current.startDate(),
                    current.dueDate(),
                    RequestStatus.CANCELLED,
                    null,
                    current.createdAt(),
                    now,
                    current.decisionBy(),
                    current.decisionAt(),
                    current.decisionReason(),
                    supervisor,
                    now,
                    normalizedReason);
        });
    }

    private LoanRequest decide(String requestId, RequestDecision decision) throws IOException {
        String normalizedRequestId = requireText(requestId, "Request ID");
        LoanDeskData data = lifecycle.loadWithExpiredApprovals();

        int requestIndex = -1;
        for (int index = 0; index < data.requests().size(); index++) {
            if (data.requests().get(index).requestId().equals(normalizedRequestId)) {
                requestIndex = index;
                break;
            }
        }
        if (requestIndex < 0) {
            throw new IllegalArgumentException("Request was not found.");
        }

        LoanRequest updated = decision.apply(data, data.requests().get(requestIndex));
        List<LoanRequest> requests = new ArrayList<>(data.requests());
        requests.set(requestIndex, updated);
        dataStore.save(new LoanDeskData(
                data.users(), data.credentials(), data.equipment(), requests, data.loans()));
        return updated;
    }

    private LoanRequest decided(
            LoanRequest current, RequestStatus status, String supervisor, String reason) {
        RequestLifecycleService.requireLegalTransition(current.status(), status);
        Instant now = clock.instant();
        return new LoanRequest(
                current.requestId(),
                current.borrowerUsername(),
                current.equipmentId(),
                current.purpose(),
                current.startDate(),
                current.dueDate(),
                status,
                null,
                current.createdAt(),
                now,
                supervisor,
                now,
                reason,
                current.cancelledBy(),
                current.cancelledAt(),
                current.cancellationReason());
    }

    private AvailabilityStatus availability(LoanDeskData data, String equipmentId) {
        Equipment equipment = data.equipment().stream()
                .filter(candidate -> candidate.id().equals(equipmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Equipment was not found."));
        return availabilityService.calculate(
                equipment, data.requests(), data.loans(), LocalDate.now(clock));
    }

    private static Instant lastActionAt(LoanRequest request) {
        if (request.cancelledAt() == null) {
            return request.decisionAt();
        }
        if (request.decisionAt() == null) {
            return request.cancelledAt();
        }
        return request.cancelledAt().isAfter(request.decisionAt())
                ? request.cancelledAt()
                : request.decisionAt();
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return value.trim();
    }

    @FunctionalInterface
    private interface RequestDecision {
        LoanRequest apply(LoanDeskData data, LoanRequest current);
    }
}
