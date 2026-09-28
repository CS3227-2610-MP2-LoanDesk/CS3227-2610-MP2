package loandesk.application;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;

/**
 * Owns the shared request lifecycle: which status transitions are legal, and
 * when an approved request that was never collected becomes EXPIRED.
 *
 * <p>Role-specific services decide whether an actor may attempt a transition;
 * this service decides whether the transition itself is allowed. Expiry is
 * applied lazily whenever shared request data is read, so a reservation that
 * lapsed overnight is released without a background task.
 */
public final class RequestLifecycleService {
    private static final Map<RequestStatus, Set<RequestStatus>> LEGAL_TRANSITIONS =
            buildTransitions();

    private final DataStore dataStore;
    private final Clock clock;

    public RequestLifecycleService(DataStore dataStore) {
        this(dataStore, Clock.systemDefaultZone());
    }

    public RequestLifecycleService(DataStore dataStore, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Reports whether a request may move directly from one status to another. */
    public static boolean isLegalTransition(RequestStatus from, RequestStatus to) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        return LEGAL_TRANSITIONS.get(from).contains(to);
    }

    /**
     * Rejects an illegal status transition.
     *
     * @throws IllegalStateException when the transition is not part of the
     *     agreed request lifecycle
     */
    public static void requireLegalTransition(RequestStatus from, RequestStatus to) {
        if (!isLegalTransition(from, to)) {
            throw new IllegalStateException(
                    "A request cannot move from " + from + " to " + to + ".");
        }
    }

    /** Returns the statuses a request may move to next. */
    public static Set<RequestStatus> nextStatuses(RequestStatus from) {
        Objects.requireNonNull(from);
        return LEGAL_TRANSITIONS.get(from);
    }

    /**
     * Reports whether an approved request was not collected by the end of its
     * start date. Collected, cancelled and rejected requests never expire.
     */
    public static boolean hasExpired(LoanRequest request, LocalDate today) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(today);
        return request.status() == RequestStatus.APPROVED
                && request.startDate().isBefore(today);
    }

    /**
     * Loads shared data with lapsed approvals already moved to EXPIRED,
     * persisting the sweep only when something actually changed.
     */
    public LoanDeskData loadWithExpiredApprovals() throws IOException {
        LoanDeskData data = dataStore.loadOrSeed();
        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();

        List<LoanRequest> requests = new ArrayList<>(data.requests());
        boolean changed = false;
        for (int index = 0; index < requests.size(); index++) {
            LoanRequest request = requests.get(index);
            if (hasExpired(request, today)) {
                requests.set(index, expire(request, now));
                changed = true;
            }
        }
        if (!changed) {
            return data;
        }

        LoanDeskData swept = new LoanDeskData(
                data.users(), data.credentials(), data.equipment(), requests, data.loans());
        dataStore.save(swept);
        return swept;
    }

    private static LoanRequest expire(LoanRequest request, Instant now) {
        requireLegalTransition(request.status(), RequestStatus.EXPIRED);
        return new LoanRequest(
                request.requestId(),
                request.borrowerUsername(),
                request.equipmentId(),
                request.purpose(),
                request.startDate(),
                request.dueDate(),
                RequestStatus.EXPIRED,
                null,
                request.createdAt(),
                now,
                request.decisionBy(),
                request.decisionAt(),
                request.decisionReason(),
                request.cancelledBy(),
                request.cancelledAt(),
                request.cancellationReason());
    }

    private static Map<RequestStatus, Set<RequestStatus>> buildTransitions() {
        Map<RequestStatus, Set<RequestStatus>> transitions = new EnumMap<>(RequestStatus.class);
        transitions.put(RequestStatus.PENDING, EnumSet.of(
                RequestStatus.APPROVED,
                RequestStatus.REJECTED,
                RequestStatus.CANCELLED));
        transitions.put(RequestStatus.APPROVED, EnumSet.of(
                RequestStatus.COLLECTED,
                RequestStatus.CANCELLED,
                RequestStatus.EXPIRED));
        transitions.put(RequestStatus.COLLECTED, EnumSet.noneOf(RequestStatus.class));
        transitions.put(RequestStatus.REJECTED, EnumSet.noneOf(RequestStatus.class));
        transitions.put(RequestStatus.CANCELLED, EnumSet.noneOf(RequestStatus.class));
        transitions.put(RequestStatus.EXPIRED, EnumSet.noneOf(RequestStatus.class));
        transitions.replaceAll((status, allowed) -> Collections.unmodifiableSet(allowed));
        return Collections.unmodifiableMap(transitions);
    }
}
