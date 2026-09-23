package loandesk.domain;

public record PasswordCredential(
        String username,
        String algorithm,
        int iterations,
        String salt,
        String hash) {
}
