package evaluation;

import java.util.HashMap;
import java.util.Map;

public final class DecisionService {
    public enum Role { BORROWER, SUPERVISOR }
    public enum Status { PENDING, APPROVED, REJECTED, COLLECTED }
    public record Session(String userId, Role role) {}
    public record Request(String id, Status status, String decidedBy, String reason) {}

    private final Map<String, Request> requests;

    public DecisionService(Map<String, Request> seed) {
        requests = new HashMap<>(seed);
    }

    public boolean approve(Session session, String requestId) {
        if (session == null || session.role() != Role.SUPERVISOR) {
            return false;
        }
        Request request = requests.get(requestId);
        if (request == null || request.status() != Status.PENDING) {
            return false;
        }
        requests.put(requestId,
                new Request(request.id(), Status.APPROVED, session.userId(), null));
        return true;
    }

    public boolean reject(Session session, String requestId, String reason) {
        if (session == null || session.role() != Role.SUPERVISOR) {
            return false;
        }
        if (reason == null || reason.isBlank()) {
            return false;
        }
        Request request = requests.get(requestId);
        if (request == null || request.status() != Status.PENDING) {
            return false;
        }
        requests.put(requestId,
                new Request(request.id(), Status.REJECTED, session.userId(), reason));
        return true;
    }

    public Map<String, Request> snapshot() {
        return Map.copyOf(requests);
    }
}
