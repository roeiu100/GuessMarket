package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks a single user's participation data within a specific event.
 * Holds share counts per option, transaction history, and commission paid.
 */
public class UserEventData {
    private int[] sharesPerOption;       // shares[0] = first option, shares[1] = second option
    private double totalCommissionPaid;
    private List<Transaction> transactions;

    public UserEventData() {
        this.sharesPerOption = new int[]{0, 0};
        this.totalCommissionPaid = 0.0;
        this.transactions = new ArrayList<>();
    }

    public int getShares(int optionIndex) {
        return sharesPerOption[optionIndex];
    }

    public void addShares(int optionIndex, int quantity) {
        sharesPerOption[optionIndex] += quantity;
    }

    public void removeShares(int optionIndex, int quantity) {
        sharesPerOption[optionIndex] -= quantity;
    }

    public int[] getSharesPerOption() { return sharesPerOption; }

    public double getTotalCommissionPaid() { return totalCommissionPaid; }
    public void addCommissionPaid(double commission) {
        this.totalCommissionPaid += commission;
    }

    public List<Transaction> getTransactions() { return transactions; }

    /**
     * Adds a transaction at the beginning (newest first).
     */
    public void addTransaction(Transaction t) {
        this.transactions.add(0, t);
    }

    /**
     * Returns the total money spent acquiring shares of the given option.
     */
    public double getTotalSpentOnOption(int optionIndex) {
        double total = 0;
        for (Transaction t : transactions) {
            if (t.getOptionIndex() == optionIndex) {
                total += t.getPricePaid();
            }
        }
        return total;
    }
}
