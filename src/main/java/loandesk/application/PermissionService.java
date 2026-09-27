package loandesk.application;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import loandesk.domain.Role;
import loandesk.domain.User;

/**
 * The single place that decides which role may perform which operation.
 *
 * <p>Services call {@link #require(Permission)} instead of checking a role
 * inline, so the permission matrix is declared once and every service shares
 * the same authenticated-session and denial behaviour. Record ownership, such
 * as a borrower reading only their own requests, remains the responsibility of
 * the owning service.
 */
public final class PermissionService {
    private static final Map<Role, Set<Permission>> MATRIX = buildMatrix();

    private final Session session;

    public PermissionService(Session session) {
        this.session = Objects.requireNonNull(session);
    }

    /**
     * Returns the signed-in user when their role grants the permission.
     *
     * @throws IllegalStateException when no user is signed in, or when the
     *     signed-in role does not hold the permission
     */
    public User require(Permission permission) {
        Objects.requireNonNull(permission);
        User currentUser = session.requireUser();
        if (!grants(currentUser.role(), permission)) {
            throw new IllegalStateException("The signed-in role is not permitted to perform "
                    + permission + ".");
        }
        return currentUser;
    }

    /** Reports whether the signed-in user holds the permission, without throwing. */
    public boolean isGranted(Permission permission) {
        Objects.requireNonNull(permission);
        return session.isActive() && grants(session.requireUser().role(), permission);
    }

    /** Reports whether a role holds a permission, independently of any session. */
    public static boolean grants(Role role, Permission permission) {
        Objects.requireNonNull(role);
        Objects.requireNonNull(permission);
        return MATRIX.get(role).contains(permission);
    }

    /** Returns the unmodifiable permission set for one role. */
    public static Set<Permission> permissionsFor(Role role) {
        Objects.requireNonNull(role);
        return MATRIX.get(role);
    }

    private static Map<Role, Set<Permission>> buildMatrix() {
        Map<Role, Set<Permission>> matrix = new EnumMap<>(Role.class);
        matrix.put(Role.BORROWER, Collections.unmodifiableSet(EnumSet.of(
                Permission.BROWSE_CATALOGUE,
                Permission.SUBMIT_REQUEST,
                Permission.EDIT_OWN_REQUEST,
                Permission.CANCEL_OWN_REQUEST,
                Permission.VIEW_OWN_REQUESTS,
                Permission.VIEW_OWN_LOANS)));
        matrix.put(Role.SUPERVISOR, Collections.unmodifiableSet(EnumSet.of(
                Permission.REVIEW_REQUESTS,
                Permission.APPROVE_REQUEST,
                Permission.REJECT_REQUEST,
                Permission.CANCEL_APPROVED_REQUEST,
                Permission.VIEW_DECISION_HISTORY)));
        matrix.put(Role.CUSTODIAN, Collections.unmodifiableSet(EnumSet.of(
                Permission.MANAGE_EQUIPMENT,
                Permission.CHECK_OUT_LOAN,
                Permission.RECORD_RETURN,
                Permission.RECORD_MAINTENANCE)));
        return Collections.unmodifiableMap(matrix);
    }
}
