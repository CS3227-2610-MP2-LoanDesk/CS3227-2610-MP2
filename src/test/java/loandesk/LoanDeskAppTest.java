package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoanDeskAppTest {
    @Test
    void applicationHasExpectedName() {
        assertEquals("LoanDesk", LoanDeskApp.class.getSimpleName().replace("App", ""));
    }
}
