package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import loandesk.application.Session;
import loandesk.domain.Role;
import loandesk.domain.User;

class SessionTest {
    private final User borrower = new User("borrower", Role.BORROWER);
    private final User supervisor = new User("supervisor", Role.SUPERVISOR);

    @Test
    void newSessionHasNoAuthenticatedUser() {
        Session session = new Session();

        assertFalse(session.isActive());
        assertThrows(IllegalStateException.class, session::requireUser);
    }

    @Test
    void startAuthenticatesAndReturnsTheSameUser() {
        Session session = new Session();

        session.start(borrower);

        assertTrue(session.isActive());
        assertSame(borrower, session.requireUser());
    }

    @Test
    void clearRemovesAuthentication() {
        Session session = new Session();
        session.start(borrower);

        session.clear();

        assertFalse(session.isActive());
        assertThrows(IllegalStateException.class, session::requireUser);
    }

    @Test
    void requireRoleAcceptsTheAuthenticatedRole() {
        Session session = new Session();
        session.start(borrower);

        assertSame(borrower, session.requireRole(Role.BORROWER));
    }

    @Test
    void requireRoleRejectsAnAuthenticatedUserWithADifferentRole() {
        Session session = new Session();
        session.start(supervisor);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> session.requireRole(Role.BORROWER));

        assertEquals("The logged-in user does not have the required role.", exception.getMessage());
        assertTrue(session.isActive());
        assertSame(supervisor, session.requireUser());
    }
}
