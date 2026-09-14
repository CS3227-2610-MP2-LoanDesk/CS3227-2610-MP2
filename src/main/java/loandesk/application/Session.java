package loandesk.application;

import java.util.Objects;

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
}