package engine;

import lmsr.LmsrCalculator;
import models.Event;
import models.Transaction;
import xml.GuessMarketParser;
import java.util.List;

public class MarketManager {
    private List<Event> events;

    public void loadEvents(String filePath) throws Exception {
        this.events = GuessMarketParser.parseEvents(filePath);
    }

    public List<Event> getEvents() {
        return events;
    }

    public void displayEventStatus(Event e) {
        System.out.println("\n--- Event: " + e.getName() + " ---");
        String opt0 = e.getOptions().get(0);
        String opt1 = e.getOptions().get(1);
        
        // Calculate current prices
        double price0 = LmsrCalculator.calculatePrice(e.getQYes(), e.getQNo(), e.getB());
        double price1 = LmsrCalculator.calculatePrice(e.getQNo(), e.getQYes(), e.getB());
        
        System.out.printf("Option 1 (%s): Price = %.2f, Shares Sold = %d\n", opt0, price0, e.getQYes());
        System.out.printf("Option 2 (%s): Price = %.2f, Shares Sold = %d\n", opt1, price1, e.getQNo());
        System.out.printf("Total Commission Collected: %.2f\n", e.getTotalCommissionCollected());
        
        System.out.println("History (Newest to Oldest):");
        if (e.getTransactions().isEmpty()) {
            System.out.println("  No transactions yet.");
        } else {
            for (Transaction t : e.getTransactions()) {
                System.out.printf("  Bought %d shares of '%s' for %.2f\n", t.getQuantity(), t.getOptionName(), t.getPricePaid());
            }
        }
        
        if (!e.isActive()) {
            System.out.println("STATUS: CLOSED. Winning Option: " + e.getWinningOption());
        }
    }

    public void buyShares(Event e, int optionIndex, int quantity) {
        if (!e.isActive()) {
            System.out.println("Event is closed.");
            return;
        }

        String chosenOption = e.getOptions().get(optionIndex);
        double costBefore = LmsrCalculator.calculatePool(e.getQYes(), e.getQNo(), e.getB());
        
        // Simulate trade to get new cost
        int newQYes = e.getQYes() + (optionIndex == 0 ? quantity : 0);
        int newQNo = e.getQNo() + (optionIndex == 1 ? quantity : 0);
        double costAfter = LmsrCalculator.calculatePool(newQYes, newQNo, e.getB());
        
        double baseCost = costAfter - costBefore;
        double commission = 0;
        
        if (e.getCommissionType().equals("on-purchase")) {
            commission = baseCost * (e.getCommission() / 100.0);
            e.setTotalCommissionCollected(e.getTotalCommissionCollected() + commission);
        }
        
        double totalPaid = baseCost + commission;
        
        // Apply trade
        e.setQYes(newQYes);
        e.setQNo(newQNo);
        e.addTransaction(new Transaction(chosenOption, quantity, totalPaid));
        
        System.out.printf("Success! You paid %.2f (Base: %.2f, Commission: %.2f)\n", totalPaid, baseCost, commission);
    }

    public void closeEvent(Event e, int winningIndex) {
        if (!e.isActive()) {
            System.out.println("Event is already closed.");
            return;
        }
        
        e.setActive(false);
        String winner = e.getOptions().get(winningIndex);
        e.setWinningOption(winner);
        
        int winningShares = (winningIndex == 0) ? e.getQYes() : e.getQNo();
        double payout = winningShares * 1.0; // 1$ per winning share
        
        if (e.getCommissionType().equals("on-close")) {
            double commission = payout * (e.getCommission() / 100.0);
            e.setTotalCommissionCollected(e.getTotalCommissionCollected() + commission);
        }
        
        System.out.println("Event closed successfully. '" + winner + "' wins!");
    }
}