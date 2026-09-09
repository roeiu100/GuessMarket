package models;

/**
 * Represents a single completed trade transaction.
 * Used for both LMSR and Order Book events.
 */
public class Transaction {
    private String optionName;
    private int optionIndex;
    private int quantity;
    private double pricePaid;
    private String userName;
    private double commissionPaid;

    /**
     * Legacy constructor for backward compatibility with EX1.
     */
    public Transaction(String optionName, int quantity, double pricePaid) {
        this(optionName, -1, quantity, pricePaid, null, 0.0);
    }

    /**
     * Full constructor for EX2.
     */
    public Transaction(String optionName, int optionIndex, int quantity,
                       double pricePaid, String userName, double commissionPaid) {
        this.optionName = optionName;
        this.optionIndex = optionIndex;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.userName = userName;
        this.commissionPaid = commissionPaid;
    }

    public String getOptionName() { return optionName; }
    public int getOptionIndex() { return optionIndex; }
    public int getQuantity() { return quantity; }
    public double getPricePaid() { return pricePaid; }
    public String getUserName() { return userName; }
    public double getCommissionPaid() { return commissionPaid; }
}