package models;

/**
 * Represents a single order in the Order Book.
 * An order is a request to buy or sell a certain quantity of shares
 * of a specific option at a given price per share.
 */
public class Order {
    private String userName;
    private int optionIndex;     // 0 = first option (YES), 1 = second option (NO)
    private OrderType orderType; // BUY or SELL
    private int quantity;
    private double pricePerShare;
    private int remainingQuantity;
    private long timestamp;      // For ordering within the same price level

    public Order(String userName, int optionIndex, OrderType orderType,
                 int quantity, double pricePerShare) {
        this.userName = userName;
        this.optionIndex = optionIndex;
        this.orderType = orderType;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
        this.remainingQuantity = quantity;
        this.timestamp = System.nanoTime();
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getOptionIndex() { return optionIndex; }
    public void setOptionIndex(int optionIndex) { this.optionIndex = optionIndex; }

    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPricePerShare() { return pricePerShare; }
    public void setPricePerShare(double pricePerShare) { this.pricePerShare = pricePerShare; }

    public int getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(int remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /**
     * Returns true if this order has been fully filled.
     */
    public boolean isFilled() {
        return remainingQuantity <= 0;
    }

    /**
     * Fills a portion of this order.
     * @param fillQty the number of shares to fill
     */
    public void fill(int fillQty) {
        this.remainingQuantity -= fillQty;
    }
}
