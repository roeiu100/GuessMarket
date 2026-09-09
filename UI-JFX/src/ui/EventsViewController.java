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

import java.util.List;

/**
 * Controller and view for the Events tab (Slide 1 of PPTX).
 * Features:
 * - Left Pane: Filter line (Method, Status, Commission) and Events Table
 * - Right Pane: Event details, Option 1 & Option 2 order books (or LMSR cards),
 *   Participations info, Trading section, and Transaction history.
 */
public class EventsViewController {

    private final MarketManager manager;
    private final GuessMarketApp app;

    private SplitPane rootSplitPane;

    // Filters
    private ToggleButton filterLmsr;
    private ToggleButton filterOrderBook;
    private ToggleButton filterNotStarted;
    private ToggleButton filterActive;
    private ToggleButton filterClosed;
    private ToggleButton filterOnPurchase;
    private ToggleButton filterOnClose;
    private TextField searchField;

    // Events Table
    private TableView<Event> eventsTable;
    private ObservableList<Event> eventsObservableList;

    // Details Pane (Right)
    private VBox detailsContent;
    private ScrollPane detailsScrollPane;

    // Order Book components
    private HBox orderBooksContainer;
    private OrderBookComponent bookComponentOpt0;
    private OrderBookComponent bookComponentOpt1;

    // LMSR cards container
    private HBox lmsrCardsContainer;
    private VBox lmsrCardOpt0;
    private VBox lmsrCardOpt1;
    private Label lmsrPrice0Label;
    private Label lmsrPrice1Label;
    private Label lmsrShares0Label;
    private Label lmsrShares1Label;

    // Participations Table
    private TableView<ParticipantRow> participationsTable;
    private ObservableList<ParticipantRow> participationsList;

    // Transaction History Table
    private TableView<Transaction> transactionsTable;
    private ObservableList<Transaction> transactionsList;

    // Currently selected event
    private Event currentSelectedEvent;

    public EventsViewController(MarketManager manager, GuessMarketApp app) {
        this.manager = manager;
        this.app = app;
        createUI();
    }

    public SplitPane getRoot() {
        return rootSplitPane;
    }

    private void createUI() {
        rootSplitPane = new SplitPane();

        // Left Pane
        VBox leftPane = createLeftPane();

        // Right Pane
        VBox rightPane = createRightPane();

        rootSplitPane.getItems().addAll(leftPane, rightPane);
        rootSplitPane.setDividerPositions(0.38);
    }

    // =========================================================================
    // LEFT PANE: Filter Line + Events Table
    // =========================================================================

    private VBox createLeftPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("left-pane");

        // Filter Bar (Slide 1: By event method | By status | By Commission method)
        VBox filterSection = createFilterSection();

        // Events Table (Slide 1: Events can be table | tiles | ...)
        eventsObservableList = FXCollections.observableArrayList();
        eventsTable = createEventsTable();
        VBox.setVgrow(eventsTable, Priority.ALWAYS);

        pane.getChildren().addAll(filterSection, eventsTable);
        return pane;
    }

    private VBox createFilterSection() {
        VBox container = new VBox(6);
        container.setPadding(new Insets(8));
        container.getStyleClass().add("filter-container");

        Label filterHeader = new Label("Filter Events");
        filterHeader.getStyleClass().add("filter-section-title");

        // Row 1: Method and Status
        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label methodLabel = new Label("Method:");
        methodLabel.getStyleClass().add("filter-sub-label");
        filterLmsr = new ToggleButton("LMSR");
        filterLmsr.setSelected(true);
        filterOrderBook = new ToggleButton("Order Book");
        filterOrderBook.setSelected(true);

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);

        Label statusLabel = new Label("Status:");
        statusLabel.getStyleClass().add("filter-sub-label");
        filterNotStarted = new ToggleButton("Not Started");
        filterNotStarted.setSelected(true);
        filterActive = new ToggleButton("Active");
        filterActive.setSelected(true);
        filterClosed = new ToggleButton("Closed");
        filterClosed.setSelected(true);

        row1.getChildren().addAll(methodLabel, filterLmsr, filterOrderBook, sep1,
                statusLabel, filterNotStarted, filterActive, filterClosed);

        // Row 2: Commission and Search
        HBox row2 = new HBox(6);
        row2.setAlignment(Pos.CENTER_LEFT);

        Label commLabel = new Label("Commission:");
        commLabel.getStyleClass().add("filter-sub-label");
        filterOnPurchase = new ToggleButton("On Purchase");
        filterOnPurchase.setSelected(true);
        filterOnClose = new ToggleButton("On Close");
        filterOnClose.setSelected(true);

        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);

        searchField = new TextField();
        searchField.setPromptText("Search title / ID...");
        searchField.setPrefWidth(140);
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());

        row2.getChildren().addAll(commLabel, filterOnPurchase, filterOnClose, sep2, searchField);

        ToggleButton[] allToggles = {
                filterLmsr, filterOrderBook,
                filterNotStarted, filterActive, filterClosed,
                filterOnPurchase, filterOnClose
        };
        for (ToggleButton tb : allToggles) {
            tb.getStyleClass().add("filter-toggle-button");
            tb.setOnAction(e -> applyFilters());
        }

        container.getChildren().addAll(filterHeader, row1, row2);
        return container;
    }

    @SuppressWarnings("unchecked")
    private TableView<Event> createEventsTable() {
        TableView<Event> table = new TableView<>(eventsObservableList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No events to display. Load an XML file first."));

        TableColumn<Event, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(40);
        idCol.setMaxWidth(60);
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getId())));
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Event, String> titleCol = new TableColumn<>("Title");
        titleCol.setPrefWidth(160);
        titleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));

        TableColumn<Event, String> methodCol = new TableColumn<>("Method");
        methodCol.setPrefWidth(95);
        methodCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTradingMethod().name()));
        methodCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Event, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(90);
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(formatStatus(cd.getValue().getStatus())));
        statusCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Event, String> commCol = new TableColumn<>("Commission");
        commCol.setPrefWidth(110);
        commCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("%d%% (%s)", cd.getValue().getCommission(), cd.getValue().getCommissionType())));
        commCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Event, String> balCol = new TableColumn<>("Account");
        balCol.setPrefWidth(85);
        balCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("$%.2f", cd.getValue().getAccountBalance())));
        balCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        table.getColumns().addAll(idCol, titleCol, methodCol, statusCol, commCol, balCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            currentSelectedEvent = newVal;
            showEventDetails(newVal);
        });

        return table;
    }

    private String formatStatus(EventStatus status) {
        if (status == null) return "Unknown";
        switch (status) {
            case NOT_STARTED: return "Not Started";
            case ACTIVE: return "Active";
            case CLOSED: return "Closed";
            default: return status.name();
        }
    }

    public void applyFilters() {
        if (!manager.isLoaded()) {
            eventsObservableList.clear();
            return;
        }

        String search = searchField.getText().trim().toLowerCase();
        eventsObservableList.clear();

        for (Event e : manager.getEvents()) {
            boolean methodMatch = (filterLmsr.isSelected() && e.isLMSR()) ||
                    (filterOrderBook.isSelected() && e.isOrderBook());

            boolean statusMatch = (filterNotStarted.isSelected() && e.getStatus() == EventStatus.NOT_STARTED) ||
                    (filterActive.isSelected() && e.getStatus() == EventStatus.ACTIVE) ||
                    (filterClosed.isSelected() && e.getStatus() == EventStatus.CLOSED);

            boolean commMatch = (filterOnPurchase.isSelected() && "on-purchase".equals(e.getCommissionType())) ||
                    (filterOnClose.isSelected() && "on-close".equals(e.getCommissionType()));

            boolean textMatch = search.isEmpty() ||
                    e.getName().toLowerCase().contains(search) ||
                    String.valueOf(e.getId()).equals(search);

            if (methodMatch && statusMatch && commMatch && textMatch) {
                eventsObservableList.add(e);
            }
        }

        // Re-select currently selected if still visible
        if (currentSelectedEvent != null && eventsObservableList.contains(currentSelectedEvent)) {
            eventsTable.getSelectionModel().select(currentSelectedEvent);
        } else if (!eventsObservableList.isEmpty()) {
            eventsTable.getSelectionModel().selectFirst();
        } else {
            showEventDetails(null);
        }
    }

    // =========================================================================
    // RIGHT PANE: Event Details & Trade (Slide 1)
    // =========================================================================

    private VBox createRightPane() {
        detailsContent = new VBox(15);
        detailsContent.setPadding(new Insets(15));
        detailsContent.getStyleClass().add("detail-content-pane");

        detailsScrollPane = new ScrollPane(detailsContent);
        detailsScrollPane.setFitToWidth(true);
        detailsScrollPane.getStyleClass().add("details-scroll-pane");

        // Initial placeholder
        Label placeholder = new Label("Select an event from the list to view details and trade");
        placeholder.getStyleClass().add("placeholder-label");
        detailsContent.getChildren().add(placeholder);

        VBox rightPane = new VBox(detailsScrollPane);
        VBox.setVgrow(detailsScrollPane, Priority.ALWAYS);
        return rightPane;
    }

    private void showEventDetails(Event event) {
        detailsContent.getChildren().clear();
        if (event == null) {
            Label placeholder = new Label("Select an event from the list to view details and trade");
            placeholder.getStyleClass().add("placeholder-label");
            detailsContent.getChildren().add(placeholder);
            return;
        }

        // 1. Header Card (Title, description, badges)
        VBox headerCard = createEventHeaderCard(event);

        // 2. Market Maker Actions (if active user is MM)
        VBox mmCard = createMmActionsCard(event);

        // 3. Option 1 & Option 2 Order Books / Pricing Section (Side by side as in PPTX sketch!)
        VBox booksSection = createOrderBooksSection(event);

        // 4. Participations Information (Slide 1 bottom)
        VBox partSection = createParticipationsSection(event);

        // 5. Trade / Order Placement Section
        VBox tradeSection = createTradeSection(event);

        // 6. Transaction History
        VBox transSection = createTransactionsSection(event);

        detailsContent.getChildren().addAll(headerCard);
        if (mmCard != null) {
            detailsContent.getChildren().add(mmCard);
        }
        detailsContent.getChildren().addAll(booksSection, partSection, tradeSection, transSection);
    }

    private VBox createEventHeaderCard(Event event) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("event-header-card");

        // Row 1: Title and ID
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label idBadge = new Label("#" + event.getId());
        idBadge.getStyleClass().add("event-id-badge");

        Label titleLabel = new Label(event.getName());
        titleLabel.getStyleClass().add("event-detail-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(formatStatus(event.getStatus()));
        statusBadge.getStyleClass().addAll("status-badge", "status-" + event.getStatus().name().toLowerCase().replace("_", "-"));

        titleRow.getChildren().addAll(idBadge, titleLabel, spacer, statusBadge);

        // Description
        Label descLabel = new Label(event.getDescription());
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("event-detail-desc");

        // Key info grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(4);
        infoGrid.setPadding(new Insets(6, 0, 0, 0));

        infoGrid.addRow(0,
                createMetaLabel("Trading Method:"), createMetaValue(event.getTradingMethod().name()),
                createMetaLabel("Market Maker:"), createMetaValue(event.getMmUserName() != null ? event.getMmUserName() : "None")
        );
        infoGrid.addRow(1,
                createMetaLabel("Commission:"), createMetaValue(String.format("%d%% (%s)", event.getCommission(), event.getCommissionType())),
                createMetaLabel("Contract Account:"), createMetaValue(String.format("$%.2f", event.getAccountBalance()))
        );
        infoGrid.addRow(2,
                createMetaLabel("Commission Collected:"), createMetaValue(String.format("$%.2f", event.getTotalCommissionCollected()))
        );

        if (event.getStatus() == EventStatus.CLOSED && event.getWinningOption() != null) {
            Label winLabel = new Label("WINNING OPTION: " + event.getWinningOption());
            winLabel.getStyleClass().add("winning-announcement");
            card.getChildren().addAll(titleRow, descLabel, infoGrid, winLabel);
        } else {
            card.getChildren().addAll(titleRow, descLabel, infoGrid);
        }

        return card;
    }

    private Label createMetaLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("meta-title");
        return l;
    }

    private Label createMetaValue(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("meta-value");
        return l;
    }

    private VBox createMmActionsCard(Event event) {
        User activeUser = app.getActiveUser();
        if (activeUser == null || !activeUser.isMmFor(event.getId())) {
            return null;
        }

        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("mm-actions-card");

        Label mmTitle = new Label("Market Maker Management (" + activeUser.getName() + ")");
        mmTitle.getStyleClass().add("mm-actions-title");

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        if (event.getStatus() == EventStatus.NOT_STARTED) {
            double cost = 0;
            if (event.isLMSR()) {
                cost = LmsrCalculator.calculatePool(0, 0, event.getB());
            } else if (event.getObConfig() != null) {
                cost = event.getObConfig().getInitial() * event.getObConfig().getD();
            }

            Label costNotice = new Label(String.format("Activation funding required: $%.2f (Your balance: $%.2f)",
                    cost, activeUser.getCash()));
            costNotice.getStyleClass().add("activation-notice");

            Button activateBtn = new Button("Activate Event");
            activateBtn.getStyleClass().add("btn-activate");
            activateBtn.setOnAction(e -> {
                String err = manager.activateEvent(event, activeUser);
                if (err != null) {
                    app.showNotification("Activation Error", err, Alert.AlertType.ERROR);
                } else {
                    app.showNotification("Success", "Event successfully activated!", Alert.AlertType.INFORMATION);
                    app.refreshAll();
                }
            });

            buttonRow.getChildren().addAll(activateBtn, costNotice);
        } else if (event.getStatus() == EventStatus.ACTIVE) {
            Button closeBtn = new Button("Close & Settle Event");
            closeBtn.getStyleClass().add("btn-close-event");
            closeBtn.setOnAction(e -> showCloseEventDialog(event));

            Label closeNotice = new Label("Closing the event declares the winning option and distributes payouts.");
            closeNotice.getStyleClass().add("activation-notice");

            buttonRow.getChildren().addAll(closeBtn, closeNotice);
        } else {
            Label closedNotice = new Label("Event is closed and resolved.");
            closedNotice.getStyleClass().add("activation-notice");
            buttonRow.getChildren().add(closedNotice);
        }

        card.getChildren().addAll(mmTitle, buttonRow);
        return card;
    }

    private void showCloseEventDialog(Event event) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(event.getOptions().get(0), event.getOptions());
        dialog.setTitle("Close & Settle Event");
        dialog.setHeaderText("Select the winning outcome for '" + event.getName() + "'");
        dialog.setContentText("Winning Option:");

        dialog.showAndWait().ifPresent(winner -> {
            int winIdx = event.getOptions().indexOf(winner);
            String err = manager.closeEvent(event, app.getActiveUser(), winIdx);
            if (err != null) {
                app.showNotification("Closing Error", err, Alert.AlertType.ERROR);
            } else {
                app.showNotification("Event Closed", "Winner declared: " + winner + "\nPayouts settled successfully!", Alert.AlertType.INFORMATION);
                app.refreshAll();
            }
        });
    }

    // =========================================================================
    // ORDER BOOKS SECTION (Slide 1: Option 1 order book | Option 2 order book)
    // =========================================================================

    private VBox createOrderBooksSection(Event event) {
        VBox section = new VBox(8);
        section.getStyleClass().add("order-books-section");

        Label sectionTitle = new Label(event.isOrderBook() ? "Order Books by Option" : "LMSR Option Pricing");
        sectionTitle.getStyleClass().add("section-heading");
        section.getChildren().add(sectionTitle);

        if (event.isOrderBook()) {
            // Side-by-side order books
            HBox booksRow = new HBox(12);
            booksRow.setAlignment(Pos.TOP_CENTER);

            bookComponentOpt0 = new OrderBookComponent();
            bookComponentOpt1 = new OrderBookComponent();

            HBox.setHgrow(bookComponentOpt0, Priority.ALWAYS);
            HBox.setHgrow(bookComponentOpt1, Priority.ALWAYS);

            bookComponentOpt0.updateData(event, 0);
            bookComponentOpt1.updateData(event, 1);

            booksRow.getChildren().addAll(bookComponentOpt0, bookComponentOpt1);
            section.getChildren().add(booksRow);
        } else {
            // LMSR Side-by-side Cards
            HBox lmsrRow = new HBox(15);
            lmsrRow.setAlignment(Pos.CENTER);

            VBox card0 = createLmsrPriceCard(event, 0);
            VBox card1 = createLmsrPriceCard(event, 1);

            HBox.setHgrow(card0, Priority.ALWAYS);
            HBox.setHgrow(card1, Priority.ALWAYS);

            lmsrRow.getChildren().addAll(card0, card1);
            section.getChildren().add(lmsrRow);
        }

        return section;
    }

    private VBox createLmsrPriceCard(Event event, int optionIndex) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("lmsr-price-card");
        card.setAlignment(Pos.CENTER);

        String optionName = event.getOptions().get(optionIndex);
        Label optTitle = new Label("Option " + (optionIndex + 1) + ": " + optionName);
        optTitle.getStyleClass().add("lmsr-card-title");

        double price = 0.5;
        if (event.getB() > 0) {
            price = (optionIndex == 0)
                    ? LmsrCalculator.calculatePrice(event.getQYes(), event.getQNo(), event.getB())
                    : LmsrCalculator.calculatePrice(event.getQNo(), event.getQYes(), event.getB());
        }

        Label priceLabel = new Label(String.format("$%.2f", price));
        priceLabel.getStyleClass().add("lmsr-price-value");

        int shares = (optionIndex == 0) ? event.getQYes() : event.getQNo();
        Label sharesLabel = new Label(String.format("Shares Purchased: %d", shares));
        sharesLabel.getStyleClass().add("lmsr-shares-value");

        Label probLabel = new Label(String.format("Implied Probability: %.1f%%", price * 100));
        probLabel.getStyleClass().add("lmsr-prob-value");

        card.getChildren().addAll(optTitle, priceLabel, sharesLabel, probLabel);
        return card;
    }

    // =========================================================================
    // PARTICIPATIONS INFORMATION (Slide 1)
    // =========================================================================

    private VBox createParticipationsSection(Event event) {
        VBox section = new VBox(6);
        section.getStyleClass().add("participations-section");

        Label heading = new Label("Participations Information");
        heading.getStyleClass().add("section-heading");

        participationsList = FXCollections.observableArrayList();
        participationsTable = createParticipationsTable(event);
        participationsTable.setPrefHeight(130);

        loadParticipationsData(event);

        section.getChildren().addAll(heading, participationsTable);
        return section;
    }

    @SuppressWarnings("unchecked")
    private TableView<ParticipantRow> createParticipationsTable(Event event) {
        TableView<ParticipantRow> table = new TableView<>(participationsList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No participants in this event yet."));

        TableColumn<ParticipantRow, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().userName));

        String opt0 = event.getOptions().size() > 0 ? event.getOptions().get(0) : "Option 1";
        String opt1 = event.getOptions().size() > 1 ? event.getOptions().get(1) : "Option 2";

        TableColumn<ParticipantRow, String> s0Col = new TableColumn<>(opt0 + " Shares");
        s0Col.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().shares0)));
        s0Col.setStyle("-fx-alignment: CENTER;");

        TableColumn<ParticipantRow, String> s1Col = new TableColumn<>(opt1 + " Shares");
        s1Col.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().shares1)));
        s1Col.setStyle("-fx-alignment: CENTER;");

        TableColumn<ParticipantRow, String> spentCol = new TableColumn<>("Total Spent");
        spentCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("$%.2f", cd.getValue().totalSpent)));
        spentCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<ParticipantRow, String> commCol = new TableColumn<>("Commission Paid");
        commCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("$%.2f", cd.getValue().commPaid)));
        commCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<ParticipantRow, String> plCol = new TableColumn<>("P/L Status");
        plCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().plStatus));
        plCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        table.getColumns().addAll(userCol, s0Col, s1Col, spentCol, commCol, plCol);
        return table;
    }

    private void loadParticipationsData(Event event) {
        participationsList.clear();
        if (manager.getUsers() == null) return;

        for (User u : manager.getUsers()) {
            UserEventData data = u.getEventData(event.getId());
            int s0 = data.getShares(0);
            int s1 = data.getShares(1);
            boolean hasOrders = false;

            if (event.isOrderBook()) {
                for (Order o : event.getBids(0)) if (o.getUserName().equalsIgnoreCase(u.getName())) hasOrders = true;
                for (Order o : event.getAsks(0)) if (o.getUserName().equalsIgnoreCase(u.getName())) hasOrders = true;
                for (Order o : event.getBids(1)) if (o.getUserName().equalsIgnoreCase(u.getName())) hasOrders = true;
                for (Order o : event.getAsks(1)) if (o.getUserName().equalsIgnoreCase(u.getName())) hasOrders = true;
            }

            if (s0 > 0 || s1 > 0 || !data.getTransactions().isEmpty() || hasOrders || u.isMmFor(event.getId())) {
                double spent = data.getTotalSpentOnOption(0) + data.getTotalSpentOnOption(1);
                double comm = data.getTotalCommissionPaid();
                String pl = "Open";

                if (event.getStatus() == EventStatus.CLOSED && event.getWinningOption() != null) {
                    int winIdx = event.getOptions().indexOf(event.getWinningOption());
                    int winShares = data.getShares(winIdx);
                    double d = event.isOrderBook() && event.getObConfig() != null ? event.getObConfig().getD() : 1.0;
                    double payout = winShares * d;
                    double netProfit = payout - spent - comm;
                    pl = String.format("%s$%.2f", netProfit >= 0 ? "+$" : "-$", Math.abs(netProfit));
                }

                String roleTag = u.isMmFor(event.getId()) ? " (MM)" : "";
                participationsList.add(new ParticipantRow(u.getName() + roleTag, s0, s1, spent, comm, pl));
            }
        }
    }

    // =========================================================================
    // TRADE / ORDER PLACEMENT SECTION
    // =========================================================================

    private VBox createTradeSection(Event event) {
        VBox section = new VBox(8);
        section.getStyleClass().add("trade-section-card");

        Label heading = new Label("Trade / Place Order");
        heading.getStyleClass().add("section-heading");

        User activeUser = app.getActiveUser();
        if (event.getStatus() != EventStatus.ACTIVE) {
            Label notice = new Label("Trading is not available. Event is currently " + formatStatus(event.getStatus()) + ".");
            notice.getStyleClass().add("trade-disabled-notice");
            section.getChildren().addAll(heading, notice);
            return section;
        }

        if (activeUser == null) {
            Label notice = new Label("Please select a user in the 'Act as' dropdown at top to trade.");
            notice.getStyleClass().add("trade-disabled-notice");
            section.getChildren().addAll(heading, notice);
            return section;
        }

        if (activeUser.isBlocked()) {
            Label notice = new Label("User '" + activeUser.getName() + "' is BLOCKED and cannot trade.");
            notice.getStyleClass().add("trade-blocked-notice");
            section.getChildren().addAll(heading, notice);
            return section;
        }

        HBox tradeControls = new HBox(12);
        tradeControls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> optionCombo = new ComboBox<>();
        for (int i = 0; i < event.getOptions().size(); i++) {
            optionCombo.getItems().add(String.format("Option %d: %s", (i + 1), event.getOptions().get(i)));
        }
        optionCombo.getSelectionModel().selectFirst();

        Spinner<Integer> qtySpinner = new Spinner<>(1, 100000, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(90);

        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("trade-feedback-label");

        if (event.isLMSR()) {
            // LMSR Trading
            Label previewLabel = new Label();
            previewLabel.getStyleClass().add("trade-cost-preview");

            Runnable updatePreview = () -> {
                int optIdx = optionCombo.getSelectionModel().getSelectedIndex();
                int qty = qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
                double costBefore = LmsrCalculator.calculatePool(event.getQYes(), event.getQNo(), event.getB());
                int newQ0 = event.getQYes() + (optIdx == 0 ? qty : 0);
                int newQ1 = event.getQNo() + (optIdx == 1 ? qty : 0);
                double costAfter = LmsrCalculator.calculatePool(newQ0, newQ1, event.getB());
                double base = costAfter - costBefore;
                double comm = "on-purchase".equals(event.getCommissionType()) ? base * (event.getCommission() / 100.0) : 0;
                previewLabel.setText(String.format("Est. Cost: $%.2f (Base: $%.2f, Comm: $%.2f)", base + comm, base, comm));
            };

            optionCombo.setOnAction(e -> updatePreview.run());
            qtySpinner.valueProperty().addListener((obs, ov, nv) -> updatePreview.run());
            updatePreview.run();

            Button buyBtn = new Button("Buy Shares");
            buyBtn.getStyleClass().add("btn-trade-buy");
            buyBtn.setOnAction(e -> {
                int optIdx = optionCombo.getSelectionModel().getSelectedIndex();
                int qty = qtySpinner.getValue();
                TradeResult res = manager.buySharesLMSR(event, activeUser, optIdx, qty);
                if (res.isSuccess()) {
                    feedbackLabel.setText("Success: " + res.getMessage());
                    feedbackLabel.setStyle("-fx-text-fill: #2e7d32;");
                    app.refreshAll();
                } else {
                    feedbackLabel.setText("Error: " + res.getMessage());
                    feedbackLabel.setStyle("-fx-text-fill: #c62828;");
                }
            });

            tradeControls.getChildren().addAll(
                    new Label("Option:"), optionCombo,
                    new Label("Quantity:"), qtySpinner,
                    buyBtn, previewLabel
            );
        } else {
            // Order Book Trading
            ComboBox<OrderType> typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll(OrderType.BUY, OrderType.SELL);
            typeCombo.getSelectionModel().selectFirst();

            double maxPrice = (event.getObConfig() != null ? event.getObConfig().getD() : 1.0) - 0.01;
            TextField priceField = new TextField("0.50");
            priceField.setPrefWidth(75);

            Button placeOrderBtn = new Button("Place Order");
            placeOrderBtn.getStyleClass().add("btn-trade-place");

            placeOrderBtn.setOnAction(e -> {
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

                    TradeResult res = manager.placeOrder(event, activeUser, optIdx, ot, qty, price);
                    if (res.isSuccess()) {
                        feedbackLabel.setText("Order Processed: " + res.getMessage());
                        feedbackLabel.setStyle("-fx-text-fill: #2e7d32;");
                        app.refreshAll();
                    } else {
                        feedbackLabel.setText("Order Rejected: " + res.getMessage());
                        feedbackLabel.setStyle("-fx-text-fill: #c62828;");
                    }
                } catch (NumberFormatException ex) {
                    feedbackLabel.setText("Invalid price format. Enter a number e.g. 0.50");
                    feedbackLabel.setStyle("-fx-text-fill: #c62828;");
                }
            });

            tradeControls.getChildren().addAll(
                    new Label("Option:"), optionCombo,
                    new Label("Type:"), typeCombo,
                    new Label("Qty:"), qtySpinner,
                    new Label("Price:"), priceField,
                    placeOrderBtn
            );
        }

        section.getChildren().addAll(heading, tradeControls, feedbackLabel);
        return section;
    }

    // =========================================================================
    // TRANSACTION HISTORY
    // =========================================================================

    private VBox createTransactionsSection(Event event) {
        VBox section = new VBox(6);
        section.getStyleClass().add("transactions-section");

        Label heading = new Label("Transaction History (Newest First)");
        heading.getStyleClass().add("section-heading");

        transactionsList = FXCollections.observableArrayList();
        transactionsTable = createTransactionsTable();
        transactionsTable.setPrefHeight(140);

        if (event.getTransactions() != null) {
            transactionsList.addAll(event.getTransactions());
        }

        section.getChildren().addAll(heading, transactionsTable);
        return section;
    }

    @SuppressWarnings("unchecked")
    private TableView<Transaction> createTransactionsTable() {
        TableView<Transaction> table = new TableView<>(transactionsList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No transactions executed yet."));

        TableColumn<Transaction, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getUserName() != null ? cd.getValue().getUserName() : "System/Initial"));

        TableColumn<Transaction, String> optCol = new TableColumn<>("Option");
        optCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOptionName()));

        TableColumn<Transaction, String> qtyCol = new TableColumn<>("Shares");
        qtyCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getQuantity())));
        qtyCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Transaction, String> priceCol = new TableColumn<>("Total Paid");
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("$%.2f", cd.getValue().getPricePaid())));
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        TableColumn<Transaction, String> commCol = new TableColumn<>("Commission");
        commCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("$%.2f", cd.getValue().getCommissionPaid())));
        commCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(userCol, optCol, qtyCol, priceCol, commCol);
        return table;
    }

    public void refresh() {
        applyFilters();
        if (currentSelectedEvent != null) {
            // Re-fetch latest instance of event
            Event updated = manager.getEventById(currentSelectedEvent.getId());
            if (updated != null) {
                currentSelectedEvent = updated;
                showEventDetails(updated);
            }
        }
    }

    public void selectEventById(int eventId) {
        if (eventsObservableList == null) return;
        for (Event e : eventsObservableList) {
            if (e.getId() == eventId) {
                eventsTable.getSelectionModel().select(e);
                eventsTable.scrollTo(e);
                break;
            }
        }
    }

    // Helper data model for participations table
    public static class ParticipantRow {
        public final String userName;
        public final int shares0;
        public final int shares1;
        public final double totalSpent;
        public final double commPaid;
        public final String plStatus;

        public ParticipantRow(String userName, int shares0, int shares1, double totalSpent, double commPaid, String plStatus) {
            this.userName = userName;
            this.shares0 = shares0;
            this.shares1 = shares1;
            this.totalSpent = totalSpent;
            this.commPaid = commPaid;
            this.plStatus = plStatus;
        }
    }
}
