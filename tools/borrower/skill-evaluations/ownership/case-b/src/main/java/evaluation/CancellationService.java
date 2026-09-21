package evaluation;

import java.util.HashMap;
import java.util.Map;

public final class CancellationService {
    public enum Role { BORROWER, STAFF }
    public enum Status { PENDING, CANCELLED }
    public record Session(String userId, Role role) {}
    public record Request(String id, String ownerId, Status status) {}

    private final Map<String, Request> requests;

    public CancellationService(Map<String, Request> seed) {
        requests = new HashMap<>(seed);
    }

    public boolean cancel(Session session, String requestId) {
        if (session == null || session.role() != Role.BORROWER) {
            return false;
        }
        Request request = requests.get(requestId);
        if (request == null || !request.ownerId().equals(session.userId())
                || request.status() != Status.PENDING) {
            return false;
        }
        requests.put(requestId, new Request(request.id(), request.ownerId(), Status.CANCELLED));
        return true;
    }

    public Map<String, Request> snapshot() {
        return Map.copyOf(requests);
    }
}
