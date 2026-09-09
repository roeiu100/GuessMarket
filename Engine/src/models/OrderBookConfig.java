package models;

/**
 * Configuration for an Order Book trading method event.
 * Parsed from the GM-order-book XML element.
 */
public class OrderBookConfig {
    private int initial;    // Initial share pairs the MM must purchase
    private int d;          // Denomination / base value per share pair
    private boolean allowMint; // Whether peer-to-peer minting is allowed

    public OrderBookConfig(int initial, int d, boolean allowMint) {
        this.initial = initial;
        this.d = d;
        this.allowMint = allowMint;
    }

    public int getInitial() { return initial; }
    public void setInitial(int initial) { this.initial = initial; }

    public int getD() { return d; }
    public void setD(int d) { this.d = d; }

    public boolean isAllowMint() { return allowMint; }
    public void setAllowMint(boolean allowMint) { this.allowMint = allowMint; }

    /**
     * Returns the maximum allowed price per share: d - 0.01
     */
    public double getMaxPrice() {
        return d - 0.01;
    }
}
