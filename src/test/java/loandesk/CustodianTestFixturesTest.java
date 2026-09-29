package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import loandesk.domain.EquipmentCondition;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;

class CustodianTestFixturesTest {
    @Test
    void suppliesAuthenticatedSessionsAndValidCustodianWorkflowRecords() {
        assertEquals(Role.CUSTODIAN,
                CustodianTestFixtures.authenticatedSession(Role.CUSTODIAN).requireUser().role());
        assertEquals(RequestStatus.APPROVED, CustodianTestFixtures.approvedRequest().status());
        assertEquals(LoanStatus.ACTIVE, CustodianTestFixtures.activeLoan().status());
        assertEquals(LoanStatus.LOST, CustodianTestFixtures.lostLoan().status());
        assertTrue(java.util.Arrays.stream(EquipmentCondition.values())
                .allMatch(condition -> CustodianTestFixtures.equipment(condition).condition() == condition));
    }
}
