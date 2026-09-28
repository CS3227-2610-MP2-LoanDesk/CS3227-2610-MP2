package evaluation;

import java.util.HashMap;
import java.util.Map;

public final class JourneyService {
    public enum Role { BORROWER, SUPERVISOR, CUSTODIAN }
    public enum RequestStatus { PENDING, APPROVED, COLLECTED }
    public enum LoanStatus { ACTIVE, RETURNED }

    public record Session(String userId, Role role) {}
    public record Request(String id, String borrowerId, String equipmentId,
            RequestStatus status, String loanId) {}
    public record Loan(String id, String requestId, String borrowerId, String equipmentId,
            LoanStatus status) {}

    private final Map<String, Request> requests;
    private final Map<String, Loan> loans = new HashMap<>();
    private int nextLoan = 1;

    public JourneyService(Map<String, Request> seed) {
        requests = new HashMap<>(seed);
    }

    public boolean approve(Session session, String requestId) {
        if (session == null || session.role() != Role.SUPERVISOR) {
            return false;
        }
        Request request = requests.get(requestId);
        if (request == null || request.status() != RequestStatus.PENDING) {
            return false;
        }
        requests.put(requestId, new Request(request.id(), request.borrowerId(),
                request.equipmentId(), RequestStatus.APPROVED, request.loanId()));
        return true;
    }

    public String checkout(Session session, String requestId) {
        if (session == null || session.role() != Role.CUSTODIAN) {
            return null;
        }
        Request request = requests.get(requestId);
        if (request == null || request.status() != RequestStatus.APPROVED) {
            return null;
        }
        String loanId = "loan-" + nextLoan++;
        loans.put(loanId, new Loan(loanId, request.id(), request.borrowerId(),
                request.equipmentId(), LoanStatus.ACTIVE));
        return loanId;
    }

    public boolean recordReturn(Session session, String loanId) {
        if (session == null || session.role() != Role.CUSTODIAN) {
            return false;
        }
        Loan loan = loans.get(loanId);
        if (loan == null || loan.status() != LoanStatus.ACTIVE) {
            return false;
        }
        loans.put(loanId, new Loan(loan.id(), loan.requestId(), loan.borrowerId(),
                loan.equipmentId(), LoanStatus.RETURNED));
        return true;
    }

    public Map<String, Request> requestSnapshot() {
        return Map.copyOf(requests);
    }

    public Map<String, Loan> loanSnapshot() {
        return Map.copyOf(loans);
    }
}
