package ui;

import engine.MarketManager;
import engine.TradeResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lmsr.LmsrCalculator;
import models.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller and view for the Users tab (Slide 2 of PPTX).
 * Features:
 * - Left Pane: Users Table
 * - Right Pane: Single User Details
 *   - Account Balance prominently displayed in top right
 *   - Middle: Events Participation / Owner table
 *   - Bottom: Single event details and trade
 */
public class UsersViewController {

    private final MarketManager manager;
    private final GuessMarketApp app;

    private SplitPane rootSplitPane;

    // Users Table (Left Pane)
    private TableView<User> usersTable;
    private ObservableList<User> usersObservableList;

    // Right Pane
    private VBox detailsContent;
    private ScrollPane detailsScrollPane;

    // Currently selected user
    private User currentSelectedUser;

    // User's Events Table (Middle)
    private TableView<UserEventParticipationRow> userEventsTable;
    private ObservableList<UserEventParticipationRow> userEventsObservableList;

    // Single event details container (Bottom)
    private VBox singleEventDetailsContainer;
    private Event currentSelectedEventForUser;

    public UsersViewController(MarketManager manager, GuessMarketApp app) {
        this.manager = manager;
        this.app = app;
        createUI();
    }

    public SplitPane getRoot() {
        return rootSplitPane;
    }

    private void createUI() {
        rootSplitPane = new SplitPane();

        // Left Pane: Users Table
        VBox leftPane = createLeftPane();

        // Right Pane: Single User Details
        VBox rightPane = createRightPane();

        rootSplitPane.getItems().addAll(leftPane, rightPane);
        rootSplitPane.setDividerPositions(0.30);
    }

    // =========================================================================
    // LEFT PANE: Users Table (Slide 2)
    // =========================================================================

    private VBox createLeftPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("left-pane");

        Label title = new Label("Users");
        title.getStyleClass().add("filter-section-title");

        usersObservableList = FXCollections.observableArrayList();
        usersTable = createUsersTable();
        VBox.setVgrow(usersTable, Priority.ALWAYS);

        pane.getChildren().addAll(title, usersTable);
        return pane;
    }

    @SuppressWarnings("unchecked")
    private TableView<User> createUsersTable() {
        TableView<User> table = new TableView<>(usersObservableList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No users loaded. Load an XML file first."));

        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setPrefWidth(95);
        roleCol.setCellValueFactory(cd -> {
            User u = cd.getValue();
            if (u.isMarketMaker()) {
                List<Integer> mmIds = u.getMmEventIds();
                return new SimpleStringProperty("MM (" + mmIds.size() + " events)");
            }
            return new SimpleStringProperty("Participant");
        });
        roleCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<User, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setPrefWidth(90);
        balanceCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("$%.2f", cd.getValue().getCash())));
        balanceCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(75);
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().isBlocked() ? "BLOCKED" : "Active"));
        statusCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(nameCol, roleCol, balanceCol, statusCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            currentSelectedUser = newVal;
            showUserDetails(newVal);
        });

        return table;
    }

    // =========================================================================
    // RIGHT PANE: Single User Details (Slide 2)
    // =========================================================================

    private VBox createRightPane() {
        detailsContent = new VBox(15);
        detailsContent.setPadding(new Insets(15));
        detailsContent.getStyleClass().add("detail-content-pane");

        detailsScrollPane = new ScrollPane(detailsContent);
        detailsScrollPane.setFitToWidth(true);
        detailsScrollPane.getStyleClass().add("details-scroll-pane");

        // Initial placeholder
        Label placeholder = new Label("Select a user from the list to view details");
        placeholder.getStyleClass().add("placeholder-label");
        detailsContent.getChildren().add(placeholder);

        VBox rightPane = new VBox(detailsScrollPane);
        VBox.setVgrow(detailsScrollPane, Priority.ALWAYS);
        return rightPane;
    }

    private void showUserDetails(User user) {
        detailsContent.getChildren().clear();
        if (user == null) {
            Label placeholder = new Label("Select a user from the list to view details");
            placeholder.getStyleClass().add("placeholder-label");
            detailsContent.getChildren().add(placeholder);
            return;
        }

        // 1. Top Section: User Name, Roles, and Prominent "Account Balance" Card (Slide 2)
        HBox topSection = createUserHeaderWithBalance(user);

        // 2. Middle Section: Events Participation \ owner (Slide 2)
        VBox middleSection = createEventsParticipationSection(user);

        // 3. Bottom Section: Single event details and trade (Slide 2)
        singleEventDetailsContainer = new VBox(10);
        singleEventDetailsContainer.getStyleClass().add("single-event-container");
        updateSingleEventSection(user, null);

        detailsContent.getChildren().addAll(topSection, new Separator(), middleSection, new Separator(), singleEventDetailsContainer);
    }

    private HBox createUserHeaderWithBalance(User user) {
        HBox container = new HBox(20);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(10, 15, 10, 15));
        container.getStyleClass().add("user-header-card");

        // User info on left
        VBox infoBox = new VBox(6);
        Label userName = new Label(user.getName());
        userName.getStyleClass().add("user-detail-title");

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);

        if (user.isMarketMaker()) {
            Label mmBadge = new Label("Market Maker (" + user.getMmEventIds().size() + " events)");
            mmBadge.getStyleClass().add("badge-mm");
            badges.getChildren().add(mmBadge);
        } else {
            Label partBadge = new Label("Standard Participant");
            partBadge.getStyleClass().add("badge-participant");
            badges.getChildren().add(partBadge);
        }

        if (user.isBlocked()) {
            Label blockedBadge = new Label("BLOCKED (Negative Balance)");
            blockedBadge.getStyleClass().add("badge-blocked");
            badges.getChildren().add(blockedBadge);
        } else {
            Label activeBadge = new Label("Active Status");
            activeBadge.getStyleClass().add("badge-active");
            badges.getChildren().add(activeBadge);
        }

        infoBox.getChildren().addAll(userName, badges);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Prominent "Account Balance" Card on the right (as designated in Slide 2)
        VBox balanceCard = new VBox(2);
        balanceCard.setAlignment(Pos.CENTER_RIGHT);
        balanceCard.setPadding(new Insets(8, 16, 8, 16));
        balanceCard.getStyleClass().add("account-balance-card");

        Label balanceTitle = new Label("Account Balance");
        balanceTitle.getStyleClass().add("account-balance-card-title");

        Label balanceValue = new Label(String.format("$%.2f", user.getCash()));
        balanceValue.getStyleClass().add("account-balance-card-value");

        Label initialLabel = new Label(String.format("Initial: $%.2f", user.getInitialCash()));
        initialLabel.getStyleClass().add("account-balance-card-sub");

        balanceCard.getChildren().addAll(balanceTitle, balanceValue, initialLabel);

        container.getChildren().addAll(infoBox, spacer, balanceCard);
        return container;
    }

    // =========================================================================
    // MIDDLE SECTION: Events Participation \ owner (Slide 2)
    // =========================================================================

    private VBox createEventsParticipationSection(User user) {
        VBox section = new VBox(8);
        section.getStyleClass().add("user-events-section");

        Label heading = new Label("Events Participation \\ owner");
        heading.getStyleClass().add("section-heading");

        userEventsObservableList = FXCollections.observableArrayList();
        userEventsTable = createUserEventsTable(user);
        userEventsTable.setPrefHeight(150);

        loadUserEventsData(user);

        section.getChildren().addAll(heading, userEventsTable);
        return section;
    }

    @SuppressWarnings("unchecked")
    private TableView<UserEventParticipationRow> createUserEventsTable(User user) {
        TableView<UserEventParticipationRow> table = new TableView<>(userEventsObservableList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("User has no active participation or MM events."));

        TableColumn<UserEventParticipationRow, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(40);
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().eventId)));
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<UserEventParticipationRow, String> nameCol = new TableColumn<>("Event Name");
        nameCol.setPrefWidth(160);
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().eventName));

        TableColumn<UserEventParticipationRow, String> roleCol = new TableColumn<>("Role");
        roleCol.setPrefWidth(90);
        roleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().role));
        roleCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<UserEventParticipationRow, String> methodCol = new TableColumn<>("Method");
        methodCol.setPrefWidth(85);
        methodCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().method));
        methodCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<UserEventParticipationRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(85);
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status));
        statusCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<UserEventParticipationRow, String> holdingsCol = new TableColumn<>("Shares Held");
        holdingsCol.setPrefWidth(140);
        holdingsCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().holdingsText));
        holdingsCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<UserEventParticipationRow, String> spentCol = new TableColumn<>("Total Spent");
        spentCol.setPrefWidth(85);
        spentCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("$%.2f", cd.getValue().totalSpent)));
        spentCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<UserEventParticipationRow, String> commCol = new TableColumn<>("Commission Paid");
        commCol.setPrefWidth(95);
        commCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("$%.2f", cd.getValue().commissionPaid)));
        commCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(idCol, nameCol, roleCol, methodCol, statusCol, holdingsCol, spentCol, commCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Event e = manager.getEventById(newVal.eventId);
                currentSelectedEventForUser = e;
                updateSingleEventSection(user, e);
            } else {
                updateSingleEventSection(user, null);
            }
        });

        return table;
    }

    private void loadUserEventsData(User user) {
        userEventsObservableList.clear();
        if (manager.getEvents() == null) return;

        for (Event event : manager.getEvents()) {
            boolean isMM = user.isMmFor(event.getId());
            UserEventData data = user.getEventData(event.getId());
            int s0 = data.getShares(0);
            int s1 = data.getShares(1);
            boolean hasTrans = !data.getTransactions().isEmpty();

            boolean hasOrders = false;
            if (event.isOrderBook()) {
                for (Order o : event.getBids(0)) if (o.getUserName().equalsIgnoreCase(user.getName())) hasOrders = true;
                for (Order o : event.getAsks(0)) if (o.getUserName().equalsIgnoreCase(user.getName())) hasOrders = true;
                for (Order o : event.getBids(1)) if (o.getUserName().equalsIgnoreCase(user.getName())) hasOrders = true;
                for (Order o : event.getAsks(1)) if (o.getUserName().equalsIgnoreCase(user.getName())) hasOrders = true;
            }

            if (isMM || s0 > 0 || s1 > 0 || hasTrans || hasOrders) {
                String role = isMM ? "Market Maker" : "Participant";
                String opt0 = event.getOptions().size() > 0 ? event.getOptions().get(0) : "Opt1";
                String opt1 = event.getOptions().size() > 1 ? event.getOptions().get(1) : "Opt2";
                String holdings = String.format("%s: %d, %s: %d", opt0, s0, opt1, s1);
                double spent = data.getTotalSpentOnOption(0) + data.getTotalSpentOnOption(1);
                double comm = data.getTotalCommissionPaid();

                userEventsObservableList.add(new UserEventParticipationRow(
                        event.getId(), event.getName(), role,
                        event.getTradingMethod().name(), event.getStatus().name(),
                        holdings, spent, comm
                ));
            }
        }

        if (!userEventsObservableList.isEmpty()) {
            userEventsTable.getSelectionModel().selectFirst();
        }
    }

    // =========================================================================
    // BOTTOM SECTION: Single event details and trade (Slide 2)
    // =========================================================================

    private void updateSingleEventSection(User user, Event event) {
        singleEventDetailsContainer.getChildren().clear();

        Label heading = new Label("Single Event Details and Trade");
        heading.getStyleClass().add("section-heading");
        singleEventDetailsContainer.getChildren().add(heading);

        if (event == null) {
            Label placeholder = new Label("Select an event from the table above to view specific details and trade");
            placeholder.getStyleClass().add("placeholder-label");
            singleEventDetailsContainer.getChildren().add(placeholder);
            return;
        }

        UserEventData data = user.getEventData(event.getId());

        // Event overview card
        VBox eventCard = new VBox(8);
        eventCard.setPadding(new Insets(10));
        eventCard.getStyleClass().add("single-event-detail-card");

        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(event.getId() + ". " + event.getName());
        titleLabel.getStyleClass().add("event-detail-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label statusBadge = new Label(event.getStatus().name());
        statusBadge.getStyleClass().addAll("status-badge", "status-" + event.getStatus().name().toLowerCase().replace("_", "-"));
        row1.getChildren().addAll(titleLabel, sp, statusBadge);

        // Details breakdown
        VBox breakdownBox = new VBox(4);
        breakdownBox.setPadding(new Insets(4, 0, 4, 0));

        if (event.isLMSR()) {
            Label lmsrTitle = new Label("LMSR Holdings & Trade History:");
            lmsrTitle.getStyleClass().add("stat-title");
            breakdownBox.getChildren().add(lmsrTitle);

            String opt0 = event.getOptions().get(0);
            String opt1 = event.getOptions().get(1);
            breakdownBox.getChildren().add(new Label(String.format("Shares: %s = %d | %s = %d",
                    opt0, data.getShares(0), opt1, data.getShares(1))));
            breakdownBox.getChildren().add(new Label(String.format("Total Commission Paid: $%.2f",
                    data.getTotalCommissionPaid())));

            if (event.getStatus() == EventStatus.CLOSED && event.getWinningOption() != null) {
                int winIdx = event.getOptions().indexOf(event.getWinningOption());
                int winShares = data.getShares(winIdx);
                double payout = winShares * 1.0;
                double spent = data.getTotalSpentOnOption(0) + data.getTotalSpentOnOption(1);
                double profit = payout - spent - data.getTotalCommissionPaid();

                Label winLabel = new Label(String.format("Winner: %s | Payout: $%.2f | Net P/L: %s$%.2f",
                        event.getWinningOption(), payout, profit >= 0 ? "+$" : "-$", Math.abs(profit)));
                winLabel.getStyleClass().add(profit >= 0 ? "pl-profit" : "pl-loss");
                breakdownBox.getChildren().add(winLabel);
            }
        } else {
            Label obTitle = new Label("Order Book Holdings & Position:");
            obTitle.getStyleClass().add("stat-title");
            breakdownBox.getChildren().add(obTitle);

            for (int i = 0; i < event.getOptions().size(); i++) {
                String optName = event.getOptions().get(i);
                int shares = data.getShares(i);
                double spent = data.getTotalSpentOnOption(i);
                breakdownBox.getChildren().add(new Label(String.format("%s: %d shares (Total Spent: $%.2f)",
                        optName, shares, spent)));
            }
            breakdownBox.getChildren().add(new Label(String.format("Total Commission Paid: $%.2f",
                    data.getTotalCommissionPaid())));

            if (event.getStatus() == EventStatus.CLOSED && event.getWinningOption() != null) {
                int winIdx = event.getOptions().indexOf(event.getWinningOption());
                int winShares = data.getShares(winIdx);
                double d = event.getObConfig() != null ? event.getObConfig().getD() : 1.0;
                double payout = winShares * d;
                double spent = data.getTotalSpentOnOption(0) + data.getTotalSpentOnOption(1);
                double profit = payout - spent - data.getTotalCommissionPaid();

                Label winLabel = new Label(String.format("Winner: %s | Payout: $%.2f | Net P/L: %s$%.2f",
                        event.getWinningOption(), payout, profit >= 0 ? "+$" : "-$", Math.abs(profit)));
                winLabel.getStyleClass().add(profit >= 0 ? "pl-profit" : "pl-loss");
                breakdownBox.getChildren().add(winLabel);
            }
        }

        eventCard.getChildren().addAll(row1, breakdownBox);

        // Trade panel right from here for this user!
        VBox tradeSubSection = createTradeSubSection(user, event);

        singleEventDetailsContainer.getChildren().addAll(eventCard, tradeSubSection);
    }

    private VBox createTradeSubSection(User user, Event event) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("user-trade-subcard");

        Label tradeTitle = new Label("Direct Trade as " + user.getName());
        tradeTitle.getStyleClass().add("stat-title");

        if (event.getStatus() != EventStatus.ACTIVE) {
            Label disabled = new Label("Event is not currently active for trading (" + event.getStatus() + ").");
            disabled.getStyleClass().add("trade-disabled-notice");
            box.getChildren().addAll(tradeTitle, disabled);
            return box;
        }

        if (user.isBlocked()) {
            Label blocked = new Label("User is BLOCKED (negative balance). Trading disabled.");
            blocked.getStyleClass().add("trade-blocked-notice");
            box.getChildren().addAll(tradeTitle, blocked);
            return box;
        }

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> optionCombo = new ComboBox<>();
        for (int i = 0; i < event.getOptions().size(); i++) {
            optionCombo.getItems().add(String.format("Option %d: %s", (i + 1), event.getOptions().get(i)));
        }
        optionCombo.getSelectionModel().selectFirst();

        Spinner<Integer> qtySpinner = new Spinner<>(1, 100000, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(85);

        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        if (event.isLMSR()) {
            Button buyBtn = new Button("Buy Shares");
            buyBtn.getStyleClass().add("btn-trade-buy");

            buyBtn.setOnAction(e -> {
                int optIdx = optionCombo.getSelectionModel().getSelectedIndex();
                int qty = qtySpinner.getValue();
                TradeResult res = manager.buySharesLMSR(event, user, optIdx, qty);
                if (res.isSuccess()) {
                    feedbackLabel.setText("Success: " + res.getMessage());
                    feedbackLabel.setStyle("-fx-text-fill: #2e7d32;");
                    app.refreshAll();
                } else {
                    feedbackLabel.setText("Error: " + res.getMessage());
                    feedbackLabel.setStyle("-fx-text-fill: #c62828;");
                }
            });

            controls.getChildren().addAll(new Label("Option:"), optionCombo, new Label("Qty:"), qtySpinner, buyBtn);
        } else {
            ComboBox<OrderType> typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll(OrderType.BUY, OrderType.SELL);
            typeCombo.getSelectionModel().selectFirst();

            double maxPrice = (event.getObConfig() != null ? event.getObConfig().getD() : 1.0) - 0.01;
            TextField priceField = new TextField("0.50");
            priceField.setPrefWidth(75);

            Button placeBtn = new Button("Place Order");
            placeBtn.getStyleClass().add("btn-trade-place");

            placeBtn.setOnAction(e -> {
                try {
                    int optIdx = optionCombo.getSelectionModel().getSelectedIndex();
                    OrderType ot = typeCombo.getValue();
                    int qty = qtySpinner.getValue();
                    double price = Double.parseDouble(priceField.getText().trim());

                    if (price < 0.01 || price > maxPrice) {
                        feedbackLabel.setText(String.format("Price must be between $0.01 and $%.2f (max = d − 0.01)", maxPrice));
                        feedbackLabel.setStyle("-fx-text-fill: #c62828;");
                        return;
                    }

                    TradeResult res = manager.placeOrder(event, user, optIdx, ot, qty, price);
                    if (res.isSuccess()) {
                        feedbackLabel.setText("Order Success: " + res.getMessage());
                        feedbackLabel.setStyle("-fx-text-fill: #2e7d32;");
                        app.refreshAll();
                    } else {
                        feedbackLabel.setText("Order Rejected: " + res.getMessage());
                        feedbackLabel.setStyle("-fx-text-fill: #c62828;");
                    }
                } catch (NumberFormatException ex) {
                    feedbackLabel.setText("Invalid price format.");
                    feedbackLabel.setStyle("-fx-text-fill: #c62828;");
                }
            });

            controls.getChildren().addAll(
                    new Label("Option:"), optionCombo,
                    new Label("Type:"), typeCombo,
                    new Label("Qty:"), qtySpinner,
                    new Label("Price:"), priceField,
                    placeBtn
            );
        }

        box.getChildren().addAll(tradeTitle, controls, feedbackLabel);
        return box;
    }

    public void refresh() {
        usersObservableList.clear();
        if (manager.getUsers() != null) {
            usersObservableList.addAll(manager.getUsers());
        }

        if (currentSelectedUser != null) {
            User updated = manager.getUserByName(currentSelectedUser.getName());
            if (updated != null) {
                currentSelectedUser = updated;
                showUserDetails(updated);
            }
        } else if (!usersObservableList.isEmpty()) {
            usersTable.getSelectionModel().selectFirst();
        }
    }

    public void selectUserByName(String name) {
        if (usersObservableList == null) return;
        for (User u : usersObservableList) {
            if (u.getName().equalsIgnoreCase(name)) {
                usersTable.getSelectionModel().select(u);
                usersTable.scrollTo(u);
                break;
            }
        }
    }

    // Helper row model for the Middle Section
    public static class UserEventParticipationRow {
        public final int eventId;
        public final String eventName;
        public final String role;
        public final String method;
        public final String status;
        public final String holdingsText;
        public final double totalSpent;
        public final double commissionPaid;

        public UserEventParticipationRow(int eventId, String eventName, String role, String method,
                                         String status, String holdingsText, double totalSpent, double commissionPaid) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.role = role;
            this.method = method;
            this.status = status;
            this.holdingsText = holdingsText;
            this.totalSpent = totalSpent;
            this.commissionPaid = commissionPaid;
        }
    }
}
