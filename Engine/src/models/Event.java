package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single event (market) in the Guess Market system.
 * Supports both LMSR and Order Book trading methods.
 */
public class Event {
    private int id;
    private String name;
    private String description;
    private int commission;
    private String commissionType;      // "on-purchase" or "on-close"
    private List<String> options;
    private TradingMethod tradingMethod;
    private EventStatus status;

    // LMSR-specific fields
    private int b;
    private int qYes = 0;
    private int qNo = 0;

    // Order Book-specific fields
    private OrderBookConfig obConfig;
    private List<Order> yesBids;        // Buy orders for option 0, sorted desc by price
    private List<Order> yesAsks;        // Sell orders for option 0, sorted asc by price
    private List<Order> noBids;         // Buy orders for option 1, sorted desc by price
    private List<Order> noAsks;         // Sell orders for option 1, sorted asc by price
    private Double lastYesTradePrice;
    private Double lastNoTradePrice;

    // Shared fields
    private double accountBalance = 0.0;          // The event's contract account
    private double totalCommissionCollected = 0.0;
    private List<Transaction> transactions = new ArrayList<>();
    private String winningOption = null;
    private String mmUserName;                     // The Market Maker assigned to this event

    public Event() {
        this.status = EventStatus.NOT_STARTED;
        this.tradingMethod = TradingMethod.LMSR; // default
        this.yesBids = new ArrayList<>();
        this.yesAsks = new ArrayList<>();
        this.noBids = new ArrayList<>();
        this.noAsks = new ArrayList<>();
    }

    // --- ID ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // --- Name ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // --- Description ---
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // --- Commission ---
    public int getCommission() { return commission; }
    public void setCommission(int commission) { this.commission = commission; }

    public String getCommissionType() { return commissionType; }
    public void setCommissionType(String commissionType) { this.commissionType = commissionType; }

    // --- Options ---
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    // --- Trading Method ---
    public TradingMethod getTradingMethod() { return tradingMethod; }
    public void setTradingMethod(TradingMethod tradingMethod) { this.tradingMethod = tradingMethod; }

    // --- Status ---
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    /**
     * Backward-compatible check: returns true if status is ACTIVE.
     */
    public boolean isActive() { return status == EventStatus.ACTIVE; }

    /**
     * Backward-compatible setter.
     */
    public void setActive(boolean active) {
        if (active) {
            this.status = EventStatus.ACTIVE;
        } else {
            this.status = EventStatus.CLOSED;
        }
    }

    // --- LMSR Fields ---
    public int getB() { return b; }
    public void setB(int b) { this.b = b; }

    public int getQYes() { return qYes; }
    public void setQYes(int qYes) { this.qYes = qYes; }

    public int getQNo() { return qNo; }
    public void setQNo(int qNo) { this.qNo = qNo; }

    // --- Order Book Config ---
    public OrderBookConfig getObConfig() { return obConfig; }
    public void setObConfig(OrderBookConfig obConfig) { this.obConfig = obConfig; }

    // --- Order Book: Bids and Asks ---
    public List<Order> getYesBids() { return yesBids; }
    public List<Order> getYesAsks() { return yesAsks; }
    public List<Order> getNoBids() { return noBids; }
    public List<Order> getNoAsks() { return noAsks; }

    /**
     * Returns the bids list for the given option index.
     */
    public List<Order> getBids(int optionIndex) {
        return optionIndex == 0 ? yesBids : noBids;
    }

    /**
     * Returns the asks list for the given option index.
     */
    public List<Order> getAsks(int optionIndex) {
        return optionIndex == 0 ? yesAsks : noAsks;
    }

    // --- Last Trade Prices ---
    public Double getLastTradePrice(int optionIndex) {
        return optionIndex == 0 ? lastYesTradePrice : lastNoTradePrice;
    }

    public void setLastTradePrice(int optionIndex, double price) {
        if (optionIndex == 0) {
            this.lastYesTradePrice = price;
        } else {
            this.lastNoTradePrice = price;
        }
    }

    // --- Account Balance ---
    public double getAccountBalance() { return accountBalance; }
    public void setAccountBalance(double accountBalance) { this.accountBalance = accountBalance; }

    public void addToAccount(double amount) { this.accountBalance += amount; }
    public void deductFromAccount(double amount) { this.accountBalance -= amount; }

    // --- Commission Collected ---
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public void setTotalCommissionCollected(double totalCommissionCollected) {
        this.totalCommissionCollected = totalCommissionCollected;
    }

    // --- Transactions ---
    public List<Transaction> getTransactions() { return transactions; }
    public void addTransaction(Transaction t) { this.transactions.add(0, t); }

    // --- Winning Option ---
    public String getWinningOption() { return winningOption; }
    public void setWinningOption(String winningOption) { this.winningOption = winningOption; }

    // --- Market Maker ---
    public String getMmUserName() { return mmUserName; }
    public void setMmUserName(String mmUserName) { this.mmUserName = mmUserName; }

    /**
     * Convenience: checks if this event uses LMSR trading.
     */
    public boolean isLMSR() {
        return tradingMethod == TradingMethod.LMSR;
    }

    /**
     * Convenience: checks if this event uses Order Book trading.
     */
    public boolean isOrderBook() {
        return tradingMethod == TradingMethod.ORDER_BOOK;
    }
}