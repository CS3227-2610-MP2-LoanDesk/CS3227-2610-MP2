package loandesk.application;

import java.util.Objects;

import loandesk.domain.Role;
import loandesk.domain.User;

public final class Session {
    private User user;

    public void start(User user) {
        this.user = Objects.requireNonNull(user);
    }

    public void clear() {
        user = null;
    }

    public boolean isActive() {
        return user != null;
    }

    public User requireUser() {
        if (user == null) {
            throw new IllegalStateException("No user is currently logged in.");
        }
        return user;
    }

    public User requireRole(Role requiredRole) {
        Objects.requireNonNull(requiredRole);
        User currentUser = requireUser();
        if (currentUser.role() != requiredRole) {
            throw new IllegalStateException("The logged-in user does not have the required role.");
        }
        return currentUser;
    }
}
