package loandesk.application;

import java.util.Locale;

public final class UsernamePolicy {
    private UsernamePolicy() {
    }

    public static String normalize(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be blank.");
        }
        String normalized = username.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank.");
        }
        if (normalized.length() > 30) {
            throw new IllegalArgumentException("Username cannot exceed 30 characters.");
        }
        if (!normalized.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Username may contain only letters, numbers, underscores, and hyphens.");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}