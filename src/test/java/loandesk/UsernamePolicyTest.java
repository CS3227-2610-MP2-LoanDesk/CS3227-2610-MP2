package loandesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import loandesk.application.UsernamePolicy;

class UsernamePolicyTest {
    @Test
    void trimsAndNormalizesUsername() {
        assertEquals("testborrower1", UsernamePolicy.normalize(" TestBorrower1 "));
    }

    @Test
    void rejectsBlankInvalidAndTooLongUsernames() {
        assertThrows(IllegalArgumentException.class, () -> UsernamePolicy.normalize(" "));
        assertThrows(IllegalArgumentException.class, () -> UsernamePolicy.normalize("two words"));
        assertThrows(IllegalArgumentException.class, () -> UsernamePolicy.normalize("bad!"));
        assertThrows(IllegalArgumentException.class, () -> UsernamePolicy.normalize("a".repeat(31)));
    }
}
