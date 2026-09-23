package loandesk;

import java.io.IOException;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import loandesk.application.AuthenticationService;
import loandesk.application.CatalogueService;
import loandesk.application.Session;
import loandesk.domain.Equipment;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DataStore;
import loandesk.persistence.DatabaseDataStore;

public final class LoanDeskApp extends Application {
    private final Session session = new Session();
    private AuthenticationService authenticationService;
    private CatalogueService catalogueService;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            DataStore dataStore = new DatabaseDataStore(Path.of("data", "loandesk"));
            authenticationService = new AuthenticationService(dataStore);
            catalogueService = new CatalogueService(dataStore);
        } catch (IOException exception) {
            showError("Unable to load LoanDesk data", exception.getMessage());
            return;
        }
        showRoleSelection();
    }

    private void showRoleSelection() {
        VBox content = layout("LoanDesk", "Choose a role to continue.");
        Button borrower = new Button("Borrower");
        Button supervisor = new Button("Supervisor");
        Button custodian = new Button("Custodian");
        borrower.setOnAction(event -> showBorrowerOptions());
        supervisor.setOnAction(event -> openDashboard(authenticationService.loginStaff(Role.SUPERVISOR)));
        custodian.setOnAction(event -> openDashboard(authenticationService.loginStaff(Role.CUSTODIAN)));
        content.getChildren().addAll(borrower, supervisor, custodian);
        showScene(content);
    }

    private void showBorrowerOptions() {
        VBox content = layout("Borrower", "Log in to an existing account or sign up.");
        Button login = new Button("Log in");
        Button signUp = new Button("Sign up");
        Button back = new Button("Back");
        login.setOnAction(event -> showBorrowerForm(false));
        signUp.setOnAction(event -> showBorrowerForm(true));
        back.setOnAction(event -> showRoleSelection());
        content.getChildren().addAll(login, signUp, back);
        showScene(content);
    }

    private void showBorrowerForm(boolean signUp) {
        String action = signUp ? "Sign up" : "Log in";
        VBox content = layout("Borrower " + action, "Enter a username to continue.");
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Button submit = new Button(action);
        Button back = new Button("Back");
        Label feedback = new Label();
        submit.setOnAction(event -> {
            try {
                User borrower = signUp
                        ? authenticationService.signUpBorrower(username.getText(), password.getText())
                        : authenticationService.loginBorrower(username.getText(), password.getText());
                openDashboard(borrower);
            } catch (IllegalArgumentException | IOException exception) {
                feedback.setText(exception.getMessage());
            } finally {
                password.clear();
            }
        });
        back.setOnAction(event -> showBorrowerOptions());
        content.getChildren().addAll(username, password, submit, feedback, back);
        showScene(content);
    }

    private void openDashboard(User user) {
        session.start(user);
        String title = switch (user.role()) {
            case BORROWER -> "Borrower Dashboard";
            case SUPERVISOR -> "Supervisor Dashboard";
            case CUSTODIAN -> "Custodian Dashboard";
        };
        VBox content = layout(title, "The shared foundation is ready for role features.");
        if (user.role() == Role.BORROWER) {
            content.getChildren().addAll(
                    catalogueButton(),
                    new Button("My Requests"),
                    new Button("My Loans"));
        } else if (user.role() == Role.SUPERVISOR) {
            content.getChildren().addAll(
                    new Button("Review Queue"),
                    new Button("Decision History"),
                    new Button("Account"));
        } else {
            content.getChildren().addAll(
                    new Button("Inventory"),
                    new Button("Collections"),
                    new Button("Maintenance"));
        }
        Button logout = new Button("Log out");
        logout.setOnAction(event -> {
            session.clear();
            showRoleSelection();
        });
        content.getChildren().add(logout);
        showScene(content);
    }

    private Button catalogueButton() {
        Button catalogue = new Button("Catalogue");
        catalogue.setOnAction(event -> showCatalogue());
        return catalogue;
    }

    private void showCatalogue() {
        VBox content = layout("Catalogue", "Search equipment by name.");
        TextField filter = new TextField();
        filter.setPromptText("Name filter");
        Button apply = new Button("Filter");
        Button clear = new Button("Clear");
        Button back = new Button("Back");
        HBox filterActions = new HBox(12, apply, clear);
        filterActions.setAlignment(Pos.CENTER);
        Label feedback = new Label();
        ListView<String> results = new ListView<>();
        results.setPrefHeight(180);

        final java.util.List<Equipment> equipment;
        try {
            equipment = catalogueService.loadCatalogue();
        } catch (IOException exception) {
            feedback.setText("Unable to load the catalogue: " + exception.getMessage());
            back.setOnAction(event -> openDashboard(session.requireUser()));
            content.getChildren().addAll(feedback, back);
            showScene(content);
            return;
        }

        Runnable renderResults = () -> {
            java.util.List<Equipment> filtered = catalogueService.filterByName(equipment, filter.getText());
            results.getItems().setAll(filtered.stream()
                    .map(item -> item.id() + " — " + item.name())
                    .toList());
            feedback.setText(filtered.isEmpty()
                    ? (equipment.isEmpty()
                            ? "The catalogue is currently empty."
                            : "No equipment matches that name.")
                    : filtered.size() + " equipment item(s) found.");
        };
        apply.setOnAction(event -> renderResults.run());
        clear.setOnAction(event -> {
            filter.clear();
            renderResults.run();
        });
        filter.setOnAction(event -> renderResults.run());
        back.setOnAction(event -> openDashboard(session.requireUser()));
        renderResults.run();

        content.getChildren().addAll(filter, filterActions, feedback, results, back);
        showScene(content, 480, 420);
    }

    private VBox layout(String title, String subtitle) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label description = new Label(subtitle);
        VBox content = new VBox(12, heading, description);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        return content;
    }

    private void showScene(VBox content) {
        showScene(content, 480, 360);
    }

    private void showScene(VBox content, double width, double height) {
        stage.setTitle("LoanDesk");
        stage.setScene(new Scene(content, width, height));
        stage.show();
    }

    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error." : message)
                .showAndWait();
        stage.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
