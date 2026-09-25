package loandesk;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import loandesk.application.AuthenticationService;
import loandesk.application.BorrowerRequestService;
import loandesk.application.CatalogueService;
import loandesk.application.Session;
import loandesk.domain.Equipment;
import loandesk.domain.LoanRequest;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DataStore;
import loandesk.persistence.DatabaseDataStore;

public final class LoanDeskApp extends Application {
    private final Session session = new Session();
    private AuthenticationService authenticationService;
    private CatalogueService catalogueService;
    private BorrowerRequestService borrowerRequestService;
    private DataStore dataStore;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            dataStore = new DatabaseDataStore(Path.of("data", "loandesk"));
            authenticationService = new AuthenticationService(dataStore);
            catalogueService = new CatalogueService(dataStore, session);
            borrowerRequestService = new BorrowerRequestService(dataStore, session);
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
                    requestsButton(),
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

    private Button requestsButton() {
        Button requests = new Button("My Requests");
        requests.setOnAction(event -> showMyRequests());
        return requests;
    }

    private void showMyRequests() {
        VBox content = layout("My Requests", "Your active requests and request history.");
        Label feedback = new Label();
        ListView<LoanRequest> requests = new ListView<>();
        requests.setPrefHeight(220);
        requests.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LoanRequest item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : item.status() + " — " + item.equipmentId()
                                + " (" + item.startDate() + " to " + item.dueDate() + ")");
            }
        });

        Label details = new Label(
                "Select a request to view its details.\n"
                        + "This MVP screen is read-only; editing and cancellation will be added later.");
        details.setWrapText(true);
        ScrollPane detailsPane = new ScrollPane(details);
        detailsPane.setFitToWidth(true);
        detailsPane.setPrefViewportHeight(140);
        Label cancellationInfo = new Label();
        ComboBox<String> cancellationReason = new ComboBox<>();
        cancellationReason.getItems().addAll(
                "No longer needed",
                "Plans changed",
                "Requested dates changed",
                "Unable to collect the equipment",
                "Submitted the request by mistake",
                "Other");
        cancellationReason.setPromptText("Cancellation reason");
        TextField otherCancellationReason = new TextField();
        otherCancellationReason.setPromptText("Explain the cancellation");
        Button cancel = new Button("Cancel request");
        otherCancellationReason.setVisible(false);
        otherCancellationReason.setManaged(false);
        cancellationReason.setVisible(false);
        cancellationReason.setManaged(false);
        cancel.setVisible(false);
        cancel.setManaged(false);
        cancel.setDisable(true);
        Map<String, String> equipmentNames;
        try {
            equipmentNames = catalogueService.loadCatalogue().stream()
                    .collect(Collectors.toMap(Equipment::id, Equipment::name));
            requests.getItems().setAll(borrowerRequestService.listOwnRequests());
            if (requests.getItems().isEmpty()) {
                feedback.setText("You have no requests yet.");
            } else {
                feedback.setText(requests.getItems().size() + " request(s) found.");
            }
        } catch (IOException | IllegalStateException exception) {
            feedback.setText("Unable to load your requests: " + exception.getMessage());
            equipmentNames = Map.of();
        }

        Map<String, String> names = equipmentNames;
        requests.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    cancellationReason.setValue(null);
                    otherCancellationReason.clear();
                    renderRequestDetails(details, selected, names);
                    refreshCancellationControls(
                            selected, cancellationInfo, cancellationReason,
                            otherCancellationReason, cancel);
                });
        cancellationReason.valueProperty().addListener((observable, oldValue, newValue) -> {
            boolean isOther = "Other".equals(newValue);
            otherCancellationReason.setVisible(isOther);
            otherCancellationReason.setManaged(isOther);
            refreshCancellationControls(
                    requests.getSelectionModel().getSelectedItem(), cancellationInfo,
                    cancellationReason, otherCancellationReason, cancel);
        });
        otherCancellationReason.textProperty().addListener((observable, oldValue, newValue) ->
                refreshCancellationControls(
                        requests.getSelectionModel().getSelectedItem(), cancellationInfo,
                        cancellationReason, otherCancellationReason, cancel));
        cancel.setOnAction(event -> {
            LoanRequest selected = requests.getSelectionModel().getSelectedItem();
            String reason = "Other".equals(cancellationReason.getValue())
                    ? otherCancellationReason.getText()
                    : cancellationReason.getValue();
            Alert confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Cancel this borrowing request?\nReason: " + reason);
            confirmation.setTitle("Confirm cancellation");
            confirmation.setHeaderText("Cancel request");
            if (confirmation.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
                return;
            }
            try {
                borrowerRequestService.cancelRequest(selected.requestId(), reason);
                String successMessage = selected.status() == RequestStatus.APPROVED
                        ? "The request was cancelled and its reservation was released."
                        : "The request was cancelled.";
                new Alert(Alert.AlertType.INFORMATION,
                        successMessage)
                        .showAndWait();
                showMyRequests();
            } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                feedback.setText(exception.getMessage());
                feedback.setStyle("-fx-text-fill: #b00020;");
            }
        });
        Button back = new Button("Back to dashboard");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        content.getChildren().addAll(
                feedback,
                requests,
                detailsPane,
                cancellationInfo,
                cancellationReason,
                otherCancellationReason,
                cancel,
                back);
        showScene(content, 560, 500);
    }

    private void refreshCancellationControls(
            LoanRequest request,
            Label info,
            ComboBox<String> reason,
            TextField otherReason,
            Button cancel) {
        boolean eligible = request != null
                && (request.status() == RequestStatus.PENDING
                || request.status() == RequestStatus.APPROVED)
                && request.startDate().isAfter(LocalDate.now());
        String message;
        if (request == null) {
            message = "";
        } else if (request.status() != RequestStatus.PENDING
                && request.status() != RequestStatus.APPROVED) {
            message = "Cancellation is unavailable because this request is "
                    + request.status() + ".";
        } else if (!request.startDate().isAfter(LocalDate.now())) {
            message = "Cancellation is unavailable because the start date is today or has passed.";
        } else {
            message = "Cancellation is available for this future request.";
        }
        info.setText(message);
        reason.setVisible(eligible);
        reason.setManaged(eligible);
        otherReason.setVisible(eligible && "Other".equals(reason.getValue()));
        otherReason.setManaged(eligible && "Other".equals(reason.getValue()));
        cancel.setVisible(eligible);
        cancel.setManaged(eligible);
        boolean hasReason = reason.getValue() != null
                && (!"Other".equals(reason.getValue()) || !otherReason.getText().isBlank());
        cancel.setDisable(!eligible || !hasReason);
    }

    private void renderRequestDetails(
            Label details, LoanRequest request, Map<String, String> equipmentNames) {
        if (request == null) {
            details.setText("Select a request to view its details.\n"
                    + "This MVP screen is read-only apart from eligible request cancellation.");
            return;
        }
        String equipmentName = equipmentNames.getOrDefault(request.equipmentId(), "Unknown equipment");
        StringBuilder text = new StringBuilder()
                .append("Equipment: ").append(equipmentName).append(" (" ).append(request.equipmentId()).append(")\n")
                .append("Purpose: ").append(request.purpose()).append("\n")
                .append("Dates: ").append(request.startDate()).append(" to ").append(request.dueDate()).append("\n")
                .append("Status: ").append(request.status());
        if (request.decisionReason() != null) {
            text.append("\nDecision reason: ").append(request.decisionReason());
        }
        if (request.cancellationReason() != null) {
            text.append("\nCancellation reason: ").append(request.cancellationReason());
        }
        details.setText(text.toString());
    }

    private void showCatalogue() {
        VBox content = layout("Catalogue", "Search equipment by name.");
        TextField filter = new TextField();
        filter.setPromptText("Name filter");
        Button apply = new Button("Filter");
        Button clear = new Button("Clear");
        Button request = new Button("Request selected equipment");
        Button back = new Button("Back");
        HBox filterActions = new HBox(12, apply, clear);
        filterActions.setAlignment(Pos.CENTER);
        Label feedback = new Label();
        ListView<Equipment> results = new ListView<>();
        results.setPrefHeight(180);
        results.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Equipment item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.id() + " — " + item.name());
            }
        });
        request.disableProperty().bind(results.getSelectionModel().selectedItemProperty().isNull());

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
            results.getItems().setAll(filtered);
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
        request.setOnAction(event -> showRequestForm(results.getSelectionModel().getSelectedItem()));
        back.setOnAction(event -> openDashboard(session.requireUser()));
        renderResults.run();

        content.getChildren().addAll(filter, filterActions, feedback, results, request, back);
        showScene(content, 480, 420);
    }

    private void showRequestForm(Equipment equipment) {
        VBox content = layout("Request equipment", "Submit one borrowing request.");
        Label selected = new Label(equipment.id() + " — " + equipment.name());
        ComboBox<String> purpose = new ComboBox<>();
        purpose.getItems().addAll(
                "Academic project",
                "Personal use",
                "Event or club activity",
                "Research or lab work",
                "Other");
        purpose.setValue(purpose.getItems().get(0));

        TextField otherPurpose = new TextField();
        otherPurpose.setPromptText("Explain the purpose");
        otherPurpose.setVisible(false);
        otherPurpose.setManaged(false);
        purpose.valueProperty().addListener((observable, oldValue, newValue) -> {
            boolean isOther = "Other".equals(newValue);
            otherPurpose.setVisible(isOther);
            otherPurpose.setManaged(isOther);
        });

        DatePicker startDate = new DatePicker(LocalDate.now());
        DatePicker dueDate = new DatePicker(startDate.getValue().plusDays(14));
        startDate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null
                    && (dueDate.getValue() == null
                    || oldValue != null && dueDate.getValue().equals(oldValue.plusDays(14)))) {
                dueDate.setValue(newValue.plusDays(14));
            }
        });

        Label feedback = new Label();
        feedback.setWrapText(true);
        Button submit = new Button("Submit request");
        Button back = new Button("Back to catalogue");
        submit.setOnAction(event -> {
            String selectedPurpose = "Other".equals(purpose.getValue())
                    ? otherPurpose.getText()
                    : purpose.getValue();
            try {
                var request = borrowerRequestService.submitRequest(
                        equipment.id(), selectedPurpose, startDate.getValue(), dueDate.getValue());
                Alert confirmation = new Alert(
                        Alert.AlertType.INFORMATION,
                        "Request submitted.\n"
                                + equipment.name() + "\n"
                                + request.startDate() + " to " + request.dueDate() + "\n"
                                + "Status: " + request.status());
                confirmation.setTitle("Request submitted");
                confirmation.setHeaderText("Your request is pending review.");
                confirmation.showAndWait();
                openDashboard(session.requireUser());
            } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                feedback.setText(exception.getMessage());
                feedback.setStyle("-fx-text-fill: #b00020;");
            }
        });
        back.setOnAction(event -> showCatalogue());

        content.getChildren().addAll(
                selected,
                new Label("Purpose"),
                purpose,
                otherPurpose,
                new Label("Start date"),
                startDate,
                new Label("Due date"),
                dueDate,
                feedback,
                submit,
                back);
        showScene(content, 520, 600);
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
