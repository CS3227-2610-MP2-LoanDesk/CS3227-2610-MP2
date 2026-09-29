package loandesk;

import javafx.application.Application;

/**
 * Entry point for the executable JAR.
 *
 * <p>Keeping this class separate from the JavaFX {@link Application} subclass
 * lets {@code java -jar} load the JavaFX classes bundled in the archive before
 * the Java launcher applies its JavaFX-specific launch handling.</p>
 */
public final class LoanDeskLauncher {
    private LoanDeskLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(LoanDeskApp.class, args);
    }
}
