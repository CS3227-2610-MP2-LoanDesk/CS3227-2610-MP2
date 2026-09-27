package loandesk.application;

/**
 * Named operations guarded by the shared permission matrix.
 *
 * <p>Every role-restricted service method names one of these instead of
 * checking a {@link loandesk.domain.Role} directly, so the whole matrix can be
 * read in {@link PermissionService}.
 */
public enum Permission {
    BROWSE_CATALOGUE,
    SUBMIT_REQUEST,
    EDIT_OWN_REQUEST,
    CANCEL_OWN_REQUEST,
    VIEW_OWN_REQUESTS,
    VIEW_OWN_LOANS,

    REVIEW_REQUESTS,
    APPROVE_REQUEST,
    REJECT_REQUEST,
    CANCEL_APPROVED_REQUEST,
    VIEW_DECISION_HISTORY,

    MANAGE_EQUIPMENT,
    CHECK_OUT_LOAN,
    RECORD_RETURN,
    RECORD_MAINTENANCE
}
