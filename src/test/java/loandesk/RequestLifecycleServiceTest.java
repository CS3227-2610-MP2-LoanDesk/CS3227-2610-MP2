package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import loandesk.application.RequestLifecycleService;
import loandesk.domain.Equipment;
import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DataStore;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.LoanDeskData;

class RequestLifecycleServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-28T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void allowsOnlyTheAgreedTransitions() {
        assertTrue(RequestLifecycleService.isLegalTransition(
                RequestStatus.PENDING, RequestStatus.APPROVED));
        assertTrue(RequestLifecycleService.isLegalTransition(
                RequestStatus.PENDING, RequestStatus.REJECTED));
        assertTrue(RequestLifecycleService.isLegalTransition(
                RequestStatus.PENDING, RequestStatus.CANCELLED));
        assertTrue(RequestLifecycleService.isLegalTransition(
                RequestStatus.APPROVED, RequestStatus.COLLECTED));
        assertTrue(RequestLifecycleService.isLegalTransition(
                RequestStatus.APPROVED, RequestStatus.CANCELLED));
        assertTrue(RequestLifecycleService.isLegalTransition(
                RequestStatus.APPROVED, RequestStatus.EXPIRED));
    }

    @Test
    void rejectsTransitionsOutOfTerminalStatuses() {
        for (RequestStatus terminal : List.of(RequestStatus.COLLECTED, RequestStatus.REJECTED,
                RequestStatus.CANCELLED, RequestStatus.EXPIRED)) {
            assertTrue(RequestLifecycleService.nextStatuses(terminal).isEmpty(),
                    terminal + " should be terminal");
            for (RequestStatus target : RequestStatus.values()) {
                assertThrows(IllegalStateException.class,
                        () -> RequestLifecycleService.requireLegalTransition(terminal, target));
            }
        }
    }

    @Test
    void rejectsSkippingAndReversingTheWorkflow() {
        assertFalse(RequestLifecycleService.isLegalTransition(
                RequestStatus.PENDING, RequestStatus.COLLECTED));
        assertFalse(RequestLifecycleService.isLegalTransition(
                RequestStatus.PENDING, RequestStatus.EXPIRED));
        assertFalse(RequestLifecycleService.isLegalTransition(
                RequestStatus.APPROVED, RequestStatus.PENDING));
        assertFalse(RequestLifecycleService.isLegalTransition(
                RequestStatus.APPROVED, RequestStatus.REJECTED));
        assertFalse(RequestLifecycleService.isLegalTransition(
                RequestStatus.PENDING, RequestStatus.PENDING));
    }

    @Test
    void collectionWindowIncludesStartDateAndFinalGraceDate() {
        LoanRequest startingToday = approved("today", TODAY);
        LoanRequest endingToday = approved("ending-today", TODAY.minusDays(3));

        assertFalse(RequestLifecycleService.hasExpired(startingToday, TODAY));
        assertFalse(RequestLifecycleService.hasExpired(endingToday, TODAY));
        assertTrue(RequestLifecycleService.isWithinCollectionWindow(startingToday, TODAY));
        assertTrue(RequestLifecycleService.isWithinCollectionWindow(endingToday, TODAY));
    }

    @Test
    void expiryStartsTheDayAfterTheFinalGraceDate() {
        LoanRequest expiredYesterday = approved("expired", TODAY.minusDays(4));

        assertFalse(RequestLifecycleService.isWithinCollectionWindow(expiredYesterday, TODAY));
        assertTrue(RequestLifecycleService.hasExpired(expiredYesterday, TODAY));
    }

    @Test
    void onlyApprovedRequestsExpire() {
        assertFalse(RequestLifecycleService.hasExpired(
                pending("pending", TODAY.minusDays(3)), TODAY));
        assertFalse(RequestLifecycleService.hasExpired(
                collected("collected", TODAY.minusDays(3)), TODAY));
    }

    @Test
    void sweepPersistsExpiryAndReleasesTheReservation() throws Exception {
        DatabaseDataStore store = storeWith(
                approved("lapsed", TODAY.minusDays(4)),
                approved("upcoming", TODAY.plusDays(1)));

        LoanDeskData swept = new RequestLifecycleService(store, CLOCK).loadWithExpiredApprovals();

        assertEquals(RequestStatus.EXPIRED, byId(swept, "lapsed").status());
        assertEquals(RequestStatus.APPROVED, byId(swept, "upcoming").status());
        assertEquals(NOW, byId(swept, "lapsed").updatedAt());

        LoanDeskData reloaded = new DatabaseDataStore(
                temporaryDirectory.resolve("loandesk")).loadOrSeed();
        assertEquals(RequestStatus.EXPIRED, byId(reloaded, "lapsed").status());
    }

    @Test
    void expiryKeepsTheOriginalDecisionRecord() throws Exception {
        DatabaseDataStore store = storeWith(approved("lapsed", TODAY.minusDays(4)));

        LoanRequest expired = byId(
                new RequestLifecycleService(store, CLOCK).loadWithExpiredApprovals(), "lapsed");

        assertEquals("supervisor", expired.decisionBy());
        assertEquals("Approved for coursework", expired.decisionReason());
        assertEquals(NOW.minusSeconds(60), expired.decisionAt());
    }

    @Test
    void sweepDoesNotSaveWhenNothingExpired() throws Exception {
        LoanDeskData data = dataWith(approved("upcoming", TODAY.plusDays(1)));
        CountingDataStore store = new CountingDataStore(data);

        LoanDeskData result = new RequestLifecycleService(store, CLOCK).loadWithExpiredApprovals();

        assertSame(data, result);
        assertEquals(0, store.saves);
    }

    private static LoanRequest byId(LoanDeskData data, String requestId) {
        return data.requests().stream()
                .filter(request -> request.requestId().equals(requestId))
                .findFirst()
                .orElseThrow();
    }

    private static LoanRequest approved(String requestId, LocalDate startDate) {
        return new LoanRequest(
                requestId, "borrower", "camera1", "Academic project",
                startDate, startDate.plusDays(7), RequestStatus.APPROVED, null,
                NOW.minusSeconds(120), NOW.minusSeconds(60),
                "supervisor", NOW.minusSeconds(60), "Approved for coursework",
                null, null, null);
    }

    private static LoanRequest pending(String requestId, LocalDate startDate) {
        return new LoanRequest(
                requestId, "borrower", "camera1", "Academic project",
                startDate, startDate.plusDays(7), RequestStatus.PENDING, null,
                NOW.minusSeconds(120), NOW.minusSeconds(120),
                null, null, null, null, null, null);
    }

    private static LoanRequest collected(String requestId, LocalDate startDate) {
        return new LoanRequest(
                requestId, "borrower", "camera1", "Academic project",
                startDate, startDate.plusDays(7), RequestStatus.COLLECTED, "loan-1",
                NOW.minusSeconds(120), NOW.minusSeconds(60),
                "supervisor", NOW.minusSeconds(60), "Approved for coursework",
                null, null, null);
    }

    private static LoanDeskData dataWith(LoanRequest... requests) {
        return new LoanDeskData(
                List.of(new User("borrower", Role.BORROWER)),
                List.of(),
                List.of(new Equipment("camera1", "Camera 1")),
                List.of(requests),
                List.of());
    }

    private DatabaseDataStore storeWith(LoanRequest... requests) throws Exception {
        DatabaseDataStore store = new DatabaseDataStore(temporaryDirectory.resolve("loandesk"));
        store.loadOrSeed();
        store.save(dataWith(requests));
        return store;
    }

    private static final class CountingDataStore implements DataStore {
        private final LoanDeskData data;
        private int saves;

        private CountingDataStore(LoanDeskData data) {
            this.data = data;
        }

        @Override
        public LoanDeskData loadOrSeed() {
            return data;
        }

        @Override
        public void save(LoanDeskData ignored) throws IOException {
            saves++;
        }
    }
}
