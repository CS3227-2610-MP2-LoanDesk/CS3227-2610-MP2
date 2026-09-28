package loandesk;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Group;
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
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

import loandesk.application.AuthenticationService;
import loandesk.application.BorrowerLoanService;
import loandesk.application.BorrowerRequestService;
import loandesk.application.CatalogueService;
import loandesk.application.CustodianCollectionService;
import loandesk.application.CustodianFulfilmentService;
import loandesk.application.CustodianInventoryService;
import loandesk.application.ReviewFilter;
import loandesk.application.Session;
import loandesk.application.SupervisorRequestService;
import loandesk.domain.Equipment;
import loandesk.domain.EquipmentCondition;
import loandesk.domain.LoanRequest;
import loandesk.domain.Loan;
import loandesk.domain.LoanStatus;
import loandesk.domain.RequestStatus;
import loandesk.domain.Role;
import loandesk.domain.User;
import loandesk.persistence.DataStore;
import loandesk.persistence.DatabaseDataStore;
import loandesk.persistence.StaleDataException;

public final class LoanDeskApp extends Application {
    private static final String ANY_STATUS = "Any status";
    private static final double WINDOW_WIDTH = 1160;
    private static final double WINDOW_HEIGHT = 720;

    private final Session session = new Session();
    private AuthenticationService authenticationService;
    private CatalogueService catalogueService;
    private BorrowerRequestService borrowerRequestService;
    private BorrowerLoanService borrowerLoanService;
    private SupervisorRequestService supervisorRequestService;
    private CustodianCollectionService custodianCollectionService;
    private CustodianFulfilmentService custodianFulfilmentService;
    private CustodianInventoryService custodianInventoryService;
    private DataStore dataStore;
    private Stage stage;
    private String custodianDashboardMessage;
    private boolean custodianDashboardMessageIsError;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            dataStore = new DatabaseDataStore(Path.of("data", "loandesk"));
            authenticationService = new AuthenticationService(dataStore);
            catalogueService = new CatalogueService(dataStore, session);
            borrowerRequestService = new BorrowerRequestService(dataStore, session);
            borrowerLoanService = new BorrowerLoanService(dataStore, session);
            supervisorRequestService = new SupervisorRequestService(dataStore, session);
            custodianCollectionService = new CustodianCollectionService(dataStore, session);
            custodianFulfilmentService = new CustodianFulfilmentService(dataStore, session);
            custodianInventoryService = new CustodianInventoryService(dataStore, session);
        } catch (IOException exception) {
            showError("Unable to load LoanDesk data", exception.getMessage());
            return;
        }
        showRoleSelection();
    }

    private void showRoleSelection() {
        Label loan = new Label("Loan");
        loan.getStyleClass().add("brand-loan");
        Label desk = new Label("Desk");
        desk.getStyleClass().add("brand-desk");
        HBox brand = new HBox(0, loan, desk);
        brand.getStyleClass().add("landing-brand");
        brand.setAlignment(Pos.CENTER);

        Label subtitle = new Label("Equipment inventory and lending management.");
        subtitle.getStyleClass().add("landing-subtitle");
        Label journey = new Label("FIND   /   LOAN   /   MANAGE");
        journey.getStyleClass().add("landing-journey");

        Button borrower = new Button("Sign in as Borrower  →");
        Button supervisor = new Button("Sign in as Supervisor  →");
        Button custodian = new Button("Sign in as Custodian  →");
        borrower.setOnAction(event -> showBorrowerOptions());
        supervisor.setOnAction(event -> showSupervisorLogin());
        custodian.setOnAction(event -> showCustodianLogin());
        HBox cards = new HBox(24,
                roleCard("borrower", "Borrower", "Request and borrow equipment\nfor your work or projects.", borrower),
                roleCard("supervisor", "Supervisor", "Approve requests and oversee\nequipment usage.", supervisor),
                roleCard("custodian", "Custodian", "Manage inventory and handle\nequipment check-in/out.", custodian));
        cards.getStyleClass().add("role-cards");
        cards.setAlignment(Pos.CENTER);

        VBox content = new VBox(12, brand, subtitle, journey, cards);
        content.getStyleClass().add("landing-content");
        content.setAlignment(Pos.TOP_CENTER);
        StackPane landing = new StackPane(content);
        landing.getStyleClass().add("landing-page");
        showScene(landing);
    }

    private VBox roleCard(String iconType, String title, String description, Button action) {
        StackPane iconHolder = new StackPane(roleIcon(iconType));
        iconHolder.getStyleClass().add("role-icon-holder");
        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("role-card-title");
        Label cardDescription = new Label(description);
        cardDescription.getStyleClass().add("role-card-description");
        cardDescription.setWrapText(true);
        action.getStyleClass().add("role-sign-in-button");
        action.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(16, iconHolder, cardTitle, cardDescription, action);
        card.getStyleClass().add("role-card");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private Group roleIcon(String iconType) {
        return switch (iconType) {
            case "borrower" -> borrowerIcon();
            case "supervisor" -> supervisorIcon();
            case "custodian" -> custodianIcon();
            default -> throw new IllegalArgumentException("Unknown role icon: " + iconType);
        };
    }

    private Group borrowerIcon() {
        Circle head = outlinedCircle(0, -17, 11, Color.web("#151a22"));
        SVGPath shoulders = outlinedPath("M -25 27 V 17 C -25 7 -17 0 -7 0 H 7 C 17 0 25 7 25 17 V 27 Z");
        Circle badge = new Circle(22, 19, 12, Color.web("#d9202b"));
        Label plus = new Label("+");
        plus.getStyleClass().add("role-icon-badge-text");
        plus.setTranslateX(17.5);
        plus.setTranslateY(8);
        return new Group(head, shoulders, badge, plus);
    }

    private Group supervisorIcon() {
        Color ink = Color.web("#151a22");
        Circle leftHead = outlinedCircle(-27, -6, 7, ink);
        Circle rightHead = outlinedCircle(27, -6, 7, ink);
        Circle centreHead = outlinedCircle(0, -16, 11, Color.web("#d9202b"));
        SVGPath leftBody = outlinedPath("M -39 23 V 16 C -39 9 -34 5 -27 5 C -20 5 -15 9 -15 16 V 23 Z");
        SVGPath rightBody = outlinedPath("M 15 23 V 16 C 15 9 20 5 27 5 C 34 5 39 9 39 16 V 23 Z");
        SVGPath centreBody = outlinedPath("M -22 28 V 16 C -22 7 -14 1 -5 1 H 5 C 14 1 22 7 22 16 V 28 Z");
        return new Group(leftHead, rightHead, centreHead, leftBody, rightBody, centreBody);
    }

    private Group custodianIcon() {
        Color ink = Color.web("#151a22");
        Polygon top = new Polygon(0, -27, 29, -12, 0, 3, -29, -12);
        top.setFill(Color.web("#d9202b"));
        top.setStroke(ink);
        top.setStrokeWidth(4);
        Polygon left = new Polygon(-29, -12, 0, 3, 0, 34, -29, 18);
        Polygon right = new Polygon(0, 3, 29, -12, 29, 18, 0, 34);
        styleCubeFace(left, ink);
        styleCubeFace(right, ink);
        Line centre = new Line(0, 3, 0, 34);
        centre.setStroke(ink);
        centre.setStrokeWidth(4);
        return new Group(top, left, right, centre);
    }

    private static Circle outlinedCircle(double x, double y, double radius, Color stroke) {
        Circle circle = new Circle(x, y, radius, Color.TRANSPARENT);
        circle.setStroke(stroke);
        circle.setStrokeWidth(4);
        return circle;
    }

    private static SVGPath outlinedPath(String content) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#151a22"));
        path.setStrokeWidth(4);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        return path;
    }

    private static void styleCubeFace(Shape face, Color stroke) {
        face.setFill(Color.WHITE);
        face.setStroke(stroke);
        face.setStrokeWidth(4);
    }

    private void showBorrowerOptions() {
        showBorrowerForm(false);
    }

    private void showBorrowerForm(boolean signUp) {
        String action = signUp ? "Create account" : "Log in";
        VBox content = authenticationLayout("Borrower", signUp ? "Create your account" : "Welcome back",
                signUp ? "Set up your borrower account to request equipment."
                        : "Sign in to request and manage your equipment loans.");
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Button submit = new Button(action);
        submit.getStyleClass().add("auth-submit-button");
        submit.setMaxWidth(Double.MAX_VALUE);
        Button switchAction = new Button(signUp ? "Log in instead" : "Sign up instead");
        switchAction.getStyleClass().add("auth-link-button");
        Button back = roleSelectionBackButton();
        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.getStyleClass().add("auth-feedback");
        submit.setOnAction(event -> {
            try {
                User borrower = signUp
                        ? authenticationService.signUpBorrower(username.getText(), password.getText())
                        : authenticationService.loginBorrower(username.getText(), password.getText());
                openDashboard(borrower);
            } catch (IllegalArgumentException | IOException exception) {
                feedback.setText(exception.getMessage());
                feedback.getStyleClass().add("error-label");
            } finally {
                password.clear();
            }
        });
        password.setOnAction(event -> submit.fire());
        switchAction.setOnAction(event -> showBorrowerForm(!signUp));
        VBox form = authenticationForm(username, password, submit, switchAction, feedback, back);
        content.getChildren().add(form);
        showScene(content);
    }

    private void showSupervisorLogin() {
        VBox content = authenticationLayout("Supervisor", "Welcome back",
                "Enter your password to access the approval workspace.");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Button submit = new Button("Log in");
        submit.getStyleClass().add("auth-submit-button");
        submit.setMaxWidth(Double.MAX_VALUE);
        Button back = roleSelectionBackButton();
        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.getStyleClass().add("auth-feedback");
        Runnable login = () -> {
            try {
                openDashboard(authenticationService.loginSupervisor(password.getText()));
            } catch (IllegalArgumentException exception) {
                feedback.setText(exception.getMessage());
                feedback.getStyleClass().add("error-label");
            } finally {
                password.clear();
            }
        };
        submit.setOnAction(event -> login.run());
        password.setOnAction(event -> login.run());
        content.getChildren().add(authenticationForm(password, submit, feedback, back));
        showScene(content);
    }

    private void showCustodianLogin() {
        VBox content = authenticationLayout("Custodian", "Welcome back",
                "Enter your password to access the inventory workspace.");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Button submit = new Button("Log in");
        submit.getStyleClass().add("auth-submit-button");
        submit.setMaxWidth(Double.MAX_VALUE);
        Button back = roleSelectionBackButton();
        TextArea feedback = new TextArea();
        feedback.setEditable(false);
        feedback.setWrapText(true);
        feedback.setPrefRowCount(2);
        feedback.setMaxWidth(440);
        feedback.getStyleClass().add("copyable-error-message");
        feedback.setVisible(false);
        feedback.setManaged(false);
        Runnable login = () -> {
            try {
                openDashboard(authenticationService.loginCustodian(password.getText()));
            } catch (IllegalArgumentException exception) {
                feedback.setText(exception.getMessage());
                feedback.setVisible(true);
                feedback.setManaged(true);
            } finally {
                password.clear();
            }
        };
        submit.setOnAction(event -> login.run());
        password.setOnAction(event -> login.run());
        content.getChildren().add(authenticationForm(password, submit, feedback, back));
        showScene(content);
    }

    private VBox authenticationLayout(String role, String heading, String subtitle) {
        VBox content = layout(heading, subtitle);
        content.getStyleClass().add("authentication-page");
        Label selectedRole = new Label("Selected role: " + role);
        selectedRole.getStyleClass().add("selected-role-label");
        content.getChildren().add(selectedRole);
        return content;
    }

    private VBox authenticationForm(Node... children) {
        VBox form = new VBox(12, children);
        form.getStyleClass().add("authentication-form");
        form.setAlignment(Pos.CENTER);
        return form;
    }

    private Button roleSelectionBackButton() {
        Button back = new Button("← Return to role selection");
        back.getStyleClass().add("auth-back-button");
        back.setOnAction(event -> showRoleSelection());
        return back;
    }

    private void openDashboard(User user) {
        session.start(user);
        if (user.role() == Role.CUSTODIAN) {
            showCustodianDashboard(user);
            return;
        }
        String title = switch (user.role()) {
            case BORROWER -> "Borrower Dashboard";
            case SUPERVISOR -> "Supervisor Dashboard";
            case CUSTODIAN -> "Custodian Dashboard";
        };
        VBox content = layout(title, "Manage your LoanDesk activity in one place.");
        Label welcome = new Label("Signed in as " + user.username());
        welcome.getStyleClass().add("welcome-label");
        if (user.role() == Role.BORROWER) {
            content.getStyleClass().add("borrower-page");
            Label section = new Label("Borrowing workspace");
            section.getStyleClass().add("section-heading");
            VBox actions = new VBox(12,
                    dashboardCard("Catalogue", "Browse equipment and start a request.",
                            catalogueButton()),
                    dashboardCard("My Requests", "Review request status and eligible actions.",
                            requestsButton()),
                    dashboardCard("My Loans", "See active loans and returned history.",
                            loansButton()));
            actions.setMaxWidth(520);
            actions.setAlignment(Pos.CENTER);
            HBox actionContainer = centeredContainer(actions);
            actionContainer.getStyleClass().add("dashboard-actions");
            content.getChildren().addAll(welcome, section, actionContainer);
        } else if (user.role() == Role.SUPERVISOR) {
            Label section = new Label("Review workspace");
            section.getStyleClass().add("section-heading");
            Button queue = new Button("Review Queue");
            queue.setOnAction(event -> showReviewQueue());
            Button history = new Button("Decision History");
            history.setOnAction(event -> showDecisionHistory());
            VBox actions = new VBox(12,
                    dashboardCard("Review Queue",
                            "Filter requests and decide on them.", queue),
                    dashboardCard("Decision History",
                            "See who decided what, when and why.", history));
            actions.setMaxWidth(520);
            actions.setAlignment(Pos.CENTER);
            HBox actionContainer = centeredContainer(actions);
            actionContainer.getStyleClass().add("dashboard-actions");
            content.getChildren().addAll(welcome, section, actionContainer);
        }
        Button logout = new Button("Log out");
        logout.getStyleClass().add("secondary-button");
        logout.setOnAction(event -> {
            session.clear();
            showRoleSelection();
        });
        content.getChildren().add(logout);
        if (user.role() == Role.BORROWER) {
            ScrollPane dashboard = new ScrollPane(content);
            dashboard.setFitToWidth(true);
            dashboard.setFitToHeight(true);
            dashboard.getStyleClass().add("dashboard-scroll");
            showScene(dashboard);
        } else {
            showScene(content);
        }
    }

    private VBox dashboardCard(String title, String description, Button action) {
        action.getStyleClass().add("primary-button");
        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("card-title");
        Label cardDescription = new Label(description);
        cardDescription.getStyleClass().add("card-description");
        VBox card = new VBox(7, cardTitle, cardDescription, action);
        card.getStyleClass().add("dashboard-card");
        return card;
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

    private Button loansButton() {
        Button loans = new Button("My Loans");
        loans.setOnAction(event -> showMyLoans());
        return loans;
    }

    private void showCustodianDashboard(User user) {
        VBox page = new VBox(24);
        page.getStyleClass().add("custodian-dashboard");

        Label title = new Label("Custodian Dashboard");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Manage collections, active loans, and inventory condition.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        VBox heading = new VBox(4, title, subtitle);

        Button inventory = new Button("Manage inventory");
        inventory.getStyleClass().add("custodian-secondary-button");
        inventory.setOnAction(event -> showCustodianInventory());
        Button logout = new Button("Log out");
        logout.getStyleClass().add("custodian-logout-button");
        logout.setOnAction(event -> {
            session.clear();
            showRoleSelection();
        });
        HBox header = new HBox(12, heading, spacer(), inventory, logout);
        header.setAlignment(Pos.CENTER_LEFT);

        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");
        feedback.setManaged(false);
        feedback.setVisible(false);
        if (custodianDashboardMessage != null) {
            showCustodianFeedback(feedback, custodianDashboardMessage, custodianDashboardMessageIsError);
            custodianDashboardMessage = null;
        }

        ObservableList<LoanRequest> requestItems = FXCollections.observableArrayList();
        FilteredList<LoanRequest> filteredRequests = new FilteredList<>(requestItems, item -> true);
        TableView<LoanRequest> requests = new TableView<>(filteredRequests);
        requests.getStyleClass().add("custodian-table");
        configureCompactTable(requests);

        ObservableList<Loan> loanItems = FXCollections.observableArrayList();
        FilteredList<Loan> filteredLoans = new FilteredList<>(loanItems, item -> true);
        TableView<Loan> loans = new TableView<>(filteredLoans);
        loans.getStyleClass().add("custodian-table");
        configureCompactTable(loans);

        TextField requestSearch = new TextField();
        requestSearch.setPromptText("Filter by item, borrower, or request ID");
        requestSearch.getStyleClass().add("custodian-filter-field");
        ComboBox<String> requestStatus = new ComboBox<>();
        requestStatus.getItems().addAll("All statuses", "APPROVED");
        requestStatus.setValue("All statuses");
        requestStatus.getStyleClass().add("custodian-filter-select");

        TextField loanSearch = new TextField();
        loanSearch.setPromptText("Filter by item, borrower, or loan ID");
        loanSearch.getStyleClass().add("custodian-filter-field");
        ComboBox<String> loanStatus = new ComboBox<>();
        loanStatus.getItems().addAll("All statuses", "ACTIVE", "OVERDUE", "LOST");
        loanStatus.setValue("All statuses");
        loanStatus.getStyleClass().add("custodian-filter-select");

        Map<String, String> equipmentNames;
        try {
            equipmentNames = equipmentNames();
        } catch (IOException | IllegalStateException exception) {
            equipmentNames = new HashMap<>();
            showCustodianFeedback(feedback, "Unable to load equipment names: " + exception.getMessage(), true);
        }
        Map<String, String> names = equipmentNames;
        requests.getColumns().addAll(
                equipmentColumn("Item", LoanRequest::equipmentId, names),
                textColumn("Borrower", LoanRequest::borrowerUsername),
                textColumn("Collection window", request -> request.startDate() + " – "
                        + request.startDate().plusDays(3)),
                textColumn("Due date", request -> request.dueDate().toString()),
                textColumn("Status", request -> request.status().name()),
                requestActionColumn(feedback));
        loans.getColumns().addAll(
                equipmentColumn("Item", Loan::equipmentId, names),
                textColumn("Borrower", Loan::borrowerUsername),
                textColumn("Checked out", loan -> loan.checkoutDate().toString()),
                textColumn("Due date", loan -> loan.dueDate().toString()),
                textColumn("Status", this::loanDisplayStatus),
                loanActionColumn(feedback));

        requestSearch.textProperty().addListener((observable, oldValue, value) ->
                applyRequestFilter(filteredRequests, value, requestStatus.getValue(), names));
        requestStatus.valueProperty().addListener((observable, oldValue, value) ->
                applyRequestFilter(filteredRequests, requestSearch.getText(), value, names));
        loanSearch.textProperty().addListener((observable, oldValue, value) ->
                applyLoanFilter(filteredLoans, value, loanStatus.getValue(), names));
        loanStatus.valueProperty().addListener((observable, oldValue, value) ->
                applyLoanFilter(filteredLoans, loanSearch.getText(), value, names));

        Label requestTitle = new Label("Loan Requests");
        requestTitle.getStyleClass().add("custodian-section-heading");
        Label requestDescription = new Label("Approved requests that are ready for collection.");
        requestDescription.getStyleClass().add("custodian-section-description");
        VBox requestHeading = new VBox(2, requestTitle, requestDescription);
        HBox requestFilters = new HBox(10, requestSearch, requestStatus);
        requestFilters.setAlignment(Pos.CENTER_RIGHT);
        HBox requestHeader = new HBox(16, requestHeading, spacer(), requestFilters);
        requestHeader.setAlignment(Pos.CENTER_LEFT);
        VBox requestPanel = new VBox(16, requestHeader, requests);
        requestPanel.getStyleClass().add("custodian-panel");

        Label loanTitle = new Label("Active Loans");
        loanTitle.getStyleClass().add("custodian-section-heading");
        Label loanDescription = new Label("Items currently on loan or reported lost.");
        loanDescription.getStyleClass().add("custodian-section-description");
        VBox loanHeading = new VBox(2, loanTitle, loanDescription);
        HBox loanFilters = new HBox(10, loanSearch, loanStatus);
        loanFilters.setAlignment(Pos.CENTER_RIGHT);
        HBox loanHeader = new HBox(16, loanHeading, spacer(), loanFilters);
        loanHeader.setAlignment(Pos.CENTER_LEFT);
        VBox loanPanel = new VBox(16, loanHeader, loans);
        loanPanel.getStyleClass().add("custodian-panel");

        HBox stats = new HBox(16);
        stats.getStyleClass().add("custodian-stat-row");

        Runnable reload = () -> {
            try {
                Map<String, String> refreshedNames = equipmentNames();
                names.clear();
                names.putAll(refreshedNames);
                requestItems.setAll(custodianCollectionService.collectionsQueue());
                loanItems.setAll(custodianFulfilmentService.activeLoans());
                int attention = (int) custodianInventoryService.inventory().stream()
                        .filter(item -> item.equipment().condition() != EquipmentCondition.GOOD).count();
                stats.getChildren().setAll(
                        statCard("Pending collections", requestItems.size(), "Ready for checkout"),
                        statCard("Active loans", loanItems.stream()
                                .filter(loan -> loan.status() == LoanStatus.ACTIVE).count(), "Currently on loan"),
                        statCard("Total inventory", refreshedNames.size(), "Equipment in catalogue"),
                        statCard("Condition alerts", attention, "Damaged, lost, or under maintenance"));
                applyRequestFilter(filteredRequests, requestSearch.getText(), requestStatus.getValue(), names);
                applyLoanFilter(filteredLoans, loanSearch.getText(), loanStatus.getValue(), names);
                if (!feedback.getText().startsWith("Checked out")
                        && !feedback.getText().startsWith("Return")
                        && !feedback.getText().startsWith("Item marked")
                        && !feedback.getText().startsWith("Lost item")) {
                    feedback.setVisible(false);
                    feedback.setManaged(false);
                }
            } catch (IOException | IllegalStateException exception) {
                requestItems.clear();
                loanItems.clear();
                stats.getChildren().setAll(statCard("Dashboard unavailable", "—", "Refresh after resolving the data issue"));
                showCustodianFeedback(feedback, "Unable to load dashboard data: " + exception.getMessage(), true);
            }
        };
        reload.run();

        page.getChildren().addAll(header, feedback, stats, requestPanel, loanPanel);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("custodian-dashboard-scroll");
        showScene(scroll);
    }

    private Node spacer() {
        StackPane spacer = new StackPane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        return spacer;
    }

    private Map<String, String> equipmentNames() throws IOException {
        return custodianInventoryService.inventory().stream().collect(Collectors.toMap(
                item -> item.equipment().id(), item -> item.equipment().name()));
    }

    private void configureCompactTable(TableView<?> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(46);
        table.setPrefHeight(276);
        table.setMaxHeight(276);
        table.setPlaceholder(new Label("Nothing to show right now."));
    }

    private <T> TableColumn<T, String> textColumn(String title, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private <T> TableColumn<T, T> equipmentColumn(
            String title, Function<T, String> equipmentId, Map<String, String> names) {
        TableColumn<T, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(table -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                String id = equipmentId.apply(item);
                Label name = new Label(names.getOrDefault(id, id));
                name.getStyleClass().add("custodian-item-name");
                Label identifier = new Label(id);
                identifier.getStyleClass().add("custodian-item-id");
                setGraphic(new VBox(1, name, identifier));
            }
        });
        return column;
    }

    private TableColumn<LoanRequest, LoanRequest> requestActionColumn(Label feedback) {
        TableColumn<LoanRequest, LoanRequest> column = new TableColumn<>("Action");
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(table -> new TableCell<>() {
            @Override
            protected void updateItem(LoanRequest request, boolean empty) {
                super.updateItem(request, empty);
                if (empty || request == null) {
                    setGraphic(null);
                    return;
                }
                Button checkout = new Button("Check out");
                checkout.getStyleClass().add("custodian-primary-button");
                checkout.setOnAction(event -> {
                    try {
                        Loan loan = custodianCollectionService.checkout(request.requestId());
                        rememberCustodianDashboardMessage("Checked out " + loan.equipmentId() + " to "
                                + loan.borrowerUsername() + ".", false);
                        showCustodianDashboard(session.requireUser());
                    } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                        showCustodianFeedback(feedback, exception.getMessage(), true);
                    }
                });
                setGraphic(checkout);
            }
        });
        return column;
    }

    private TableColumn<Loan, Loan> loanActionColumn(Label feedback) {
        TableColumn<Loan, Loan> column = new TableColumn<>("Action");
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(table -> new TableCell<>() {
            @Override
            protected void updateItem(Loan loan, boolean empty) {
                super.updateItem(loan, empty);
                if (empty || loan == null) {
                    setGraphic(null);
                    return;
                }
                Button manage = new Button("Return / update");
                manage.getStyleClass().add("custodian-secondary-button");
                manage.setOnAction(event -> showCustodianActiveLoans());
                Button action = new Button(loan.status() == LoanStatus.LOST ? "Recover" : "Mark lost");
                action.getStyleClass().add(loan.status() == LoanStatus.LOST
                        ? "custodian-secondary-button" : "custodian-danger-button");
                action.setOnAction(event -> {
                    try {
                        if (loan.status() == LoanStatus.LOST) {
                            custodianFulfilmentService.recoverLost(loan.loanId());
                            rememberCustodianDashboardMessage(
                                    "Lost item recovered. Use the Active Loans page to record its return.", false);
                        } else {
                            custodianFulfilmentService.markLost(loan.loanId());
                            rememberCustodianDashboardMessage("Item marked lost.", false);
                        }
                        showCustodianDashboard(session.requireUser());
                    } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                        showCustodianFeedback(feedback, exception.getMessage(), true);
                    }
                });
                HBox actions = new HBox(6, manage, action);
                actions.setAlignment(Pos.CENTER_LEFT);
                setGraphic(actions);
            }
        });
        return column;
    }

    private void applyRequestFilter(
            FilteredList<LoanRequest> items, String search, String status, Map<String, String> names) {
        String query = search == null ? "" : search.trim().toLowerCase();
        items.setPredicate(request -> ("All statuses".equals(status) || request.status().name().equals(status))
                && (query.isBlank() || request.borrowerUsername().toLowerCase().contains(query)
                || request.requestId().toLowerCase().contains(query)
                || request.equipmentId().toLowerCase().contains(query)
                || names.getOrDefault(request.equipmentId(), "").toLowerCase().contains(query)));
    }

    private void applyLoanFilter(FilteredList<Loan> items, String search, String status, Map<String, String> names) {
        String query = search == null ? "" : search.trim().toLowerCase();
        items.setPredicate(loan -> ("All statuses".equals(status) || loanDisplayStatus(loan).equals(status))
                && (query.isBlank() || loan.borrowerUsername().toLowerCase().contains(query)
                || loan.loanId().toLowerCase().contains(query)
                || loan.equipmentId().toLowerCase().contains(query)
                || names.getOrDefault(loan.equipmentId(), "").toLowerCase().contains(query)));
    }

    private Node statCard(String title, Object value, String description) {
        Label statTitle = new Label(title);
        statTitle.getStyleClass().add("custodian-stat-title");
        Label statValue = new Label(String.valueOf(value));
        statValue.getStyleClass().add("custodian-stat-value");
        Label statDescription = new Label(description);
        statDescription.getStyleClass().add("custodian-stat-description");
        VBox card = new VBox(5, statTitle, statValue, statDescription);
        card.getStyleClass().add("custodian-stat-card");
        HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        return card;
    }

    private void showCustodianFeedback(Label feedback, String message, boolean error) {
        feedback.setText(message);
        feedback.getStyleClass().removeAll("custodian-feedback-error", "custodian-feedback-success");
        feedback.getStyleClass().add(error ? "custodian-feedback-error" : "custodian-feedback-success");
        feedback.setManaged(true);
        feedback.setVisible(true);
    }

    private void rememberCustodianDashboardMessage(String message, boolean error) {
        custodianDashboardMessage = message;
        custodianDashboardMessageIsError = error;
    }

    private void showCollectionsQueue() {
        VBox content = layout("Collections Queue",
                "Approved requests can be issued from their start date through the third day after it.");
        Label feedback = new Label();
        ListView<LoanRequest> requests = new ListView<>();
        requests.setPrefHeight(280);
        requests.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LoanRequest item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.borrowerUsername() + " — " + item.equipmentId() + "\n"
                                + "Collect: " + item.startDate() + " to "
                                + item.startDate().plusDays(3) + ", due: " + item.dueDate());
            }
        });
        Button checkout = new Button("Check out selected item");
        checkout.getStyleClass().add("primary-button");
        checkout.disableProperty().bind(requests.getSelectionModel().selectedItemProperty().isNull());
        Button refresh = new Button("Refresh queue");
        refresh.getStyleClass().add("secondary-button");
        Button back = new Button("Back to dashboard");
        back.getStyleClass().add("secondary-button");

        Runnable reload = () -> {
            try {
                requests.getItems().setAll(custodianCollectionService.collectionsQueue());
                feedback.getStyleClass().remove("error-label");
                feedback.setText(requests.getItems().isEmpty()
                        ? "No approved requests are collectable today."
                        : requests.getItems().size() + " request(s) ready for collection.");
            } catch (IllegalStateException | IOException exception) {
                requests.getItems().clear();
                feedback.setText("Unable to load the collections queue: " + exception.getMessage());
                feedback.getStyleClass().add("error-label");
            }
        };
        refresh.setOnAction(event -> reload.run());
        checkout.setOnAction(event -> {
            LoanRequest selected = requests.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            try {
                Loan loan = custodianCollectionService.checkout(selected.requestId());
                feedback.getStyleClass().remove("error-label");
                feedback.setText("Checked out " + loan.equipmentId() + " to "
                        + loan.borrowerUsername() + ".");
                reload.run();
            } catch (StaleDataException exception) {
                feedback.setText("Collection data changed. Refresh the queue and retry.");
                feedback.getStyleClass().add("error-label");
            } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                feedback.setText(exception.getMessage());
                feedback.getStyleClass().add("error-label");
            }
        });
        back.setOnAction(event -> openDashboard(session.requireUser()));
        reload.run();
        content.getChildren().addAll(feedback, requests, checkout, refresh, back);
        showScrollableScene(content);
    }

    private void showCustodianInventory() {
        VBox content = new VBox(24);
        content.getStyleClass().add("custodian-inventory-page");
        Label title = new Label("Manage Inventory");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Review equipment status and update its physical condition.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Button addEquipment = new Button("Add equipment");
        addEquipment.setGraphic(plusIcon());
        addEquipment.getStyleClass().add("custodian-primary-button");
        addEquipment.getStyleClass().add("custodian-create-button");
        Button back = new Button("← Back to dashboard");
        back.getStyleClass().add("custodian-secondary-button");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        HBox header = new HBox(12, new VBox(4, title, subtitle), spacer(), addEquipment, back);
        header.setAlignment(Pos.CENTER_LEFT);
        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");
        TableView<CustodianInventoryService.InventoryItem> items = new TableView<>();
        items.getStyleClass().add("custodian-table");
        items.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        items.setPrefHeight(330);
        Map<String, String> names = new HashMap<>();
        VBox details = new VBox(12);
        details.getStyleClass().add("custodian-detail-panel");
        details.setVisible(false);
        details.setManaged(false);
        VBox addPanel = new VBox(16);
        addPanel.getStyleClass().add("custodian-add-panel");
        addPanel.setVisible(false);
        addPanel.setManaged(false);
        Label addTitle = new Label("Add equipment");
        addTitle.getStyleClass().add("custodian-section-heading");
        TextField name = new TextField();
        name.setPromptText("e.g. Canon EOS R6 Camera");
        name.getStyleClass().add("custodian-filter-field");
        Label nameLabel = new Label("Name *");
        nameLabel.getStyleClass().add("custodian-form-label");
        ComboBox<EquipmentCondition> initialCondition = new ComboBox<>();
        initialCondition.getItems().setAll(EquipmentCondition.values());
        initialCondition.setValue(EquipmentCondition.GOOD);
        initialCondition.getStyleClass().add("custodian-filter-select");
        Label conditionLabel = new Label("Condition *");
        conditionLabel.getStyleClass().add("custodian-form-label");
        VBox nameField = new VBox(6, nameLabel, name);
        VBox conditionField = new VBox(6, conditionLabel, initialCondition);
        Button create = new Button("Create equipment");
        create.getStyleClass().add("custodian-primary-button");
        create.getStyleClass().add("custodian-create-button");
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("custodian-secondary-button");
        HBox formActions = new HBox(10, cancel, create);
        formActions.setAlignment(Pos.CENTER_RIGHT);
        addPanel.getChildren().addAll(addTitle, nameField, conditionField, formActions);

        Runnable reload = () -> {
            try {
                items.getItems().setAll(custodianInventoryService.inventory());
                names.clear();
                items.getItems().forEach(item -> names.put(item.equipment().id(), item.equipment().name()));
                if (items.getItems().isEmpty()) {
                    showCustodianFeedback(feedback, "No equipment has been added. Add an item to begin.", false);
                } else if (feedback.getText().isBlank()) {
                    feedback.setVisible(false);
                    feedback.setManaged(false);
                }
            } catch (IllegalStateException | IOException exception) {
                items.getItems().clear();
                showCustodianFeedback(feedback, "Unable to load inventory: " + exception.getMessage(), true);
            }
        };
        items.getColumns().addAll(
                equipmentColumn("Item", item -> item.equipment().id(), names),
                inventoryConditionColumn(feedback, reload),
                textColumn("Availability", item -> item.availability().name()),
                inventoryDetailsColumn(details, feedback));
        addEquipment.setOnAction(event -> {
            details.setVisible(false);
            details.setManaged(false);
            addPanel.setVisible(true);
            addPanel.setManaged(true);
            name.requestFocus();
        });
        cancel.setOnAction(event -> {
            addPanel.setVisible(false);
            addPanel.setManaged(false);
        });
        create.setOnAction(event -> runInventoryAction(feedback, reload, () -> {
            Equipment added = custodianInventoryService.addEquipment(name.getText(), initialCondition.getValue());
            name.clear();
            initialCondition.setValue(EquipmentCondition.GOOD);
            addPanel.setVisible(false);
            addPanel.setManaged(false);
        }, "Equipment added."));
        reload.run();
        content.getChildren().addAll(header, feedback, addPanel, items, details);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("custodian-dashboard-scroll");
        showScene(scroll);
    }

    private Node plusIcon() {
        Line horizontal = new Line(3, 8, 13, 8);
        Line vertical = new Line(8, 3, 8, 13);
        horizontal.setStroke(Color.WHITE);
        vertical.setStroke(Color.WHITE);
        horizontal.setStrokeWidth(1.8);
        vertical.setStrokeWidth(1.8);
        return new Group(horizontal, vertical);
    }

    private TableColumn<CustodianInventoryService.InventoryItem, CustodianInventoryService.InventoryItem>
            inventoryConditionColumn(Label feedback, Runnable reload) {
        TableColumn<CustodianInventoryService.InventoryItem, CustodianInventoryService.InventoryItem> column
                = new TableColumn<>("Condition");
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(table -> new TableCell<>() {
            @Override
            protected void updateItem(CustodianInventoryService.InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                ComboBox<EquipmentCondition> condition = new ComboBox<>();
                condition.getItems().setAll(EquipmentCondition.values());
                condition.setValue(item.equipment().condition());
                condition.getStyleClass().add("custodian-table-select");
                boolean reserved = item.availability() == loandesk.domain.AvailabilityStatus.RESERVED;
                condition.setDisable(reserved);
                condition.setTooltip(new Tooltip(reserved
                        ? "Condition cannot be changed while this item is reserved."
                        : "Changes are saved immediately."));
                condition.setOnAction(event -> {
                    EquipmentCondition selected = condition.getValue();
                    if (selected == item.equipment().condition()) {
                        return;
                    }
                    condition.setDisable(true);
                    runInventoryAction(feedback, reload, () -> custodianInventoryService.updateEquipment(
                            item.equipment().id(), item.equipment().name(), selected),
                            "Condition updated for " + item.equipment().name() + ".");
                    condition.setDisable(false);
                });
                setGraphic(condition);
            }
        });
        return column;
    }

    private TableColumn<CustodianInventoryService.InventoryItem, CustodianInventoryService.InventoryItem>
            inventoryDetailsColumn(VBox details, Label feedback) {
        TableColumn<CustodianInventoryService.InventoryItem, CustodianInventoryService.InventoryItem> column
                = new TableColumn<>("");
        column.setPrefWidth(54);
        column.setMaxWidth(54);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(table -> new TableCell<>() {
            @Override
            protected void updateItem(CustodianInventoryService.InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Button view = new Button();
                view.setGraphic(eyeIcon());
                view.setAccessibleText("View details for " + item.equipment().name());
                view.setTooltip(new Tooltip("View item details"));
                view.getStyleClass().add("custodian-eye-button");
                view.setOnAction(event -> showInventoryDetails(details, item, feedback));
                setGraphic(view);
            }
        });
        return column;
    }

    private Node eyeIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent("M2,10 C4.4,5.8 7.1,4 10,4 C12.9,4 15.6,5.8 18,10 C15.6,14.2 12.9,16 10,16 C7.1,16 4.4,14.2 2,10 Z M10,7 C8.35,7 7,8.35 7,10 C7,11.65 8.35,13 10,13 C11.65,13 13,11.65 13,10 C13,8.35 11.65,7 10,7 Z");
        icon.setFill(Color.web("#5b6370"));
        return icon;
    }

    private void showInventoryDetails(
            VBox details, CustodianInventoryService.InventoryItem item, Label feedback) {
        Equipment equipment = item.equipment();
        Loan currentLoan = null;
        LoanRequest reservation = null;
        try {
            var data = dataStore.loadOrSeed();
            currentLoan = data.loans().stream()
                    .filter(loan -> loan.equipmentId().equals(equipment.id()))
                    .filter(loan -> loan.status() == LoanStatus.ACTIVE || loan.status() == LoanStatus.LOST)
                    .findFirst().orElse(null);
            if (currentLoan == null && item.availability()
                    == loandesk.domain.AvailabilityStatus.RESERVED) {
                reservation = data.requests().stream()
                        .filter(request -> request.equipmentId().equals(equipment.id()))
                        .filter(request -> request.status() == RequestStatus.APPROVED)
                        .findFirst().orElse(null);
            }
        } catch (IOException exception) {
            showCustodianFeedback(feedback, "Unable to load borrowing details: " + exception.getMessage(), true);
            return;
        }
        Label heading = new Label(equipment.name());
        heading.getStyleClass().add("custodian-section-heading");
        Label itemId = new Label(equipment.id());
        itemId.getStyleClass().add("custodian-item-id");
        String borrower = currentLoan != null ? currentLoan.borrowerUsername()
                : reservation != null ? reservation.borrowerUsername() : "—";
        String collectionDate = currentLoan != null ? currentLoan.checkoutDate().toString()
                : reservation != null ? reservation.startDate().toString() : "—";
        String dueDate = currentLoan != null ? currentLoan.dueDate().toString()
                : reservation != null ? reservation.dueDate().toString() : "—";
        String borrowStatus = currentLoan != null ? currentLoan.status().name()
                : reservation != null ? "RESERVED" : "Not currently borrowed";
        VBox fields = new VBox(8,
                detailRow("Condition", equipment.condition().name()),
                detailRow("Availability", item.availability().name()),
                detailRow("Borrow status", borrowStatus),
                detailRow("Borrower", borrower),
                detailRow("Collection date", collectionDate),
                detailRow("Due date", dueDate));
        Button close = new Button("Close details");
        close.getStyleClass().add("custodian-secondary-button");
        close.setOnAction(event -> {
            details.setVisible(false);
            details.setManaged(false);
        });
        details.getChildren().setAll(new VBox(2, heading, itemId), fields, close);
        details.setVisible(true);
        details.setManaged(true);
    }

    private HBox detailRow(String label, String value) {
        Label detailLabel = new Label(label);
        detailLabel.getStyleClass().add("custodian-detail-label");
        Label detailValue = new Label(value);
        detailValue.getStyleClass().add("custodian-detail-value");
        HBox row = new HBox(16, detailLabel, spacer(), detailValue);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void runInventoryAction(Label feedback, Runnable reload, InventoryAction action, String success) {
        try {
            action.run();
            feedback.getStyleClass().remove("error-label");
            feedback.setText(success);
            reload.run();
        } catch (StaleDataException exception) {
            feedback.setText("Inventory data changed. Refresh the list and retry.");
            feedback.getStyleClass().add("error-label");
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            feedback.setText(exception.getMessage());
            feedback.getStyleClass().add("error-label");
        }
    }

    @FunctionalInterface
    private interface InventoryAction {
        void run() throws IOException;
    }

    private void showCustodianActiveLoans() {
        VBox content = layout("Active Loans",
                "Record returns, loss, or recovery. Overdue loans are marked in the list.");
        Label feedback = new Label();
        ListView<Loan> loans = new ListView<>();
        loans.setPrefHeight(260);
        loans.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Loan loan, boolean empty) {
                super.updateItem(loan, empty);
                if (empty || loan == null) {
                    setText(null);
                    return;
                }
                String status = loan.status() == LoanStatus.LOST ? "LOST"
                        : loan.isOverdue(LocalDate.now()) ? "OVERDUE" : "ACTIVE";
                setText(status + " — " + loan.equipmentId() + " for " + loan.borrowerUsername()
                        + "\nChecked out: " + loan.checkoutDate() + ", due: " + loan.dueDate());
            }
        });
        ComboBox<EquipmentCondition> condition = new ComboBox<>();
        condition.getItems().addAll(EquipmentCondition.GOOD, EquipmentCondition.DAMAGED,
                EquipmentCondition.UNDER_MAINTENANCE);
        condition.setPromptText("Return condition");
        Button returnLoan = new Button("Return selected loan");
        Button markLost = new Button("Mark selected item lost");
        Button recover = new Button("Recover selected LOST item");
        Button refresh = new Button("Refresh loans");
        Button back = new Button("Back to dashboard");
        returnLoan.getStyleClass().add("primary-button");
        markLost.getStyleClass().add("secondary-button");
        recover.getStyleClass().add("secondary-button");
        refresh.getStyleClass().add("secondary-button");
        back.getStyleClass().add("secondary-button");
        returnLoan.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Loan selected = loans.getSelectionModel().getSelectedItem();
            return selected == null || selected.status() != LoanStatus.ACTIVE || condition.getValue() == null;
        }, loans.getSelectionModel().selectedItemProperty(), condition.valueProperty()));
        markLost.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Loan selected = loans.getSelectionModel().getSelectedItem();
            return selected == null || selected.status() != LoanStatus.ACTIVE;
        }, loans.getSelectionModel().selectedItemProperty()));
        recover.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Loan selected = loans.getSelectionModel().getSelectedItem();
            return selected == null || selected.status() != LoanStatus.LOST;
        }, loans.getSelectionModel().selectedItemProperty()));

        Runnable reload = () -> {
            try {
                loans.getItems().setAll(custodianFulfilmentService.activeLoans());
                feedback.getStyleClass().remove("error-label");
                feedback.setText(loans.getItems().isEmpty()
                        ? "No active or lost loans need action."
                        : loans.getItems().size() + " loan(s) need action.");
            } catch (IllegalStateException | IOException exception) {
                loans.getItems().clear();
                feedback.setText("Unable to load active loans: " + exception.getMessage());
                feedback.getStyleClass().add("error-label");
            }
        };
        returnLoan.setOnAction(event -> runFulfilmentAction(feedback, reload, () ->
                custodianFulfilmentService.returnLoan(
                        loans.getSelectionModel().getSelectedItem().loanId(), condition.getValue()),
                "Return recorded."));
        markLost.setOnAction(event -> runFulfilmentAction(feedback, reload, () ->
                custodianFulfilmentService.markLost(
                        loans.getSelectionModel().getSelectedItem().loanId()), "Item marked lost."));
        recover.setOnAction(event -> runFulfilmentAction(feedback, reload, () ->
                custodianFulfilmentService.recoverLost(
                        loans.getSelectionModel().getSelectedItem().loanId()),
                "Lost item recovered. Select a return condition to complete the return."));
        refresh.setOnAction(event -> reload.run());
        back.setOnAction(event -> openDashboard(session.requireUser()));
        reload.run();
        content.getChildren().addAll(feedback, loans, condition, returnLoan, markLost, recover, refresh, back);
        showScrollableScene(content);
    }

    private void runFulfilmentAction(Label feedback, Runnable reload, LoanAction action, String success) {
        try {
            action.run();
            feedback.getStyleClass().remove("error-label");
            feedback.setText(success);
            reload.run();
        } catch (StaleDataException exception) {
            feedback.setText("Loan data changed. Refresh the list and retry.");
            feedback.getStyleClass().add("error-label");
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            feedback.setText(exception.getMessage());
            feedback.getStyleClass().add("error-label");
        }
    }

    @FunctionalInterface
    private interface LoanAction {
        void run() throws IOException;
    }

    private void showMyLoans() {
        VBox content = borrowerLayout("My Loans", "Your current loans and borrowing history.");
        Label feedback = new Label();
        Label activeHeading = new Label("Active loans");
        activeHeading.getStyleClass().add("loan-section-heading");
        Label historyHeading = new Label("History");
        historyHeading.getStyleClass().add("loan-section-heading");
        ListView<Loan> activeLoans = loanListView();
        ListView<Loan> history = loanListView();
        Map<String, String> equipmentNames;
        boolean loadFailed = false;
        try {
            equipmentNames = catalogueService.loadCatalogue().stream()
                    .collect(Collectors.toMap(Equipment::id, Equipment::name));
            java.util.List<Loan> loans = borrowerLoanService.listOwnLoans();
            activeLoans.getItems().setAll(loans.stream()
                    .filter(loan -> loan.status() != LoanStatus.RETURNED)
                    .toList());
            history.getItems().setAll(loans.stream()
                    .filter(loan -> loan.status() == LoanStatus.RETURNED)
                    .toList());
            feedback.setText(loans.isEmpty()
                    ? "You have no loans yet."
                    : loans.size() + " loan(s) found.");
        } catch (IOException | IllegalStateException exception) {
            equipmentNames = Map.of();
            loadFailed = true;
            feedback.setText("Unable to load your loans: " + exception.getMessage());
            feedback.getStyleClass().add("error-label");
        }

        Map<String, String> names = equipmentNames;
        activeLoans.setCellFactory(list -> loanCell(names));
        history.setCellFactory(list -> loanCell(names));
        Label activeEmpty = new Label(loadFailed
                ? "Unable to load active loans."
                : "No active loans.");
        Label historyEmpty = new Label(loadFailed
                ? "Unable to load loan history."
                : "No returned loans yet.");
        activeLoans.setPlaceholder(activeEmpty);
        history.setPlaceholder(historyEmpty);
        Button back = new Button("Back to dashboard");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        VBox lists = new VBox(18, activeHeading, activeLoans, historyHeading, history, back);
        lists.setMaxWidth(620);
        lists.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centeredContainer(lists));
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.getStyleClass().add("loan-scroll");
        content.getChildren().addAll(feedback, scroll);
        showScene(content);
    }

    private ListView<Loan> loanListView() {
        ListView<Loan> loans = new ListView<>();
        loans.setPrefHeight(130);
        loans.setMaxWidth(620);
        return loans;
    }

    private ListCell<Loan> loanCell(Map<String, String> equipmentNames) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Loan loan, boolean empty) {
                super.updateItem(loan, empty);
                if (empty || loan == null) {
                    setText(null);
                    return;
                }
                String equipmentName = equipmentNames.getOrDefault(
                        loan.equipmentId(), "Unknown equipment");
                String returnText = loan.returnedDate() == null
                        ? ""
                        : ", returned " + loan.returnedDate();
                setText(equipmentName + " (" + loan.equipmentId() + ") — "
                        + loanDisplayStatus(loan) + "\n"
                        + "Checked out: " + loan.checkoutDate()
                        + ", due: " + loan.dueDate() + returnText);
            }
        };
    }

    private String loanDisplayStatus(Loan loan) {
        if (loan.status() == LoanStatus.LOST) {
            return "LOST";
        }
        if (loan.isOverdue(LocalDate.now())) {
            return "OVERDUE";
        }
        return loan.status().name();
    }

    private void showMyRequests() {
        VBox content = borrowerLayout("My Requests", "Your active requests and request history.");
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
                        + "Eligible pending requests can be edited before their start date.");
        details.setWrapText(true);
        ScrollPane detailsPane = new ScrollPane(details);
        detailsPane.setFitToWidth(true);
        detailsPane.setPrefViewportHeight(140);
        Label editInfo = new Label();
        Button edit = new Button("Edit request");
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
        Map<String, Equipment> equipmentById;
        try {
            var catalogue = catalogueService.loadCatalogue();
            equipmentNames = catalogue.stream()
                    .collect(Collectors.toMap(Equipment::id, Equipment::name));
            equipmentById = catalogue.stream()
                    .collect(Collectors.toMap(Equipment::id, equipment -> equipment));
            requests.getItems().setAll(borrowerRequestService.listOwnRequests());
            if (requests.getItems().isEmpty()) {
                feedback.setText("You have no requests yet.");
            } else {
                feedback.setText(requests.getItems().size() + " request(s) found.");
            }
        } catch (IOException | IllegalStateException exception) {
            feedback.setText("Unable to load your requests: " + exception.getMessage());
            equipmentNames = Map.of();
            equipmentById = Map.of();
            feedback.getStyleClass().add("error-label");
        }

        Map<String, String> names = equipmentNames;
        Map<String, Equipment> equipment = equipmentById;
        requests.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    cancellationReason.setValue(null);
                    otherCancellationReason.clear();
                    renderRequestDetails(details, selected, names);
                    refreshEditControls(selected, editInfo, edit);
                    refreshCancellationControls(
                            selected, cancellationInfo, cancellationReason,
                            otherCancellationReason, cancel);
                });
        edit.setOnAction(event -> {
            LoanRequest selected = requests.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Equipment selectedEquipment = equipment.get(selected.equipmentId());
            if (selectedEquipment == null) {
                feedback.setText("Unable to edit because the equipment was not found.");
                feedback.getStyleClass().add("error-label");
                return;
            }
            showRequestForm(selectedEquipment, selected);
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
                feedback.getStyleClass().add("error-label");
            }
        });
        Button back = new Button("Back to dashboard");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        content.getChildren().addAll(
                feedback,
                requests,
                detailsPane,
                editInfo,
                edit,
                cancellationInfo,
                cancellationReason,
                otherCancellationReason,
                cancel,
                back);
        ScrollPane requestScroll = new ScrollPane(content);
        requestScroll.setFitToWidth(true);
        requestScroll.setFitToHeight(false);
        requestScroll.getStyleClass().add("request-scroll");
        showScene(requestScroll);
    }

    private HBox centeredContainer(Node child) {
        HBox container = new HBox(child);
        container.setAlignment(Pos.TOP_CENTER);
        container.setMaxWidth(Double.MAX_VALUE);
        return container;
    }

    private void refreshEditControls(LoanRequest request, Label info, Button edit) {
        boolean eligible = request != null
                && request.status() == RequestStatus.PENDING
                && request.startDate().isAfter(LocalDate.now());
        if (request == null) {
            info.setText("");
        } else if (request.status() != RequestStatus.PENDING) {
            info.setText("Editing is unavailable because this request is "
                    + request.status() + ".");
        } else if (!request.startDate().isAfter(LocalDate.now())) {
            info.setText("Editing is unavailable because the start date is today or has passed.");
        } else {
            info.setText("You can edit the purpose and dates of this pending request.");
        }
        edit.setVisible(eligible);
        edit.setManaged(eligible);
        edit.setDisable(!eligible);
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
                    + "Eligible pending requests can be edited before their start date.");
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
        VBox content = borrowerLayout("Catalogue", "Search equipment by name.");
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
            feedback.getStyleClass().add("error-label");
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
        showScene(content);
    }

    private void showRequestForm(Equipment equipment) {
        showRequestForm(equipment, null);
    }

    private void showRequestForm(Equipment equipment, LoanRequest existingRequest) {
        boolean editing = existingRequest != null;
        VBox content = borrowerLayout(
                editing ? "Edit request" : "Request equipment",
                editing ? "Update the purpose and dates before the request starts."
                        : "Submit one borrowing request.");
        Label selected = new Label(equipment.id() + " — " + equipment.name());
        selected.getStyleClass().add("selected-equipment");
        ComboBox<String> purpose = new ComboBox<>();
        purpose.getItems().addAll(
                "Academic project",
                "Personal use",
                "Event or club activity",
                "Research or lab work",
                "Other");

        TextField otherPurpose = new TextField();
        otherPurpose.setPromptText("Explain the purpose");
        if (editing) {
            if (purpose.getItems().contains(existingRequest.purpose())) {
                purpose.setValue(existingRequest.purpose());
            } else {
                purpose.setValue("Other");
                otherPurpose.setText(existingRequest.purpose());
            }
        } else {
            purpose.setValue(purpose.getItems().get(0));
        }
        VBox purposeGroup = formGroup("Purpose", purpose);
        VBox otherPurposeGroup = formGroup("Other purpose", otherPurpose);
        boolean initialOtherPurpose = "Other".equals(purpose.getValue());
        otherPurposeGroup.setVisible(initialOtherPurpose);
        otherPurposeGroup.setManaged(initialOtherPurpose);
        purpose.valueProperty().addListener((observable, oldValue, newValue) -> {
            boolean isOther = "Other".equals(newValue);
            otherPurposeGroup.setVisible(isOther);
            otherPurposeGroup.setManaged(isOther);
        });

        LocalDate initialStartDate = editing ? existingRequest.startDate() : LocalDate.now();
        LocalDate initialDueDate = editing
                ? existingRequest.dueDate()
                : initialStartDate.plusDays(14);
        DatePicker startDate = new DatePicker(initialStartDate);
        DatePicker dueDate = new DatePicker(initialDueDate);
        startDate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && oldValue != null && dueDate.getValue() != null) {
                long currentDuration = ChronoUnit.DAYS.between(oldValue, dueDate.getValue());
                if (currentDuration >= 0 && currentDuration <= 14) {
                    dueDate.setValue(newValue.plusDays(currentDuration));
                }
            }
        });
        HBox dateFields = new HBox(
                formGroup("Start date", startDate),
                formGroup("Due date", dueDate));
        dateFields.getStyleClass().add("date-fields");

        Label feedback = new Label();
        feedback.setWrapText(true);
        Button submit = new Button(editing ? "Save changes" : "Submit request");
        Button back = new Button(editing ? "Back to requests" : "Back to catalogue");
        submit.setOnAction(event -> {
            String selectedPurpose = "Other".equals(purpose.getValue())
                    ? otherPurpose.getText()
                    : purpose.getValue();
            try {
                var request = editing
                        ? borrowerRequestService.editRequest(
                                existingRequest.requestId(), selectedPurpose,
                                startDate.getValue(), dueDate.getValue())
                        : borrowerRequestService.submitRequest(
                                equipment.id(), selectedPurpose,
                                startDate.getValue(), dueDate.getValue());
                Alert confirmation = new Alert(
                        Alert.AlertType.INFORMATION,
                        (editing ? "Request updated.\n" : "Request submitted.\n")
                                + equipment.name() + "\n"
                                + request.startDate() + " to " + request.dueDate() + "\n"
                                + "Status: " + request.status());
                confirmation.setTitle(editing ? "Request updated" : "Request submitted");
                confirmation.setHeaderText(
                        editing ? "Your pending request was updated."
                                : "Your request is pending review.");
                confirmation.showAndWait();
                if (editing) {
                    showMyRequests();
                } else {
                    openDashboard(session.requireUser());
                }
            } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                feedback.setText(exception.getMessage());
                feedback.getStyleClass().add("error-label");
            }
        });
        back.setOnAction(event -> {
            if (editing) {
                showMyRequests();
            } else {
                showCatalogue();
            }
        });

        content.getChildren().addAll(
                selected,
                purposeGroup,
                otherPurposeGroup,
                dateFields,
                feedback,
                submit,
                back);
        ScrollPane formScroll = new ScrollPane(content);
        formScroll.setFitToWidth(true);
        formScroll.getStyleClass().add("form-scroll");
        showScene(formScroll);
    }

    private VBox formGroup(String labelText, Node input) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        VBox group = new VBox(6, label, input);
        group.getStyleClass().add("form-group");
        return group;
    }

    private void showReviewQueue() {
        VBox content = layout("Review Queue", "Filter borrower requests and open one to decide.");
        Label feedback = new Label();

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().add(ANY_STATUS);
        for (RequestStatus status : RequestStatus.values()) {
            statusFilter.getItems().add(status.name());
        }
        statusFilter.setValue(RequestStatus.PENDING.name());
        TextField borrowerFilter = new TextField();
        borrowerFilter.setPromptText("Borrower username");
        DatePicker from = new DatePicker();
        from.setPromptText("Starting on or after");
        DatePicker to = new DatePicker();
        to.setPromptText("Starting on or before");
        Button apply = new Button("Apply filters");
        apply.getStyleClass().add("primary-button");
        Button clear = new Button("Clear");
        clear.getStyleClass().add("secondary-button");
        HBox filterActions = new HBox(12, apply, clear);
        filterActions.setAlignment(Pos.CENTER);

        ListView<LoanRequest> requests = new ListView<>();
        requests.setPrefHeight(200);
        requests.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LoanRequest item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : item.status() + " — " + item.borrowerUsername()
                                + " — " + item.equipmentId()
                                + " (" + item.startDate() + " to " + item.dueDate() + ")");
            }
        });

        Button review = new Button("Review selected request");
        review.getStyleClass().add("primary-button");
        review.disableProperty().bind(requests.getSelectionModel().selectedItemProperty().isNull());
        review.setOnAction(event ->
                showReviewDetails(requests.getSelectionModel().getSelectedItem().requestId()));

        Runnable reload = () -> {
            try {
                String status = statusFilter.getValue();
                ReviewFilter filter = new ReviewFilter(
                        ANY_STATUS.equals(status) || status == null
                                ? null
                                : RequestStatus.valueOf(status),
                        borrowerFilter.getText(),
                        from.getValue(),
                        to.getValue());
                requests.getItems().setAll(supervisorRequestService.reviewQueue(filter));
                feedback.getStyleClass().remove("error-label");
                feedback.setText(requests.getItems().isEmpty()
                        ? "No request matches these filters."
                        : requests.getItems().size() + " request(s) found.");
            } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                requests.getItems().clear();
                feedback.setText("Unable to load the review queue: " + exception.getMessage());
                feedback.getStyleClass().add("error-label");
            }
        };
        apply.setOnAction(event -> reload.run());
        clear.setOnAction(event -> {
            statusFilter.setValue(ANY_STATUS);
            borrowerFilter.clear();
            from.setValue(null);
            to.setValue(null);
            reload.run();
        });
        reload.run();

        Button back = new Button("Back to dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        VBox filters = new VBox(10, statusFilter, borrowerFilter, from, to, filterActions);
        filters.setMaxWidth(320);
        filters.setAlignment(Pos.CENTER);
        content.getChildren().addAll(
                centeredContainer(filters), feedback, requests, review, back);
        showScrollableScene(content);
    }

    private void showReviewDetails(String requestId) {
        VBox content = layout("Review Details", "Check the request before deciding.");
        Label feedback = new Label();
        Label details = new Label();
        details.setWrapText(true);

        LoanRequest request;
        try {
            request = supervisorRequestService.findRequest(requestId);
            details.setText(reviewSummary(request));
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            feedback.setText("Unable to load the request: " + exception.getMessage());
            feedback.getStyleClass().add("error-label");
            Button failedBack = new Button("Back to the review queue");
            failedBack.setOnAction(event -> showReviewQueue());
            content.getChildren().addAll(feedback, failedBack);
            showScene(content);
            return;
        }

        TextField reason = new TextField();
        reason.setPromptText(request.status() == RequestStatus.PENDING
                ? "Reason (required to reject, optional to approve)"
                : "Cancellation reason");
        reason.setMaxWidth(360);

        Button approve = new Button("Approve");
        approve.getStyleClass().add("primary-button");
        Button reject = new Button("Reject");
        Button cancelApproved = new Button("Cancel booking");
        boolean pending = request.status() == RequestStatus.PENDING;
        boolean approved = request.status() == RequestStatus.APPROVED;
        setShown(approve, pending);
        setShown(reject, pending);
        setShown(cancelApproved, approved);

        approve.setOnAction(event ->
                decide(requestId, reason, feedback, "Approve this request?",
                        () -> supervisorRequestService.approve(requestId, reason.getText())));
        reject.setOnAction(event ->
                decide(requestId, reason, feedback, "Reject this request?",
                        () -> supervisorRequestService.reject(requestId, reason.getText())));
        cancelApproved.setOnAction(event ->
                decide(requestId, reason, feedback, "Cancel this approved booking?",
                        () -> supervisorRequestService.cancelApproved(
                                requestId, reason.getText())));

        if (!pending && !approved) {
            feedback.setText("This request is " + request.status()
                    + " and can no longer be decided.");
        }

        HBox decisions = new HBox(12, approve, reject, cancelApproved);
        decisions.setAlignment(Pos.CENTER);
        Button back = new Button("Back to the review queue");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> showReviewQueue());
        ScrollPane detailsPane = new ScrollPane(details);
        detailsPane.setFitToWidth(true);
        detailsPane.setPrefViewportHeight(240);
        content.getChildren().addAll(detailsPane, reason, decisions, feedback, back);
        showScrollableScene(content);
    }

    private void showDecisionHistory() {
        VBox content = layout("Decision History", "Every recorded decision, most recent first.");
        Label feedback = new Label();
        ListView<LoanRequest> history = new ListView<>();
        history.setPrefHeight(320);
        history.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LoanRequest item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : historyLine(item));
            }
        });
        try {
            history.getItems().setAll(supervisorRequestService.decisionHistory());
            feedback.setText(history.getItems().isEmpty()
                    ? "No decision has been recorded yet."
                    : history.getItems().size() + " decision(s) recorded.");
        } catch (IllegalStateException | IOException exception) {
            feedback.setText("Unable to load the decision history: " + exception.getMessage());
            feedback.getStyleClass().add("error-label");
        }
        Button back = new Button("Back to dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        content.getChildren().addAll(feedback, history, back);
        showScrollableScene(content);
    }

    private void decide(
            String requestId, TextField reason, Label feedback, String question,
            DecisionAction action) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, question);
        confirmation.setTitle("Confirm decision");
        confirmation.setHeaderText(question);
        if (confirmation.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }
        try {
            LoanRequest decided = action.run();
            new Alert(Alert.AlertType.INFORMATION,
                    "The request is now " + decided.status() + ".").showAndWait();
            showReviewDetails(requestId);
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            feedback.setText(exception.getMessage());
            feedback.getStyleClass().add("error-label");
        }
    }

    private String reviewSummary(LoanRequest request) {
        StringBuilder summary = new StringBuilder()
                .append("Status: ").append(request.status()).append('\n')
                .append("Borrower: ").append(request.borrowerUsername()).append('\n')
                .append("Equipment: ").append(request.equipmentId()).append('\n')
                .append("Purpose: ").append(request.purpose()).append('\n')
                .append("Requested: ").append(request.startDate())
                .append(" to ").append(request.dueDate()).append('\n');
        try {
            summary.append("Availability now: ")
                    .append(supervisorRequestService.availabilityOf(request.equipmentId()))
                    .append('\n');
            var eligibility = supervisorRequestService.eligibilityOf(request.borrowerUsername());
            summary.append("Borrower eligibility: ")
                    .append(eligibility.canSubmitRequest()
                            ? "no blockers"
                            : eligibility.blockers().toString())
                    .append('\n');
            var outstanding = supervisorRequestService.outstandingLoans(
                    request.borrowerUsername());
            if (outstanding.isEmpty()) {
                summary.append("Outstanding loans: none\n");
            } else {
                summary.append("Outstanding loans:\n");
                for (Loan loan : outstanding) {
                    summary.append("  ").append(loan.equipmentId())
                            .append(" due ").append(loan.dueDate())
                            .append(loan.isOverdue(LocalDate.now()) ? " (OVERDUE)" : "")
                            .append('\n');
                }
            }
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            summary.append("Unable to load decision context: ")
                    .append(exception.getMessage()).append('\n');
        }
        if (request.decisionBy() != null) {
            summary.append("Decided by ").append(request.decisionBy())
                    .append(" at ").append(request.decisionAt());
            if (request.decisionReason() != null) {
                summary.append(" because: ").append(request.decisionReason());
            }
            summary.append('\n');
        }
        if (request.cancelledBy() != null) {
            summary.append("Cancelled by ").append(request.cancelledBy())
                    .append(" at ").append(request.cancelledAt())
                    .append(" because: ").append(request.cancellationReason()).append('\n');
        }
        return summary.toString();
    }

    private static String historyLine(LoanRequest request) {
        String line = request.status() + " — " + request.borrowerUsername()
                + " — " + request.equipmentId();
        if (request.cancelledBy() != null) {
            return line + " — cancelled by " + request.cancelledBy()
                    + " at " + request.cancelledAt() + " — " + request.cancellationReason();
        }
        return line + " — decided by " + request.decisionBy() + " at " + request.decisionAt()
                + (request.decisionReason() == null ? "" : " — " + request.decisionReason());
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void showScrollableScene(VBox content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setFitToWidth(true);
        pane.setFitToHeight(true);
        pane.getStyleClass().add("dashboard-scroll");
        showScene(pane);
    }

    @FunctionalInterface
    private interface DecisionAction {
        LoanRequest run() throws IOException;
    }

    private VBox layout(String title, String subtitle) {
        Label heading = new Label(title);
        heading.getStyleClass().add("page-heading");
        Label description = new Label(subtitle);
        description.getStyleClass().add("page-subtitle");
        VBox content = new VBox(14, heading, description);
        content.getStyleClass().add("page-root");
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(28, 34, 32, 34));
        return content;
    }

    private VBox borrowerLayout(String title, String subtitle) {
        VBox content = layout(title, subtitle);
        content.getStyleClass().add("borrower-page");
        return content;
    }

    private void showScene(Parent content) {
        boolean wasShowing = stage.isShowing();
        boolean wasMaximized = stage.isMaximized();
        boolean wasFullScreen = stage.isFullScreen();
        double previousWidth = stage.getWidth();
        double previousHeight = stage.getHeight();
        stage.setTitle("LoanDesk");
        Scene scene = new Scene(content, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/loandesk.css").toExternalForm());
        stage.setMinWidth(WINDOW_WIDTH);
        stage.setMinHeight(WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.show();
        if (wasFullScreen) {
            stage.setFullScreen(true);
        } else if (wasMaximized) {
            stage.setMaximized(true);
        } else if (wasShowing && previousWidth > 0 && previousHeight > 0) {
            stage.setWidth(previousWidth);
            stage.setHeight(previousHeight);
        }
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
