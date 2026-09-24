package loandesk.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import loandesk.domain.AvailabilityStatus;
import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.Loan;
import loandesk.domain.LoanStatus;
import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;

public final class AvailabilityService {
    public AvailabilityStatus calculate(
            Equipment equipment,
            List<LoanRequest> requests,
            List<Loan> loans,
            LocalDate today) {
        Objects.requireNonNull(equipment);
        Objects.requireNonNull(requests);
        Objects.requireNonNull(loans);
        Objects.requireNonNull(today);

        if (equipment.condition() != EquipmentCondition.GOOD
                || loans.stream().anyMatch(loan -> matchesEquipment(loan, equipment)
                        && loan.status() == LoanStatus.LOST)) {
            return AvailabilityStatus.UNAVAILABLE;
        }
        if (loans.stream().anyMatch(loan -> matchesEquipment(loan, equipment)
                && loan.status() == LoanStatus.ACTIVE)) {
            return AvailabilityStatus.ON_LOAN;
        }
        if (requests.stream().anyMatch(request -> request.equipmentId().equals(equipment.id())
                && request.status() == RequestStatus.APPROVED
                && !request.startDate().isBefore(today))) {
            return AvailabilityStatus.RESERVED;
        }
        return AvailabilityStatus.AVAILABLE;
    }

    private static boolean matchesEquipment(Loan loan, Equipment equipment) {
        return loan.equipmentId().equals(equipment.id());
    }
}
