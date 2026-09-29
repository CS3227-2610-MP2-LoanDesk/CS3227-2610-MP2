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
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
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
        switchAction.getStyleClass().add(signUp ? "auth-link-button" : "auth-outline-button");
        if (!signUp) {
            switchAction.setMaxWidth(Double.MAX_VALUE);
        }
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
        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.setMaxWidth(440);
        feedback.getStyleClass().add("auth-feedback");
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
        if (user.role() == Role.BORROWER) {
            showBorrowerDashboard(user);
            return;
        }
        if (user.role() == Role.SUPERVISOR) {
            showSupervisorDashboard(user);
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
        Button logout = new Button("Log out");
        logout.getStyleClass().add("secondary-button");
        logout.setOnAction(event -> {
            session.clear();
            showRoleSelection();
        });
        content.getChildren().add(logout);
        showScene(content);
    }

    private void showSupervisorDashboard(User user) {
        VBox page = new VBox(24);
        page.getStyleClass().add("supervisor-page");

        Label title = new Label("Supervisor Dashboard");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Review borrower requests and keep decisions accountable.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Label signedIn = new Label("Signed in as " + user.username());
        signedIn.getStyleClass().add("supervisor-signed-in");
        VBox heading = new VBox(4, title, subtitle, signedIn);
        Button logout = new Button("Log out");
        logout.getStyleClass().add("custodian-logout-button");
        logout.setOnAction(event -> {
            session.clear();
            showRoleSelection();
        });
        HBox header = new HBox(12, heading, spacer(), logout);
        header.setAlignment(Pos.CENTER_LEFT);

        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");
        int pending = 0;
        int decisions = 0;
        try {
            pending = supervisorRequestService.reviewQueue(
                    ReviewFilter.ofStatus(RequestStatus.PENDING)).size();
            decisions = supervisorRequestService.decisionHistory().size();
        } catch (IOException | IllegalStateException exception) {
            feedback.setText("Unable to load dashboard totals: " + exception.getMessage());
            feedback.getStyleClass().add("custodian-feedback-error");
        }
        VBox pendingCard = supervisorStatCard("Awaiting review", Integer.toString(pending),
                "Requests that still need a decision.", true);
        VBox decisionsCard = supervisorStatCard("Recorded decisions", Integer.toString(decisions),
                "Approved, rejected, or cancelled bookings.", false);

        Button queue = new Button("Open review queue  →");
        queue.getStyleClass().add("supervisor-primary-button");
        queue.setMaxWidth(Double.MAX_VALUE);
        queue.setOnAction(event -> showReviewQueue());
        Button history = new Button("View decision history  →");
        history.getStyleClass().add("supervisor-workflow-button");
        history.setMaxWidth(Double.MAX_VALUE);
        history.setOnAction(event -> showDecisionHistory());
        VBox queueCard = supervisorActionCard("Review queue",
                "Filter requests, inspect the details, and record a decision.", queue);
        VBox historyCard = supervisorActionCard("Decision history",
                "See the full decision record, including who acted and why.", history);
        GridPane dashboardCards = new GridPane();
        dashboardCards.setHgap(16);
        dashboardCards.setVgap(16);
        ColumnConstraints halfWidth = new ColumnConstraints();
        halfWidth.setPercentWidth(50);
        ColumnConstraints secondHalfWidth = new ColumnConstraints();
        secondHalfWidth.setPercentWidth(50);
        dashboardCards.getColumnConstraints().addAll(halfWidth, secondHalfWidth);
        dashboardCards.add(pendingCard, 0, 0);
        dashboardCards.add(decisionsCard, 1, 0);
        dashboardCards.add(queueCard, 0, 1);
        dashboardCards.add(historyCard, 1, 1);
        page.getChildren().addAll(header, feedback, dashboardCards);
        showScrollableScene(page);
    }

    private VBox supervisorStatCard(
            String heading, String value, String description, boolean useAccentRed) {
        Label title = new Label(heading);
        title.getStyleClass().add("custodian-stat-title");
        Label number = new Label(value);
        number.getStyleClass().add("custodian-stat-value");
        if (!useAccentRed) {
            number.getStyleClass().add("custodian-stat-value-blue");
        }
        Label detail = new Label(description);
        detail.getStyleClass().add("custodian-stat-description");
        detail.setWrapText(true);
        VBox card = new VBox(6, title, number, detail);
        card.getStyleClass().add("custodian-stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        return card;
    }

    private VBox supervisorActionCard(String heading, String description, Button action) {
        Label title = new Label(heading);
        title.getStyleClass().add("custodian-section-heading");
        Label detail = new Label(description);
        detail.getStyleClass().add("custodian-section-description");
        detail.setWrapText(true);
        StackPane actionSpacer = new StackPane();
        VBox.setVgrow(actionSpacer, javafx.scene.layout.Priority.ALWAYS);
        VBox card = new VBox(12, title, detail, actionSpacer, action);
        card.getStyleClass().add("supervisor-action-card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        return card;
    }

    private void showBorrowerDashboard(User user) {
        VBox page = new VBox(24);
        page.getStyleClass().add("borrower-dashboard");

        Label title = new Label("Borrower Dashboard");
        title.getStyleClass().add("borrower-dashboard-heading");
        Label subtitle = new Label("Manage your requests and keep track of your equipment loans.");
        subtitle.getStyleClass().add("borrower-dashboard-subtitle");
        Label signedIn = new Label("Signed in as " + user.username());
        signedIn.getStyleClass().add("borrower-dashboard-signed-in");
        VBox heading = new VBox(4, title, subtitle, signedIn);

        Button logout = new Button("Log out");
        logout.getStyleClass().add("custodian-logout-button");
        logout.setOnAction(event -> {
            session.clear();
            showRoleSelection();
        });
        HBox header = new HBox(12, heading, spacer(), logout);
        header.setAlignment(Pos.CENTER_LEFT);

        Button catalogue = new Button("View Catalogue  →");
        catalogue.getStyleClass().add("borrower-workflow-button");
        catalogue.setMaxWidth(Double.MAX_VALUE);
        catalogue.setOnAction(event -> showCatalogue());
        Button requests = new Button("My Requests  →");
        requests.getStyleClass().add("borrower-workflow-button");
        requests.setMaxWidth(Double.MAX_VALUE);
        requests.setOnAction(event -> showMyRequests());
        HBox actions = new HBox(16,
                borrowerActionCard(catalogue, "Browse equipment",
                        "View the catalogue and start a request when you find what you need."),
                borrowerActionCard(requests, "Manage requests",
                        "Review request statuses and take any available actions."));
        actions.getStyleClass().add("borrower-action-row");

        Label loansHeading = new Label("My Loans");
        loansHeading.getStyleClass().add("borrower-section-heading");
        Label loansDescription = new Label("Your equipment that is currently on loan.");
        loansDescription.getStyleClass().add("borrower-section-description");
        VBox loansHeadingGroup = new VBox(3, loansHeading, loansDescription);
        Button history = new Button("View past loans");
        history.getStyleClass().add("custodian-secondary-button");
        history.setOnAction(event -> showMyLoans());
        HBox loansHeader = new HBox(12, loansHeadingGroup, spacer(), history);
        loansHeader.setAlignment(Pos.CENTER_LEFT);

        TableView<Loan> activeLoans = new TableView<>();
        activeLoans.getStyleClass().addAll("custodian-table", "borrower-active-loans-table");
        configureCompactTable(activeLoans);
        Map<String, String> equipmentNames = Map.of();
        try {
            equipmentNames = catalogueService.loadCatalogue().stream()
                    .collect(Collectors.toMap(Equipment::id, Equipment::name));
            activeLoans.getItems().setAll(borrowerLoanService.listOwnLoans().stream()
                    .filter(loan -> loan.status() == LoanStatus.ACTIVE)
                    .toList());
            activeLoans.setPlaceholder(new Label("You have no active loans right now."));
        } catch (IOException | IllegalStateException exception) {
            activeLoans.setPlaceholder(new Label("Unable to load active loans: " + exception.getMessage()));
        }
        Map<String, String> names = equipmentNames;
        activeLoans.getColumns().addAll(
                textColumn("Equipment", loan -> names.getOrDefault(
                        loan.equipmentId(), "Unknown equipment")),
                textColumn("Checked out", loan -> loan.checkoutDate().toString()),
                textColumn("Due date", loan -> loan.dueDate().toString()),
                textColumn("Status", this::loanDisplayStatus));

        VBox loansPanel = new VBox(16, loansHeader, activeLoans);
        loansPanel.getStyleClass().add("borrower-loans-panel");
        page.getChildren().addAll(header, actions, loansPanel);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("custodian-dashboard-scroll");
        showScene(scroll);
    }

    private VBox borrowerActionCard(Button action, String heading, String description) {
        Label title = new Label(heading);
        title.getStyleClass().add("borrower-action-heading");
        Label detail = new Label(description);
        detail.getStyleClass().add("borrower-action-description");
        detail.setWrapText(true);
        StackPane actionSpacer = new StackPane();
        VBox.setVgrow(actionSpacer, javafx.scene.layout.Priority.ALWAYS);
        VBox card = new VBox(12, title, detail, actionSpacer, action);
        card.getStyleClass().add("borrower-action-card");
        HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        return card;
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
        inventory.getStyleClass().add("custodian-workflow-button");
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
        StackPane loanOverlay = new StackPane();
        loanOverlay.getStyleClass().add("custodian-overlay");
        loanOverlay.setVisible(false);
        loanOverlay.setManaged(false);

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
                loanActionColumn(loanOverlay, names));

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
        StackPane root = new StackPane(scroll, loanOverlay);
        showScene(root);
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

    private TableColumn<CustodianInventoryService.InventoryItem, CustodianInventoryService.InventoryItem>
            inventoryNameColumn(Label feedback, Runnable reload) {
        TableColumn<CustodianInventoryService.InventoryItem, CustodianInventoryService.InventoryItem> column
                = new TableColumn<>("Item");
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(table -> new TableCell<>() {
            @Override
            protected void updateItem(CustodianInventoryService.InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Equipment equipment = item.equipment();
                Label name = new Label(equipment.name());
                name.getStyleClass().add("custodian-item-name");
                Button edit = new Button();
                edit.setGraphic(penIcon());
                edit.setAccessibleText("Edit name for " + equipment.name());
                edit.setTooltip(new Tooltip("Edit item name"));
                edit.getStyleClass().add("custodian-inline-edit-button");
                HBox nameLine = new HBox(4, name, edit);
                nameLine.setAlignment(Pos.CENTER_LEFT);
                Label identifier = new Label(equipment.id());
                identifier.getStyleClass().add("custodian-item-id");
                VBox display = new VBox(1, nameLine, identifier);
                edit.setOnAction(event -> showInlineNameEditor(
                        display, equipment, identifier, feedback, reload));
                setGraphic(display);
            }
        });
        return column;
    }

    private void showInlineNameEditor(
            VBox display, Equipment equipment, Label identifier, Label feedback, Runnable reload) {
        TextField name = new TextField(equipment.name());
        name.getStyleClass().add("custodian-inline-name-field");
        name.setMaxWidth(Double.MAX_VALUE);
        Button save = new Button();
        save.setGraphic(tickIcon());
        save.setAccessibleText("Save name for " + equipment.name());
        save.setTooltip(new Tooltip("Save item name"));
        save.getStyleClass().add("custodian-inline-save-button");
        HBox editor = new HBox(4, name, save);
        editor.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);
        editor.setMaxWidth(Double.MAX_VALUE);
        display.setMaxWidth(Double.MAX_VALUE);
        display.getChildren().setAll(editor, identifier);
        name.requestFocus();
        save.setOnAction(event -> {
            save.setDisable(true);
            runInventoryAction(feedback, reload, () -> custodianInventoryService.updateEquipment(
                    equipment.id(), name.getText(), equipment.condition()), "Item name updated.");
        });
        name.setOnAction(event -> save.fire());
    }

    private Node penIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent("M3,14.5 L3,17 L5.5,17 L15.8,6.7 L13.3,4.2 Z M14.7,2.8 L17.2,5.3 L18.3,4.2 C18.9,3.6 18.9,2.7 18.3,2.1 C17.7,1.5 16.8,1.5 16.2,2.1 Z");
        icon.setFill(Color.web("#5b6370"));
        return icon;
    }

    private Node tickIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent("M3,10 L7.2,14.2 L17,4.4 L19,6.4 L7.2,18 L1,12 Z");
        icon.setFill(Color.web("#ffffff"));
        return icon;
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
                checkout.getStyleClass().add("custodian-workflow-button");
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

    private TableColumn<Loan, Loan> loanActionColumn(StackPane loanOverlay, Map<String, String> equipmentNames) {
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
                manage.getStyleClass().add("custodian-workflow-button");
                manage.setOnAction(event -> showLoanOverlay(loanOverlay, loan, equipmentNames));
                setGraphic(manage);
            }
        });
        return column;
    }

    private void showLoanOverlay(StackPane overlay, Loan loan, Map<String, String> equipmentNames) {
        String equipmentName = equipmentNames.getOrDefault(loan.equipmentId(), loan.equipmentId());
        Label title = new Label("Loan details");
        title.getStyleClass().add("custodian-section-heading");
        Label item = new Label(equipmentName);
        item.getStyleClass().add("custodian-detail-value");
        Label itemId = new Label(loan.equipmentId());
        itemId.getStyleClass().add("custodian-item-id");
        VBox identity = new VBox(2, item, itemId);
        VBox details = new VBox(8,
                detailRow("Borrower", loan.borrowerUsername()),
                detailRow("Status", loanDisplayStatus(loan)),
                detailRow("Collection date", loan.checkoutDate().toString()),
                detailRow("Due date", loan.dueDate().toString()));
        Label panelFeedback = new Label();
        panelFeedback.getStyleClass().add("custodian-feedback");
        panelFeedback.setVisible(false);
        panelFeedback.setManaged(false);
        ComboBox<EquipmentCondition> returnCondition = new ComboBox<>();
        returnCondition.getItems().addAll(EquipmentCondition.GOOD, EquipmentCondition.DAMAGED,
                EquipmentCondition.UNDER_MAINTENANCE);
        returnCondition.setPromptText("Select condition");
        returnCondition.getStyleClass().add("custodian-filter-select");
        Label conditionLabel = new Label("Return condition *");
        conditionLabel.getStyleClass().add("custodian-form-label");
        VBox conditionField = new VBox(6, conditionLabel, returnCondition);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("custodian-secondary-button");
        cancel.setOnAction(event -> hideOverlay(overlay));
        Button returnLoan = new Button("Mark as returned");
        returnLoan.getStyleClass().add("custodian-primary-button");
        returnLoan.getStyleClass().add("custodian-create-button");
        returnLoan.disableProperty().bind(Bindings.createBooleanBinding(
                () -> loan.status() != LoanStatus.ACTIVE || returnCondition.getValue() == null,
                returnCondition.valueProperty()));
        returnLoan.setTooltip(new Tooltip(loan.status() == LoanStatus.LOST
                ? "Recover the item before recording its return."
                : "Select the observed return condition."));
        returnLoan.setOnAction(event -> runLoanOverlayAction(panelFeedback, overlay, () ->
                custodianFulfilmentService.returnLoan(loan.loanId(), returnCondition.getValue()),
                "Return recorded."));

        Button markLost = new Button("Mark as lost");
        markLost.getStyleClass().add("custodian-danger-button");
        markLost.setDisable(loan.status() == LoanStatus.LOST);
        markLost.setTooltip(new Tooltip(loan.status() == LoanStatus.LOST
                ? "This item is already marked lost." : "Mark this active loan and item as lost."));
        markLost.setOnAction(event -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Mark " + equipmentName + " as lost? The item will become unavailable.");
            confirmation.setTitle("Confirm lost item");
            confirmation.setHeaderText("Mark item as lost");
            if (confirmation.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
                runLoanOverlayAction(panelFeedback, overlay,
                        () -> custodianFulfilmentService.markLost(loan.loanId()), "Item marked lost.");
            }
        });
        Button recover = new Button("Recover item");
        recover.getStyleClass().add("custodian-secondary-button");
        recover.setVisible(loan.status() == LoanStatus.LOST);
        recover.setManaged(loan.status() == LoanStatus.LOST);
        recover.setOnAction(event -> runLoanOverlayAction(panelFeedback, overlay,
                () -> custodianFulfilmentService.recoverLost(loan.loanId()),
                "Lost item recovered. Record its observed return when ready."));
        HBox actions = new HBox(10, cancel, recover, markLost, returnLoan);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox panel = new VBox(16, title, identity, details, conditionField, panelFeedback, actions);
        panel.getStyleClass().add("custodian-loan-overlay-panel");
        overlay.getChildren().setAll(panel);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void runLoanOverlayAction(
            Label panelFeedback, StackPane overlay, LoanAction action, String success) {
        try {
            action.run();
            rememberCustodianDashboardMessage(success, false);
            hideOverlay(overlay);
            showCustodianDashboard(session.requireUser());
        } catch (StaleDataException exception) {
            showCustodianFeedback(panelFeedback, "Loan data changed. Close and retry.", true);
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            showCustodianFeedback(panelFeedback, exception.getMessage(), true);
        }
    }

    private void hideOverlay(StackPane overlay) {
        overlay.setVisible(false);
        overlay.setManaged(false);
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
        if (title.equals("Active loans") || title.equals("Total inventory")) {
            statValue.getStyleClass().add("custodian-stat-value-blue");
        }
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
        Label nameLabel = new Label("Name");
        nameLabel.getStyleClass().add("custodian-form-label");
        ComboBox<EquipmentCondition> initialCondition = new ComboBox<>();
        initialCondition.getItems().setAll(EquipmentCondition.values());
        initialCondition.setValue(EquipmentCondition.GOOD);
        initialCondition.getStyleClass().add("custodian-filter-select");
        Label conditionLabel = new Label("Condition");
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
                inventoryNameColumn(feedback, reload),
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
            showCustodianFeedback(feedback, success, false);
            reload.run();
        } catch (StaleDataException exception) {
            showCustodianFeedback(feedback, "Inventory data changed. Refresh the list and retry.", true);
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            showCustodianFeedback(feedback, exception.getMessage(), true);
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
        VBox content = new VBox(24);
        content.getStyleClass().add("borrower-history-page");
        Label title = new Label("Past Loans");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Your returned equipment loan history.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Button back = new Button("← Back to dashboard");
        back.getStyleClass().add("custodian-secondary-button");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        HBox header = new HBox(12, new VBox(4, title, subtitle), spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);
        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");
        TableView<Loan> history = new TableView<>();
        history.getStyleClass().addAll("custodian-table", "borrower-history-table");
        configureCompactTable(history);
        history.setPrefHeight(330);
        history.setMaxHeight(330);
        Map<String, String> equipmentNames;
        boolean loadFailed = false;
        try {
            equipmentNames = catalogueService.loadCatalogue().stream()
                    .collect(Collectors.toMap(Equipment::id, Equipment::name));
            java.util.List<Loan> loans = borrowerLoanService.listOwnLoans();
            history.getItems().setAll(loans.stream()
                    .filter(loan -> loan.status() == LoanStatus.RETURNED)
                    .toList());
            feedback.setText("Review your returned-loan history below.");
        } catch (IOException | IllegalStateException exception) {
            equipmentNames = Map.of();
            loadFailed = true;
            feedback.setText("Unable to load your loans: " + exception.getMessage());
            feedback.getStyleClass().add("error-label");
        }

        Map<String, String> names = equipmentNames;
        history.getColumns().addAll(
                textColumn("Equipment", loan -> names.getOrDefault(
                        loan.equipmentId(), "Unknown equipment")),
                textColumn("Checked out", loan -> loan.checkoutDate().toString()),
                textColumn("Returned", loan -> loan.returnedDate().toString()),
                textColumn("Status", loan -> loan.status().name()));
        Label historyEmpty = new Label(loadFailed
                ? "Unable to load loan history."
                : "You have no past loans yet.");
        history.setPlaceholder(historyEmpty);
        VBox historyPanel = new VBox(16, history);
        historyPanel.getStyleClass().add("borrower-history-panel");
        content.getChildren().addAll(header, feedback, historyPanel);
        showScrollableScene(content);
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
        VBox content = new VBox(24);
        content.getStyleClass().add("borrower-requests-page");
        Label title = new Label("My Requests");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Review your active requests and request history.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Button back = new Button("← Back to dashboard");
        back.getStyleClass().add("custodian-secondary-button");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        HBox header = new HBox(12, new VBox(4, title, subtitle), spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);
        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");
        TableView<LoanRequest> requests = new TableView<>();
        requests.getStyleClass().addAll("custodian-table", "borrower-requests-table");
        configureCompactTable(requests);
        requests.setPrefHeight(330);
        requests.setMaxHeight(330);

        Label details = new Label(
                "Select a request to view its details.\n"
                        + "Eligible pending requests can be edited before their start date.");
        details.setWrapText(true);
        ScrollPane detailsPane = new ScrollPane(details);
        detailsPane.setFitToWidth(true);
        detailsPane.setPrefViewportHeight(140);
        detailsPane.getStyleClass().add("borrower-request-details-scroll");
        Label editInfo = new Label();
        Button edit = new Button("Edit request");
        edit.getStyleClass().add("borrower-workflow-button");
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
        cancel.getStyleClass().add("custodian-danger-button");
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
        requests.getColumns().addAll(
                textColumn("Equipment", request -> names.getOrDefault(
                        request.equipmentId(), "Unknown equipment")),
                textColumn("Status", request -> request.status().name()),
                textColumn("Start date", request -> request.startDate().toString()),
                textColumn("Due date", request -> request.dueDate().toString()),
                textColumn("Requested", this::requestDate));
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
        VBox detailsPanel = new VBox(12,
                detailsPane,
                editInfo,
                edit,
                cancellationInfo,
                cancellationReason,
                otherCancellationReason,
                cancel);
        detailsPanel.getStyleClass().add("borrower-request-details");
        content.getChildren().addAll(
                header,
                feedback,
                requests,
                detailsPanel);
        showScrollableScene(content);
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
                .append("Equipment: ").append(equipmentName).append("\n")
                .append("Purpose: ").append(request.purpose()).append("\n")
                .append("Dates: ").append(request.startDate()).append(" to ").append(request.dueDate()).append("\n")
                .append("Status: ").append(request.status()).append("\n")
                .append("Requested: ").append(requestDate(request));
        if (request.decisionReason() != null) {
            text.append("\nDecision reason: ").append(request.decisionReason());
        }
        if (request.cancellationReason() != null) {
            text.append("\nCancellation reason: ").append(request.cancellationReason());
        }
        details.setText(text.toString());
    }

    private String requestDate(LoanRequest request) {
        return request.createdAt() == null
                ? "Not available"
                : request.createdAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
    }

    private void showCatalogue() {
        VBox content = new VBox(24);
        content.getStyleClass().addAll("custodian-inventory-page", "borrower-catalogue-page");
        Label title = new Label("Catalogue");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Browse equipment and see its current availability.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Button back = new Button("← Back to dashboard");
        back.getStyleClass().add("custodian-secondary-button");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        HBox header = new HBox(12, new VBox(4, title, subtitle), spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);

        TextField filter = new TextField();
        filter.setPromptText("Search by equipment name");
        filter.getStyleClass().add("custodian-filter-field");
        Button apply = new Button("Filter");
        apply.getStyleClass().add("borrower-workflow-button");
        Button clear = new Button("Clear");
        clear.getStyleClass().add("custodian-secondary-button");
        Button request = new Button("Request selected equipment  →");
        request.getStyleClass().add("borrower-workflow-button");
        HBox filterActions = new HBox(12, apply, clear);
        filterActions.setAlignment(Pos.CENTER_LEFT);
        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");
        TableView<CatalogueService.CatalogueItem> results = new TableView<>();
        results.getStyleClass().addAll("custodian-table", "borrower-catalogue-table");
        configureCompactTable(results);
        results.getColumns().addAll(
                textColumn("Equipment name", item -> item.equipment().name()),
                textColumn("Availability", item -> item.availability().name()));
        request.disableProperty().bind(Bindings.createBooleanBinding(
                () -> results.getSelectionModel().getSelectedItem() == null
                        || results.getSelectionModel().getSelectedItem().availability()
                                != loandesk.domain.AvailabilityStatus.AVAILABLE,
                results.getSelectionModel().selectedItemProperty()));
        request.setTooltip(new Tooltip("Only equipment marked AVAILABLE can be requested."));

        final java.util.List<CatalogueService.CatalogueItem> equipment;
        try {
            equipment = catalogueService.loadCatalogueWithAvailability();
        } catch (IOException | IllegalStateException exception) {
            showCustodianFeedback(feedback, "Unable to load the catalogue: " + exception.getMessage(), true);
            content.getChildren().addAll(header, feedback);
            showScrollableScene(content);
            return;
        }

        Runnable renderResults = () -> {
            java.util.List<CatalogueService.CatalogueItem> filtered = catalogueService
                    .filterAndSortByName(equipment, filter.getText());
            results.getItems().setAll(filtered);
            if (filtered.isEmpty()) {
                showCustodianFeedback(feedback, equipment.isEmpty()
                        ? "The catalogue is currently empty."
                        : "No equipment matches that name.", false);
            } else {
                feedback.setVisible(false);
                feedback.setManaged(false);
            }
        };
        apply.setOnAction(event -> renderResults.run());
        clear.setOnAction(event -> {
            filter.clear();
            renderResults.run();
        });
        filter.setOnAction(event -> renderResults.run());
        request.setOnAction(event -> showRequestForm(
                results.getSelectionModel().getSelectedItem().equipment()));
        renderResults.run();

        HBox catalogueActions = new HBox(12, request);
        catalogueActions.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().addAll(header, filter, filterActions, feedback, results, catalogueActions);
        showScrollableScene(content);
    }

    private void showRequestForm(Equipment equipment) {
        showRequestForm(equipment, null);
    }

    private void showRequestForm(Equipment equipment, LoanRequest existingRequest) {
        boolean editing = existingRequest != null;
        VBox content = new VBox(24);
        content.getStyleClass().add("borrower-request-page");
        Label title = new Label(editing ? "Edit request" : "Request equipment");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label(editing
                ? "Update the purpose and dates before the request starts."
                : "Submit one borrowing request.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Button back = new Button(editing ? "← Back to requests" : "← Back to catalogue");
        back.getStyleClass().add("custodian-secondary-button");
        back.setOnAction(event -> {
            if (editing) {
                showMyRequests();
            } else {
                showCatalogue();
            }
        });
        HBox header = new HBox(12, new VBox(4, title, subtitle), spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);

        Label selected = new Label("Equipment: " + equipment.name());
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
        VBox purposeGroup = formGroup("Purpose *", purpose);
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
        startDate.setEditable(false);
        dueDate.setEditable(false);
        startDate.setDayCellFactory(picker -> disabledPastDateCell());
        dueDate.setDayCellFactory(picker -> disabledInvalidDueDateCell(startDate));
        startDate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && oldValue != null && dueDate.getValue() != null) {
                long currentDuration = ChronoUnit.DAYS.between(oldValue, dueDate.getValue());
                if (currentDuration >= 0 && currentDuration <= 14) {
                    dueDate.setValue(newValue.plusDays(currentDuration));
                }
            }
            dueDate.setDayCellFactory(picker -> disabledInvalidDueDateCell(startDate));
        });
        HBox dateFields = new HBox(
                formGroup("Start date *", startDate),
                formGroup("Due date *", dueDate));
        dateFields.getStyleClass().add("date-fields");

        Label feedback = new Label();
        feedback.setWrapText(true);
        feedback.getStyleClass().add("request-feedback");
        feedback.setManaged(false);
        feedback.setVisible(false);
        Button submit = new Button(editing ? "Save changes" : "Submit request");
        submit.getStyleClass().add("auth-submit-button");
        submit.setOnAction(event -> {
            String selectedPurpose = "Other".equals(purpose.getValue())
                    ? otherPurpose.getText()
                    : purpose.getValue();
            submit.setDisable(true);
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
                confirmation.getDialogPane().getStyleClass().add("request-confirmation-dialog");
                confirmation.getDialogPane().setPrefWidth(420);
                confirmation.showAndWait();
                if (editing) {
                    showMyRequests();
                } else {
                    openDashboard(session.requireUser());
                }
            } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                feedback.setText(exception.getMessage());
                feedback.getStyleClass().add("error-label");
                feedback.setManaged(true);
                feedback.setVisible(true);
            } finally {
                submit.setDisable(false);
            }
        });

        Label requiredNote = new Label("* Required fields");
        requiredNote.getStyleClass().add("request-required-note");
        HBox formActions = new HBox(12, submit);
        formActions.setAlignment(Pos.CENTER_RIGHT);
        VBox form = new VBox(16,
                selected,
                purposeGroup,
                otherPurposeGroup,
                dateFields,
                requiredNote,
                feedback,
                formActions);
        form.getStyleClass().add("borrower-request-form");
        content.getChildren().addAll(header, form);
        ScrollPane formScroll = new ScrollPane(content);
        formScroll.setFitToWidth(true);
        formScroll.getStyleClass().add("form-scroll");
        showScene(formScroll);
    }

    private DateCell disabledPastDateCell() {
        return new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(!empty && item.isBefore(LocalDate.now()));
            }
        };
    }

    private DateCell disabledInvalidDueDateCell(DatePicker startDate) {
        return new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                LocalDate earliestDate = startDate.getValue() == null
                        ? LocalDate.now()
                        : startDate.getValue();
                setDisable(!empty && (item.isBefore(earliestDate)
                        || item.isAfter(earliestDate.plusDays(14))));
            }
        };
    }

    private VBox formGroup(String labelText, Node input) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        VBox group = new VBox(6, label, input);
        group.getStyleClass().add("form-group");
        return group;
    }

    private void showReviewQueue() {
        VBox content = new VBox(24);
        content.getStyleClass().add("supervisor-page");
        Label title = new Label("Review Queue");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Filter borrower requests, then open a record to make a decision.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Button back = new Button("← Back to dashboard");
        back.getStyleClass().add("custodian-secondary-button");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        HBox header = new HBox(12, new VBox(4, title, subtitle), spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);
        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");

        TextField borrowerFilter = new TextField();
        borrowerFilter.setPromptText("Borrower username");
        TextField equipmentFilter = new TextField();
        equipmentFilter.setPromptText("Equipment name");
        DatePicker from = new DatePicker();
        from.setPromptText("Start date from");
        DatePicker to = new DatePicker();
        to.setPromptText("Start date to");
        Button apply = new Button("Apply filters");
        apply.getStyleClass().add("supervisor-workflow-button");
        Button clear = new Button("Clear");
        clear.getStyleClass().add("custodian-secondary-button");
        HBox filterActions = new HBox(12, apply, clear);
        filterActions.setAlignment(Pos.CENTER_LEFT);

        TableView<LoanRequest> requests = new TableView<>();
        requests.getStyleClass().addAll("custodian-table", "supervisor-review-table");
        configureCompactTable(requests);
        requests.setPrefHeight(330);
        requests.setMaxHeight(330);
        requests.getColumns().addAll(
                textColumn("Status", request -> request.status().name()),
                textColumn("Borrower", LoanRequest::borrowerUsername),
                textColumn("Equipment", LoanRequest::equipmentId),
                textColumn("Purpose", LoanRequest::purpose),
                textColumn("Start date", request -> request.startDate().toString()),
                textColumn("Due date", request -> request.dueDate().toString()),
                supervisorReviewActionColumn());

        Runnable reload = () -> {
            try {
                ReviewFilter filter = new ReviewFilter(
                        RequestStatus.PENDING,
                        borrowerFilter.getText(),
                        from.getValue(),
                        to.getValue(),
                        equipmentFilter.getText());
                requests.getItems().setAll(supervisorRequestService.reviewQueue(filter));
                feedback.getStyleClass().remove("custodian-feedback-error");
                feedback.setText(requests.getItems().isEmpty()
                        ? "No requests match these filters. Try adjusting or clearing them."
                        : requests.getItems().size() + " request(s) found.");
            } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
                requests.getItems().clear();
                feedback.setText("Unable to load the review queue: " + exception.getMessage());
                feedback.getStyleClass().add("custodian-feedback-error");
            }
        };
        apply.setOnAction(event -> reload.run());
        clear.setOnAction(event -> {
            borrowerFilter.clear();
            equipmentFilter.clear();
            from.setValue(null);
            to.setValue(null);
            reload.run();
        });
        reload.run();

        borrowerFilter.getStyleClass().add("custodian-filter-field");
        equipmentFilter.getStyleClass().add("custodian-filter-field");
        from.getStyleClass().add("custodian-filter-select");
        to.getStyleClass().add("custodian-filter-select");
        VBox dateFilter = supervisorFilterGroup("Requested start date", new HBox(10, from, to));
        VBox actionFilter = supervisorFilterGroup("Actions", filterActions);
        HBox filterFields = new HBox(12,
                supervisorFilterGroup("Borrower", borrowerFilter),
                supervisorFilterGroup("Equipment", equipmentFilter),
                dateFilter,
                actionFilter);
        filterFields.setAlignment(Pos.BOTTOM_LEFT);
        VBox filters = new VBox(12, filterFields);
        filters.getStyleClass().add("supervisor-filter-panel");
        VBox tablePanel = new VBox(14, feedback, requests);
        tablePanel.getStyleClass().add("custodian-panel");
        content.getChildren().addAll(header, filters, tablePanel);
        showScrollableScene(content);
    }

    private VBox supervisorFilterGroup(String labelText, Node input) {
        Label label = new Label(labelText);
        VBox group = new VBox(6, label, input);
        group.getStyleClass().add("supervisor-filter-group");
        return group;
    }

    private TableColumn<LoanRequest, LoanRequest> supervisorReviewActionColumn() {
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
                Button review = new Button("Review");
                review.getStyleClass().add("supervisor-table-action");
                review.setOnAction(event -> showReviewDetails(request.requestId()));
                setGraphic(review);
            }
        });
        return column;
    }

    private void showReviewDetails(String requestId) {
        VBox content = new VBox(24);
        content.getStyleClass().add("supervisor-page");
        Label title = new Label("Review Request");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Inspect the request and its current eligibility before recording a decision.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Button back = new Button("← Back to review queue");
        back.getStyleClass().add("custodian-secondary-button");
        back.setOnAction(event -> showReviewQueue());
        HBox header = new HBox(12, new VBox(4, title, subtitle), spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);
        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");

        LoanRequest request;
        try {
            request = supervisorRequestService.findRequest(requestId);
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            feedback.setText("Unable to load the request: " + exception.getMessage());
            feedback.getStyleClass().add("custodian-feedback-error");
            content.getChildren().addAll(header, feedback);
            showScrollableScene(content);
            return;
        }

        Label requestTitle = new Label("Request details");
        requestTitle.getStyleClass().add("custodian-section-heading");
        Label requestIdLabel = new Label("Request " + request.requestId());
        requestIdLabel.getStyleClass().add("supervisor-request-id");
        HBox requestIdentity = new HBox(10, requestIdLabel, supervisorOutcomePill(request.status()));
        requestIdentity.getStyleClass().add("supervisor-request-identity");
        VBox requestDetails = new VBox(12, requestTitle, requestIdentity,
                supervisorDetailRow("Borrower", request.borrowerUsername()),
                supervisorDetailRow("Equipment", request.equipmentId()),
                supervisorDetailRow("Purpose", request.purpose()),
                supervisorDetailRow("Requested dates", request.startDate() + " to " + request.dueDate()));
        requestDetails.getStyleClass().add("supervisor-detail-card");
        requestDetails.setMaxWidth(Double.MAX_VALUE);

        VBox decisionContext = supervisorDecisionContext(request);
        decisionContext.setMaxWidth(Double.MAX_VALUE);
        GridPane overview = new GridPane();
        overview.setHgap(16);
        ColumnConstraints requestDetailsColumn = new ColumnConstraints();
        requestDetailsColumn.setPercentWidth(50);
        ColumnConstraints reviewContextColumn = new ColumnConstraints();
        reviewContextColumn.setPercentWidth(50);
        overview.getColumnConstraints().addAll(requestDetailsColumn, reviewContextColumn);
        overview.add(requestDetails, 0, 0);
        overview.add(decisionContext, 1, 0);
        overview.getStyleClass().add("supervisor-review-overview");

        TextField reason = new TextField();
        reason.setPromptText(request.status() == RequestStatus.PENDING
                ? "Reason (required to reject, optional to approve)"
                : "Cancellation reason");
        reason.getStyleClass().add("supervisor-reason-field");
        reason.setMaxWidth(Double.MAX_VALUE);
        Label reasonLabel = new Label(request.status() == RequestStatus.PENDING
                ? "Decision note" : "Cancellation reason *");
        reasonLabel.getStyleClass().add("supervisor-detail-label");

        Button approve = new Button("Approve");
        approve.getStyleClass().add("supervisor-primary-button");
        Button reject = new Button("Reject");
        reject.getStyleClass().add("supervisor-danger-button");
        Button cancelApproved = new Button("Cancel booking");
        cancelApproved.getStyleClass().add("supervisor-danger-button");
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
            feedback.getStyleClass().add("custodian-feedback-error");
        }

        HBox decisions = new HBox(12, approve, reject, cancelApproved);
        decisions.setAlignment(Pos.CENTER_RIGHT);
        VBox decisionPanel = new VBox(10,
                new Label("Record a decision"), reasonLabel, reason, feedback, decisions);
        decisionPanel.getStyleClass().add("supervisor-decision-panel");
        ((Label) decisionPanel.getChildren().get(0)).getStyleClass().add("custodian-section-heading");
        content.getChildren().addAll(header, overview, decisionPanel);
        showScrollableScene(content);
    }

    private VBox supervisorDecisionContext(LoanRequest request) {
        Label title = new Label("Current review context");
        title.getStyleClass().add("custodian-section-heading");
        VBox context = new VBox(12, title);
        context.getStyleClass().add("supervisor-detail-card");
        try {
            context.getChildren().add(supervisorDetailRow("Availability",
                    supervisorRequestService.availabilityOf(request.equipmentId()).name()));
            var eligibility = supervisorRequestService.eligibilityOf(request.borrowerUsername());
            context.getChildren().add(supervisorDetailRow("Borrower eligibility",
                    eligibility.canSubmitRequest() ? "Eligible" : eligibility.blockers().toString()));
            var outstanding = supervisorRequestService.outstandingLoans(request.borrowerUsername());
            context.getChildren().add(supervisorDetailRow("Outstanding loans", outstanding.isEmpty()
                    ? "None" : outstanding.stream()
                            .map(loan -> equipmentNameForReview(loan.equipmentId())
                                    + " (due " + loan.dueDate() + ")")
                            .collect(Collectors.joining(", "))));
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            context.getChildren().add(supervisorDetailRow("Review context",
                    "Unable to load: " + exception.getMessage()));
        }
        return context;
    }

    private String equipmentNameForReview(String equipmentId) {
        try {
            return supervisorRequestService.equipmentNameOf(equipmentId);
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            return equipmentId;
        }
    }

    private VBox supervisorDetailRow(String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("supervisor-detail-label");
        Label detail = new Label(value == null || value.isBlank() ? "—" : value);
        detail.getStyleClass().add("supervisor-detail-value");
        detail.setWrapText(true);
        return new VBox(3, label, detail);
    }

    private void showDecisionHistory() {
        VBox content = new VBox(24);
        content.getStyleClass().add("supervisor-page");
        Label title = new Label("Decision History");
        title.getStyleClass().add("custodian-page-heading");
        Label subtitle = new Label("Every recorded decision, most recent first.");
        subtitle.getStyleClass().add("custodian-page-subtitle");
        Button back = new Button("← Back to dashboard");
        back.getStyleClass().add("custodian-secondary-button");
        back.setOnAction(event -> openDashboard(session.requireUser()));
        HBox header = new HBox(12, new VBox(4, title, subtitle), spacer(), back);
        header.setAlignment(Pos.CENTER_LEFT);
        Label feedback = new Label();
        feedback.getStyleClass().add("custodian-feedback");
        TableView<LoanRequest> history = new TableView<>();
        history.getStyleClass().addAll("custodian-table", "supervisor-history-table");
        configureCompactTable(history);
        history.setPrefHeight(380);
        history.setMaxHeight(380);
        history.getColumns().addAll(
                supervisorOutcomeColumn(),
                textColumn("Borrower", LoanRequest::borrowerUsername),
                textColumn("Equipment", LoanRequest::equipmentId),
                textColumn("Decided by", request -> request.cancelledBy() != null
                        ? request.cancelledBy() : request.decisionBy()),
                textColumn("Decision date", request -> request.cancelledAt() != null
                        ? request.cancelledAt().toString() : request.decisionAt().toString()),
                textColumn("Reason", this::decisionReason));
        try {
            history.getItems().setAll(supervisorRequestService.decisionHistory());
            feedback.setText(history.getItems().isEmpty()
                    ? "No decision has been recorded yet."
                    : history.getItems().size() + " decision(s) recorded.");
        } catch (IllegalStateException | IOException exception) {
            feedback.setText("Unable to load the decision history: " + exception.getMessage());
            feedback.getStyleClass().add("custodian-feedback-error");
        }
        history.setPlaceholder(new Label("No decisions have been recorded yet."));
        VBox tablePanel = new VBox(8, feedback, history);
        tablePanel.getStyleClass().addAll("custodian-panel", "supervisor-history-panel");
        content.getChildren().addAll(header, tablePanel);
        showScrollableScene(content);
    }

    private String decisionReason(LoanRequest request) {
        String reason = request.cancelledBy() != null
                ? request.cancellationReason() : request.decisionReason();
        return reason == null || reason.isBlank() ? "—" : reason;
    }

    private TableColumn<LoanRequest, LoanRequest> supervisorOutcomeColumn() {
        TableColumn<LoanRequest, LoanRequest> column = new TableColumn<>("Outcome");
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(table -> new TableCell<>() {
            @Override
            protected void updateItem(LoanRequest request, boolean empty) {
                super.updateItem(request, empty);
                if (empty || request == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(supervisorOutcomePill(request.status()));
            }
        });
        return column;
    }

    private Label supervisorOutcomePill(RequestStatus status) {
        Label outcome = new Label(status.name());
        outcome.getStyleClass().add("supervisor-outcome-pill");
        if (status == RequestStatus.REJECTED) {
            outcome.getStyleClass().add("supervisor-outcome-rejected");
        } else if (status == RequestStatus.APPROVED || status == RequestStatus.COLLECTED) {
            outcome.getStyleClass().add("supervisor-outcome-blue");
        }
        return outcome;
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
