package loandesk.application;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.Loan;
import loandesk.domain.LoanStatus;
import loandesk.persistence.DataStore;
import loandesk.persistence.LoanDeskData;
import loandesk.persistence.StaleDataException;

/** Custodian-only physical fulfilment operations for checked-out equipment. */
public final class CustodianFulfilmentService {
    private final DataStore dataStore;
    private final PermissionService permissions;
    private final Clock clock;

    public CustodianFulfilmentService(DataStore dataStore, Session session) {
        this(dataStore, session, Clock.systemDefaultZone());
    }

    public CustodianFulfilmentService(DataStore dataStore, Session session, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.permissions = new PermissionService(Objects.requireNonNull(session));
        this.clock = Objects.requireNonNull(clock);
    }

    /** Returns loans needing physical fulfilment, with overdue active loans first. */
    public List<Loan> activeLoans() throws IOException {
        permissions.require(Permission.RECORD_RETURN);
        LocalDate today = LocalDate.now(clock);
        return dataStore.loadOrSeed().loans().stream()
                .filter(loan -> loan.status() == LoanStatus.ACTIVE || loan.status() == LoanStatus.LOST)
                .sorted(Comparator.comparingInt((Loan loan) -> loan.isOverdue(today) ? 0 : 1)
                        .thenComparing(Loan::dueDate)
                        .thenComparing(Loan::loanId))
                .toList();
    }

    /** Records a return and the observed physical condition in one saved snapshot. */
    public Loan returnLoan(String loanId, EquipmentCondition condition) throws IOException {
        permissions.require(Permission.RECORD_RETURN);
        if (condition != EquipmentCondition.GOOD && condition != EquipmentCondition.DAMAGED
                && condition != EquipmentCondition.UNDER_MAINTENANCE) {
            throw new IllegalArgumentException("A returned item must be GOOD, DAMAGED, or UNDER_MAINTENANCE.");
        }
        return replaceLoanAndEquipment(loanId, LoanStatus.ACTIVE, LoanStatus.RETURNED, condition);
    }

    /** Marks an active loan and its equipment as lost in one saved snapshot. */
    public Loan markLost(String loanId) throws IOException {
        permissions.require(Permission.RECORD_MAINTENANCE);
        return replaceLoanAndEquipment(loanId, LoanStatus.ACTIVE, LoanStatus.LOST, EquipmentCondition.LOST);
    }

    /** Restores a lost item to an active loan so its return can be recorded separately. */
    public Loan recoverLost(String loanId) throws IOException {
        permissions.require(Permission.RECORD_MAINTENANCE);
        return replaceLoanAndEquipment(loanId, LoanStatus.LOST, LoanStatus.ACTIVE, EquipmentCondition.GOOD);
    }

    private Loan replaceLoanAndEquipment(
            String loanId,
            LoanStatus expectedStatus,
            LoanStatus targetStatus,
            EquipmentCondition targetCondition) throws IOException {
        String normalizedLoanId = requireText(loanId, "Loan ID");
        LoanDeskData data = dataStore.loadOrSeed();
        int loanIndex = loanIndex(data.loans(), normalizedLoanId);
        if (loanIndex < 0) {
            throw new IllegalArgumentException("Loan was not found.");
        }
        Loan existing = data.loans().get(loanIndex);
        if (existing.status() != expectedStatus) {
            throw new IllegalStateException("This operation requires a " + expectedStatus + " loan.");
        }
        int equipmentIndex = equipmentIndex(data.equipment(), existing.equipmentId());
        if (equipmentIndex < 0) {
            throw new IllegalArgumentException("Equipment was not found.");
        }

        LocalDate today = LocalDate.now(clock);
        Loan updated = new Loan(existing.loanId(), existing.requestId(), existing.borrowerUsername(),
                existing.equipmentId(), existing.checkoutDate(), existing.dueDate(),
                targetStatus == LoanStatus.RETURNED ? today : null, targetStatus);
        Equipment currentEquipment = data.equipment().get(equipmentIndex);
        Equipment updatedEquipment = new Equipment(currentEquipment.id(), currentEquipment.name(), targetCondition);
        List<Loan> loans = new ArrayList<>(data.loans());
        loans.set(loanIndex, updated);
        List<Equipment> equipment = new ArrayList<>(data.equipment());
        equipment.set(equipmentIndex, updatedEquipment);
        save(new LoanDeskData(data.users(), data.credentials(), equipment, data.requests(), loans));
        return updated;
    }

    private void save(LoanDeskData data) throws IOException {
        try {
            dataStore.save(data);
        } catch (IOException exception) {
            if (isStaleSnapshotFailure(exception)) {
                throw new StaleDataException("LoanDesk data changed; refresh and retry.");
            }
            throw exception;
        }
    }

    private static int loanIndex(List<Loan> loans, String loanId) {
        for (int index = 0; index < loans.size(); index++) {
            if (loans.get(index).loanId().equals(loanId)) {
                return index;
            }
        }
        return -1;
    }

    private static int equipmentIndex(List<Equipment> equipment, String equipmentId) {
        for (int index = 0; index < equipment.size(); index++) {
            if (equipment.get(index).id().equals(equipmentId)) {
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
