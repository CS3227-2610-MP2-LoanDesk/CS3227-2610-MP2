package evaluation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ReturnStore {
    public enum LoanStatus { ACTIVE, RETURNED }
    public enum Condition { GOOD, DAMAGED }

    public record Loan(String id, String equipmentId, LoanStatus status) {}
    public record Equipment(String id, Condition condition) {}

    /** Writes both record sets together, or throws and writes neither. */
    public interface Sink {
        void write(Map<String, Loan> loans, Map<String, Equipment> equipment) throws IOException;
    }

    private Map<String, Loan> loans;
    private Map<String, Equipment> equipment;
    private final Sink sink;

    public ReturnStore(Map<String, Loan> loans, Map<String, Equipment> equipment, Sink sink) {
        this.loans = new HashMap<>(loans);
        this.equipment = new HashMap<>(equipment);
        this.sink = sink;
    }

    /**
     * Closes an active loan and marks its equipment damaged. Both records
     * change together or neither does.
     */
    public boolean recordDamagedReturn(String loanId) throws IOException {
        Loan loan = loans.get(loanId);
        if (loan == null || loan.status() != LoanStatus.ACTIVE) {
            return false;
        }
        Equipment item = equipment.get(loan.equipmentId());
        if (item == null) {
            return false;
        }

        Map<String, Loan> updatedLoans = new HashMap<>(loans);
        updatedLoans.put(loanId, new Loan(loan.id(), loan.equipmentId(), LoanStatus.RETURNED));
        Map<String, Equipment> updatedEquipment = new HashMap<>(equipment);
        updatedEquipment.put(item.id(), new Equipment(item.id(), Condition.DAMAGED));

        sink.write(updatedLoans, updatedEquipment);

        loans = updatedLoans;
        equipment = updatedEquipment;
        return true;
    }

    public Map<String, Loan> loanSnapshot() {
        return Map.copyOf(loans);
    }

    public Map<String, Equipment> equipmentSnapshot() {
        return Map.copyOf(equipment);
    }
}
