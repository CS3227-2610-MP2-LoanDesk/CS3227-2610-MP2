final class CheckoutService {
    boolean checkout(boolean custodian, boolean approved, boolean inWindow,
            boolean availableGood, String existingLoanId) {
        return custodian && approved && inWindow && availableGood && existingLoanId == null;
    }
}
