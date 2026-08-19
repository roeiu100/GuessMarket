package models;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private int id;
    private String name;
    private String description;
    private int commission;
    private String commissionType;
    private List<String> options;
    private int b;
    
    private boolean isActive = true;
    private int qYes = 0;
    private int qNo = 0;
    private double totalCommissionCollected = 0.0;
    
    private List<Transaction> transactions = new ArrayList<>();
    private String winningOption = null;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCommission() { return commission; }
    public void setCommission(int commission) { this.commission = commission; }

    public String getCommissionType() { return commissionType; }
    public void setCommissionType(String commissionType) { this.commissionType = commissionType; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public int getB() { return b; }
    public void setB(int b) { this.b = b; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getQYes() { return qYes; }
    public void setQYes(int qYes) { this.qYes = qYes; }

    public int getQNo() { return qNo; }
    public void setQNo(int qNo) { this.qNo = qNo; }

    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public void setTotalCommissionCollected(double totalCommissionCollected) { this.totalCommissionCollected = totalCommissionCollected; }

    public List<Transaction> getTransactions() { return transactions; }
    public void addTransaction(Transaction t) { this.transactions.add(0, t); }

    public String getWinningOption() { return winningOption; }
    public void setWinningOption(String winningOption) { this.winningOption = winningOption; }
}