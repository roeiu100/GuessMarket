package engine;

/**
 * DTO that encapsulates the result of a trade operation.
 * Used to communicate trade outcomes from the Engine to the UI layer.
 */
public class TradeResult {
    private boolean success;
    private double baseCost;
    private double commission;
    private double totalPaid;
    private String message;

    private TradeResult(boolean success, double baseCost, double commission,
                        double totalPaid, String message) {
        this.success = success;
        this.baseCost = baseCost;
        this.commission = commission;
        this.totalPaid = totalPaid;
        this.message = message;
    }

    /**
     * Creates a successful trade result.
     */
    public static TradeResult success(double baseCost, double commission, double totalPaid, String message) {
        return new TradeResult(true, baseCost, commission, totalPaid, message);
    }

    /**
     * Creates a failed trade result with an error message.
     */
    public static TradeResult failure(String message) {
        return new TradeResult(false, 0, 0, 0, message);
    }

    public boolean isSuccess() { return success; }
    public double getBaseCost() { return baseCost; }
    public double getCommission() { return commission; }
    public double getTotalPaid() { return totalPaid; }
    public String getMessage() { return message; }
}
