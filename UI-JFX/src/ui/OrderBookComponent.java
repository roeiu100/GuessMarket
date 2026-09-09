package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import models.Event;
import models.Order;

import java.util.List;

/**
 * Visual component representing a single option's Order Book.
 * Displays statistics (LAST, BID, ASK, MID, SPREAD) along with
 * Bids and Asks tables.
 */
public class OrderBookComponent extends VBox {

    private final Label titleLabel;
    private final Label lastLabel;
    private final Label bidLabel;
    private final Label askLabel;
    private final Label midLabel;
    private final Label spreadLabel;

    private final TableView<Order> bidsTable;
    private final TableView<Order> asksTable;

    public OrderBookComponent() {
        setSpacing(8);
        setPadding(new Insets(10));
        getStyleClass().add("order-book-card");

        // Title
        titleLabel = new Label("Order Book");
        titleLabel.getStyleClass().add("order-book-title");

        // Stats grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(4);
        statsGrid.getStyleClass().add("stats-grid");
        statsGrid.setAlignment(Pos.CENTER_LEFT);

        lastLabel = createStatValue();
        bidLabel = createStatValue();
        askLabel = createStatValue();
        midLabel = createStatValue();
        spreadLabel = createStatValue();

        statsGrid.addRow(0, createStatTitle("LAST:"), lastLabel, createStatTitle("MID:"), midLabel);
        statsGrid.addRow(1, createStatTitle("BID:"), bidLabel, createStatTitle("SPREAD:"), spreadLabel);
        statsGrid.addRow(2, createStatTitle("ASK:"), askLabel);

        // Bids Table
        Label bidsHeader = new Label("Bids (Buy Orders)");
        bidsHeader.getStyleClass().add("order-table-header-buy");
        bidsTable = createTable();
        bidsTable.getStyleClass().add("bids-table");

        // Asks Table
        Label asksHeader = new Label("Asks (Sell Orders)");
        asksHeader.getStyleClass().add("order-table-header-sell");
        asksTable = createTable();
        asksTable.getStyleClass().add("asks-table");

        VBox.setVgrow(bidsTable, Priority.ALWAYS);
        VBox.setVgrow(asksTable, Priority.ALWAYS);

        getChildren().addAll(
                titleLabel,
                statsGrid,
                bidsHeader,
                bidsTable,
                asksHeader,
                asksTable
        );
    }

    private Label createStatTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("stat-title");
        return l;
    }

    private Label createStatValue() {
        Label l = new Label("—");
        l.getStyleClass().add("stat-value");
        return l;
    }

    @SuppressWarnings("unchecked")
    private TableView<Order> createTable() {
        TableView<Order> table = new TableView<>();
        table.setPrefHeight(130);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No resting orders"));

        TableColumn<Order, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUserName()));

        TableColumn<Order, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.valueOf(cd.getValue().getRemainingQuantity())));
        qtyCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Order, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(
                String.format("$%.2f", cd.getValue().getPricePerShare())));
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        table.getColumns().addAll(userCol, qtyCol, priceCol);
        return table;
    }

    /**
     * Updates the component with current event data for the given option.
     */
    public void updateData(Event event, int optionIndex) {
        if (event == null || optionIndex < 0 || optionIndex >= event.getOptions().size()) {
            setVisible(false);
            return;
        }

        setVisible(true);
        String optionName = event.getOptions().get(optionIndex);
        titleLabel.setText(String.format("Option %d: %s", (optionIndex + 1), optionName));

        List<Order> bids = event.getBids(optionIndex);
        List<Order> asks = event.getAsks(optionIndex);
        Double last = event.getLastTradePrice(optionIndex);

        Double bestBid = (bids != null && !bids.isEmpty()) ? bids.get(0).getPricePerShare() : null;
        Double bestAsk = (asks != null && !asks.isEmpty()) ? asks.get(0).getPricePerShare() : null;
        Double mid = (bestBid != null && bestAsk != null) ? (bestBid + bestAsk) / 2.0 : null;
        Double spread = (bestBid != null && bestAsk != null) ? (bestAsk - bestBid) : null;

        lastLabel.setText(last != null ? String.format("$%.2f", last) : "—");
        bidLabel.setText(bestBid != null ? String.format("$%.2f", bestBid) : "—");
        askLabel.setText(bestAsk != null ? String.format("$%.2f", bestAsk) : "—");
        midLabel.setText(mid != null ? String.format("$%.2f", mid) : "—");
        spreadLabel.setText(spread != null ? String.format("$%.2f", spread) : "—");

        bidsTable.setItems(FXCollections.observableArrayList(bids != null ? bids : List.of()));
        asksTable.setItems(FXCollections.observableArrayList(asks != null ? asks : List.of()));
    }
}
