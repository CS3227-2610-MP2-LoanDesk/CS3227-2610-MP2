package loandesk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import loandesk.application.Permission;
import loandesk.application.PermissionService;
import loandesk.application.Session;
import loandesk.domain.Role;
import loandesk.domain.User;

class PermissionServiceTest {
    private final User borrower = new User("borrower", Role.BORROWER);
    private final User supervisor = new User("supervisor", Role.SUPERVISOR);
    private final User custodian = new User("custodian", Role.CUSTODIAN);

    @Test
    void requireRejectsAnUnauthenticatedSession() {
        PermissionService permissions = new PermissionService(new Session());

        assertThrows(IllegalStateException.class,
                () -> permissions.require(Permission.BROWSE_CATALOGUE));
        assertFalse(permissions.isGranted(Permission.BROWSE_CATALOGUE));
    }

    @Test
    void requireReturnsTheSignedInUserForAGrantedPermission() {
        Session session = new Session();
        session.start(supervisor);
        PermissionService permissions = new PermissionService(session);

        assertSame(supervisor, permissions.require(Permission.APPROVE_REQUEST));
        assertTrue(permissions.isGranted(Permission.REVIEW_REQUESTS));
    }

    @Test
    void requireRejectsAPermissionTheSignedInRoleDoesNotHold() {
        Session session = new Session();
        session.start(borrower);
        PermissionService permissions = new PermissionService(session);

        assertThrows(IllegalStateException.class,
                () -> permissions.require(Permission.APPROVE_REQUEST));
        assertFalse(permissions.isGranted(Permission.APPROVE_REQUEST));
    }

    @Test
    void borrowersCannotDecideAndSupervisorsCannotBorrow() {
        assertFalse(PermissionService.grants(Role.BORROWER, Permission.REJECT_REQUEST));
        assertFalse(PermissionService.grants(Role.BORROWER, Permission.CANCEL_APPROVED_REQUEST));
        assertFalse(PermissionService.grants(Role.SUPERVISOR, Permission.SUBMIT_REQUEST));
        assertFalse(PermissionService.grants(Role.SUPERVISOR, Permission.BROWSE_CATALOGUE));
    }

    @Test
    void supervisorsCannotChangePhysicalEquipmentAndCustodiansCannotDecide() {
        assertFalse(PermissionService.grants(Role.SUPERVISOR, Permission.MANAGE_EQUIPMENT));
        assertFalse(PermissionService.grants(Role.SUPERVISOR, Permission.RECORD_MAINTENANCE));
        assertFalse(PermissionService.grants(Role.CUSTODIAN, Permission.APPROVE_REQUEST));
        assertFalse(PermissionService.grants(Role.CUSTODIAN, Permission.REJECT_REQUEST));
        assertTrue(PermissionService.grants(Role.CUSTODIAN, Permission.CHECK_OUT_LOAN));
    }

    @Test
    void everyPermissionIsHeldByExactlyOneRole() {
        for (Permission permission : Permission.values()) {
            long holders = EnumSet.allOf(Role.class).stream()
                    .filter(role -> PermissionService.grants(role, permission))
                    .count();
            assertTrue(holders == 1,
                    permission + " should be held by exactly one role but was held by " + holders);
        }
    }

    @Test
    void theMatrixIsUnmodifiable() {
        Set<Permission> borrowerPermissions = PermissionService.permissionsFor(Role.BORROWER);

        assertThrows(UnsupportedOperationException.class,
                () -> borrowerPermissions.add(Permission.APPROVE_REQUEST));
    }

    @Test
    void custodianSessionsAreNotGrantedBorrowerReads() {
        Session session = new Session();
        session.start(custodian);
        PermissionService permissions = new PermissionService(session);

        assertThrows(IllegalStateException.class,
                () -> permissions.require(Permission.VIEW_OWN_LOANS));
    }
}
