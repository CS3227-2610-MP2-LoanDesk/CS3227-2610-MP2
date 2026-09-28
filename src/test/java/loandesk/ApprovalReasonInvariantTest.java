package loandesk;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;

/** Policy: a reason is required to reject a request but optional to approve one. */
class ApprovalReasonInvariantTest {
    private static final Instant NOW = Instant.parse("2026-09-28T09:00:00Z");
    private static final LocalDate START = LocalDate.of(2026, 9, 29);
    private static final LocalDate DUE = LocalDate.of(2026, 10, 5);

    @Test
    void approvalWithoutAReasonIsAccepted() {
        LoanRequest approved = decided(RequestStatus.APPROVED, null);

        assertNull(approved.decisionReason());
    }

    @Test
    void rejectionWithoutAReasonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> decided(RequestStatus.REJECTED, null));
        assertThrows(IllegalArgumentException.class,
                () -> decided(RequestStatus.REJECTED, "   "));
    }

    @Test
    void anyDecisionStillRequiresItsOwnerAndTime() {
        assertThrows(IllegalArgumentException.class, () -> new LoanRequest(
                "request1", "borrower", "camera1", "Coursework", START, DUE,
                RequestStatus.APPROVED, null, NOW, NOW,
                null, NOW, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new LoanRequest(
                "request1", "borrower", "camera1", "Coursework", START, DUE,
                RequestStatus.APPROVED, null, NOW, NOW,
                "supervisor", null, null, null, null, null));
    }

    private static LoanRequest decided(RequestStatus status, String reason) {
        return new LoanRequest(
                "request1", "borrower", "camera1", "Coursework", START, DUE,
                status, null, NOW, NOW,
                "supervisor", NOW, reason, null, null, null);
    }
}
