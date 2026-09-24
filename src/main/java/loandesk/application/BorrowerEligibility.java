package loandesk.application;

import java.util.List;

public record BorrowerEligibility(boolean canSubmitRequest, List<EligibilityBlocker> blockers) {
    public BorrowerEligibility {
        blockers = List.copyOf(blockers);
    }
}
