package evaluation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import evaluation.ReturnStore.Condition;
import evaluation.ReturnStore.Equipment;
import evaluation.ReturnStore.Loan;
import evaluation.ReturnStore.LoanStatus;
import evaluation.ReturnStore.Sink;

/** Shared contract tests run against both fixture cases. */
class ReturnStoreContractTest {
    private static Map<String, Loan> seedLoans() {
        return Map.of(
                "loan-1", new Loan("loan-1", "camera1", LoanStatus.ACTIVE),
                "loan-2", new Loan("loan-2", "camera2", LoanStatus.RETURNED));
    }

    private static Map<String, Equipment> seedEquipment() {
        return Map.of(
                "camera1", new Equipment("camera1", Condition.GOOD),
                "camera2", new Equipment("camera2", Condition.GOOD));
    }

    /** Records what was committed; can be switched to fail every write. */
    private static final class RecordingSink implements Sink {
        private final boolean failing;
        private Map<String, Loan> committedLoans;
        private Map<String, Equipment> committedEquipment;
        private int writes;

        private RecordingSink(boolean failing) {
            this.failing = failing;
        }

        @Override
        public void write(Map<String, Loan> loans, Map<String, Equipment> equipment)
                throws IOException {
            writes++;
            if (failing) {
                throw new IOException("synthetic save failure");
            }
            committedLoans = new HashMap<>(loans);
            committedEquipment = new HashMap<>(equipment);
        }
    }

    private static ReturnStore store(Sink sink) {
        return new ReturnStore(seedLoans(), seedEquipment(), sink);
    }

    @Test
    void aSuccessfulDamagedReturnClosesTheLoanAndMarksTheEquipment() throws Exception {
        ReturnStore store = store(new RecordingSink(false));

        assertTrue(store.recordDamagedReturn("loan-1"));

        assertAll(
                () -> assertEquals(LoanStatus.RETURNED,
                        store.loanSnapshot().get("loan-1").status()),
                () -> assertEquals(Condition.DAMAGED,
                        store.equipmentSnapshot().get("camera1").condition()),
                () -> assertEquals(LoanStatus.RETURNED,
                        store.loanSnapshot().get("loan-2").status()),
                () -> assertEquals(Condition.GOOD,
                        store.equipmentSnapshot().get("camera2").condition()));
    }

    @Test
    void aSuccessfulReturnCommitsBothRecordSetsInOneWrite() throws Exception {
        RecordingSink sink = new RecordingSink(false);
        ReturnStore store = store(sink);

        store.recordDamagedReturn("loan-1");

        assertAll(
                () -> assertEquals(1, sink.writes),
                () -> assertEquals(LoanStatus.RETURNED, sink.committedLoans.get("loan-1").status()),
                () -> assertEquals(Condition.DAMAGED,
                        sink.committedEquipment.get("camera1").condition()));
    }

    @Test
    void aFailedSaveLeavesTheInMemoryStateUnchanged() {
        ReturnStore store = store(new RecordingSink(true));

        assertThrows(IOException.class, () -> store.recordDamagedReturn("loan-1"));

        assertAll(
                () -> assertEquals(seedLoans(), store.loanSnapshot()),
                () -> assertEquals(seedEquipment(), store.equipmentSnapshot()));
    }

    @Test
    void aFailedSaveCommitsNothing() {
        RecordingSink sink = new RecordingSink(true);
        ReturnStore store = store(sink);

        assertThrows(IOException.class, () -> store.recordDamagedReturn("loan-1"));

        assertAll(
                () -> assertEquals(1, sink.writes),
                () -> assertTrue(sink.committedLoans == null),
                () -> assertTrue(sink.committedEquipment == null));
    }

    @Test
    void anUnknownLoanIsRefusedWithoutWritingOrMutating() throws Exception {
        RecordingSink sink = new RecordingSink(false);
        ReturnStore store = store(sink);

        assertFalse(store.recordDamagedReturn("loan-missing"));

        assertAll(
                () -> assertEquals(0, sink.writes),
                () -> assertEquals(seedLoans(), store.loanSnapshot()),
                () -> assertEquals(seedEquipment(), store.equipmentSnapshot()));
    }

    @Test
    void anAlreadyReturnedLoanIsRefusedWithoutWritingOrMutating() throws Exception {
        RecordingSink sink = new RecordingSink(false);
        ReturnStore store = store(sink);

        assertFalse(store.recordDamagedReturn("loan-2"));

        assertAll(
                () -> assertEquals(0, sink.writes),
                () -> assertEquals(seedLoans(), store.loanSnapshot()),
                () -> assertEquals(seedEquipment(), store.equipmentSnapshot()));
    }
}
