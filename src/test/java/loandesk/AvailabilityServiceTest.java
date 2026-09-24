package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import loandesk.application.AvailabilityService;
import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.Loan;
import loandesk.domain.LoanRequest;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;

class AvailabilityServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);
    private static final Equipment CAMERA = new Equipment("camera1", "Camera 1");

    @Test
    void physicalUnavailabilityOverridesLoanAndReservationStates() {
        Equipment damaged = new Equipment("camera1", "Camera 1", EquipmentCondition.DAMAGED);

        assertEquals(AvailabilityStatus.UNAVAILABLE, new AvailabilityService().calculate(
                damaged, List.of(approvedRequest()), List.of(activeLoan()), TODAY));
    }

    @Test
    void activeLoanTakesPrecedenceOverApprovedReservation() {
        assertEquals(AvailabilityStatus.ON_LOAN, new AvailabilityService().calculate(
                CAMERA, List.of(approvedRequest()), List.of(activeLoan()), TODAY));
    }

    @Test
    void approvedFutureRequestReservesEquipment() {
        assertEquals(AvailabilityStatus.RESERVED, new AvailabilityService().calculate(
                CAMERA, List.of(approvedRequest()), List.of(), TODAY));
    }

    @Test
    void pendingRequestDoesNotReserveEquipment() {
        LoanRequest pending = new LoanRequest(
                "request-1", "borrower", "camera1", "Academic project", TODAY,
                TODAY.plusDays(14), RequestStatus.PENDING, null,
                Instant.parse("2026-09-25T08:00:00Z"), Instant.parse("2026-09-25T08:00:00Z"),
                null, null, null, null, null, null);

        assertEquals(AvailabilityStatus.AVAILABLE, new AvailabilityService().calculate(
                CAMERA, List.of(pending), List.of(), TODAY));
    }

    @Test
    void approvedRequestPastItsCollectionDateDoesNotReserveEquipment() {
        LoanRequest expiredCandidate = new LoanRequest(
                "request-expired", "borrower", "camera1", "Academic project",
                TODAY.minusDays(1), TODAY.plusDays(13), RequestStatus.APPROVED, null,
                Instant.parse("2026-09-24T08:00:00Z"), Instant.parse("2026-09-24T08:00:00Z"),
                "supervisor", Instant.parse("2026-09-24T09:00:00Z"), "Approved",
                null, null, null);

        assertEquals(AvailabilityStatus.AVAILABLE, new AvailabilityService().calculate(
                CAMERA, List.of(expiredCandidate), List.of(), TODAY));
    }

    @Test
    void lostLoanMakesEquipmentUnavailable() {
        Loan lostLoan = new Loan(
                "loan-lost", "request-lost", "borrower", "camera1", TODAY,
                TODAY.plusDays(14), null, LoanStatus.LOST);

        assertEquals(AvailabilityStatus.UNAVAILABLE, new AvailabilityService().calculate(
                CAMERA, List.of(), List.of(lostLoan), TODAY));
    }

    private static LoanRequest approvedRequest() {
        return new LoanRequest(
                "request-1", "borrower", "camera1", "Academic project", TODAY.plusDays(1),
                TODAY.plusDays(15), RequestStatus.APPROVED, null,
                Instant.parse("2026-09-25T08:00:00Z"), Instant.parse("2026-09-25T08:00:00Z"),
                "supervisor", Instant.parse("2026-09-25T09:00:00Z"), "Approved",
                null, null, null);
    }

    private static Loan activeLoan() {
        return new Loan(
                "loan-1", "request-1", "borrower", "camera1", TODAY,
                TODAY.plusDays(14), null, LoanStatus.ACTIVE);
    }
}
