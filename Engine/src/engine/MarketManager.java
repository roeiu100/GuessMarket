package engine;

import lmsr.LmsrCalculator;
import models.*;
import orderbook.OrderBookManager;
import xml.GuessMarketParser;
import xml.ParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Central engine for the Guess Market system.
 * Manages events, users, and all trading operations.
 * This class is UI-agnostic — it returns data objects instead of printing.
 */
public class MarketManager {
    private List<Event> events;
    private List<User> users;

    public MarketManager() {
        this.events = null;
        this.users = null;
    }

    // ========================== FILE LOADING ==========================

    /**
     * Loads events from an EX1-format XML file (no users).
     * Kept for backward compatibility with the console app.
     */
    public void loadEvents(String filePath) throws Exception {
        ParseResult result = GuessMarketParser.parseFile(filePath);
        this.events = result.getEvents();
        this.users = result.getUsers();
    }

    /**
     * Loads events and users from an EX2-format XML file.
     *
     * @param filePath path to the XML file
     * @return a descriptive message about the load result
     * @throws Exception if the file is invalid
     */
    public String loadFile(String filePath) throws Exception {
        ParseResult result = GuessMarketParser.parseFile(filePath);
        this.events = result.getEvents();
        this.users = result.getUsers();

        // Process LMSR subsidies are NOT done at load time in EX2.
        // They are done when the MM activates the event.

        StringBuilder msg = new StringBuilder();
        msg.append("File loaded successfully.\n");
        msg.append("Events: ").append(events.size()).append("\n");
        if (users != null && !users.isEmpty()) {
            msg.append("Users: ").append(users.size());
        }
        return msg.toString();
    }

    // ========================== GETTERS ==========================

    public List<Event> getEvents() { return events; }
    public List<User> getUsers() { return users; }

    public boolean isLoaded() {
        return events != null && !events.isEmpty();
    }

    /**
     * Finds an event by its ID.
     */
    public Event getEventById(int id) {
        if (events == null) return null;
        for (Event e : events) {
            if (e.getId() == id) return e;
        }
        return null;
    }

    /**
     * Finds a user by their name (case insensitive).
     */
    public User getUserByName(String name) {
        if (users == null) return null;
        for (User u : users) {
            if (u.getName().equalsIgnoreCase(name)) return u;
        }
        return null;
    }

    // ========================== EVENT ACTIVATION ==========================

    /**
     * Activates an event. Only the MM can do this.
     * - LMSR: deducts C(0,0) subsidy from MM → event account
     * - Order Book: MM pays for initial share pairs
     *
     * @param event the event to activate
     * @param mmUser the market maker user
     * @return error message if failed, null if success
     */
    public String activateEvent(Event event, User mmUser) {
        if (event.getStatus() != EventStatus.NOT_STARTED) {
            return "Event is not in 'Not Started' state.";
        }
        if (!mmUser.isMmFor(event.getId())) {
            return "Only the Market Maker can activate this event.";
        }
        if (mmUser.isBlocked()) {
            return "User '" + mmUser.getName() + "' is blocked (negative balance).";
        }

        if (event.isLMSR()) {
            // Calculate LMSR initial subsidy: C(0,0) = b * ln(2)
            double subsidy = LmsrCalculator.calculatePool(0, 0, event.getB());
            if (!mmUser.canAfford(subsidy)) {
                return String.format("Market Maker '%s' cannot afford the LMSR subsidy of $%.2f (balance: $%.2f)",
                        mmUser.getName(), subsidy, mmUser.getCash());
            }
            mmUser.withdraw(subsidy);
            event.addToAccount(subsidy);
        } else {
            // Order Book: initial mint
            String error = OrderBookManager.performInitialMint(event, mmUser);
            if (error != null) return error;
        }

        event.setStatus(EventStatus.ACTIVE);
        return null; // Success
    }

    // ========================== LMSR TRADING ==========================

    /**
     * Buys shares in an LMSR event.
     *
     * @param event the LMSR event
     * @param user the buying user
     * @param optionIndex 0 or 1
     * @param quantity number of shares to buy
     * @return TradeResult with outcome details
     */
    public TradeResult buySharesLMSR(Event event, User user, int optionIndex, int quantity) {
        if (event.getStatus() != EventStatus.ACTIVE) {
            return TradeResult.failure("Event is not active.");
        }
        if (!event.isLMSR()) {
            return TradeResult.failure("This event uses Order Book, not LMSR.");
        }
        if (user.isBlocked()) {
            return TradeResult.failure("User '" + user.getName() + "' is blocked.");
        }
        if (quantity <= 0) {
            return TradeResult.failure("Quantity must be positive.");
        }

        String chosenOption = event.getOptions().get(optionIndex);
        double costBefore = LmsrCalculator.calculatePool(event.getQYes(), event.getQNo(), event.getB());

        int newQYes = event.getQYes() + (optionIndex == 0 ? quantity : 0);
        int newQNo = event.getQNo() + (optionIndex == 1 ? quantity : 0);
        double costAfter = LmsrCalculator.calculatePool(newQYes, newQNo, event.getB());

        double baseCost = costAfter - costBefore;
        double commission = 0;

        if (event.getCommissionType().equals("on-purchase")) {
            commission = baseCost * (event.getCommission() / 100.0);
        }

        double totalPaid = baseCost + commission;

        if (!user.canAfford(totalPaid)) {
            return TradeResult.failure(String.format(
                    "Insufficient funds. Cost: $%.2f, Balance: $%.2f",
                    totalPaid, user.getCash()));
        }

        // Execute trade
        user.withdraw(totalPaid);
        event.addToAccount(baseCost);
        event.setQYes(newQYes);
        event.setQNo(newQNo);

        if (commission > 0) {
            event.setTotalCommissionCollected(event.getTotalCommissionCollected() + commission);
            // Commission goes to MM's account
            User mmUser = getUserByName(event.getMmUserName());
            if (mmUser != null) {
                mmUser.deposit(commission);
            }
        }

        // Record transaction
        Transaction t = new Transaction(chosenOption, optionIndex, quantity,
                totalPaid, user.getName(), commission);
        event.addTransaction(t);

        // Update user's event data
        UserEventData userData = user.getEventData(event.getId());
        userData.addShares(optionIndex, quantity);
        userData.addCommissionPaid(commission);
        userData.addTransaction(t);

        return TradeResult.success(baseCost, commission, totalPaid,
                String.format("Bought %d shares of '%s' for $%.2f (Base: $%.2f, Commission: $%.2f)",
                        quantity, chosenOption, totalPaid, baseCost, commission));
    }

    /**
     * Legacy method for EX1 ConsoleApp compatibility.
     */
    public void buyShares(Event e, int optionIndex, int quantity) {
        // For EX1, there are no users. Simulate with a direct approach.
        String chosenOption = e.getOptions().get(optionIndex);
        double costBefore = LmsrCalculator.calculatePool(e.getQYes(), e.getQNo(), e.getB());

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
        e.setQYes(newQYes);
        e.setQNo(newQNo);
        e.addTransaction(new Transaction(chosenOption, quantity, totalPaid));
    }

    // ========================== ORDER BOOK TRADING ==========================

    /**
     * Places an order in an Order Book event.
     *
     * @param event the OB event
     * @param user the user placing the order
     * @param optionIndex 0 or 1
     * @param orderType BUY or SELL
     * @param quantity number of shares
     * @param pricePerShare price per share
     * @return TradeResult with outcome details
     */
    public TradeResult placeOrder(Event event, User user, int optionIndex,
                                   OrderType orderType, int quantity, double pricePerShare) {
        if (event.getStatus() != EventStatus.ACTIVE) {
            return TradeResult.failure("Event is not active.");
        }
        if (!event.isOrderBook()) {
            return TradeResult.failure("This event uses LMSR, not Order Book.");
        }
        if (user.isBlocked()) {
            return TradeResult.failure("User '" + user.getName() + "' is blocked.");
        }
        if (quantity <= 0) {
            return TradeResult.failure("Quantity must be positive.");
        }

        // For SELL orders, verify user has enough shares
        if (orderType == OrderType.SELL) {
            UserEventData userData = user.getEventData(event.getId());
            if (userData.getShares(optionIndex) < quantity) {
                return TradeResult.failure(String.format(
                        "Insufficient shares. You have %d, trying to sell %d.",
                        userData.getShares(optionIndex), quantity));
            }
        }

        // For BUY orders, verify user can potentially afford it
        if (orderType == OrderType.BUY) {
            double maxCost = quantity * pricePerShare;
            if (!user.canAfford(maxCost)) {
                return TradeResult.failure(String.format(
                        "Insufficient funds. Max cost: $%.2f, Balance: $%.2f",
                        maxCost, user.getCash()));
            }
        }

        // Create and process the order
        Order order = new Order(user.getName(), optionIndex, orderType, quantity, pricePerShare);
        OrderBookManager.ProcessResult result = OrderBookManager.processOrder(event, order);

        if (result.isRejected()) {
            return TradeResult.failure(result.getErrorMessage());
        }

        // Process all executed trades
        double totalPaid = 0;
        double totalCommission = 0;
        int totalSharesTraded = 0;

        for (OrderBookManager.Trade trade : result.getTrades()) {
            totalSharesTraded += trade.getQuantity();

            if (trade.getType() == OrderBookManager.Trade.TradeType.RESALE) {
                processResaleTrade(event, trade);
            } else {
                processMintTrade(event, trade);
            }

            // Calculate commission for this trade
            if (event.getCommissionType().equals("on-purchase")) {
                double commission = trade.getTotalAmount() * (event.getCommission() / 100.0);
                totalCommission += commission;

                // Deduct from buyer and pay to MM
                User buyer = getUserByName(trade.getBuyer());
                if (buyer != null) {
                    buyer.withdraw(commission);
                    UserEventData buyerData = buyer.getEventData(event.getId());
                    buyerData.addCommissionPaid(commission);
                }
                User mmUser = getUserByName(event.getMmUserName());
                if (mmUser != null) {
                    mmUser.deposit(commission);
                }
                event.setTotalCommissionCollected(event.getTotalCommissionCollected() + commission);
            }

            totalPaid += trade.getTotalAmount();
        }

        if (result.getTrades().isEmpty()) {
            String optionName = event.getOptions().get(optionIndex);
            return TradeResult.success(0, 0, 0,
                    String.format("Order placed: %s %d shares of '%s' @ $%.2f. Resting in the book.",
                            orderType, quantity, optionName, pricePerShare));
        }

        return TradeResult.success(totalPaid, totalCommission, totalPaid + totalCommission,
                String.format("%d trade(s) executed for %d shares. Total: $%.2f, Commission: $%.2f",
                        result.getTrades().size(), totalSharesTraded, totalPaid, totalCommission));
    }

    /**
     * Processes a resale trade: money and shares change hands between buyer and seller.
     */
    private void processResaleTrade(Event event, OrderBookManager.Trade trade) {
        User buyer = getUserByName(trade.getBuyer());
        User seller = getUserByName(trade.getSeller());

        if (buyer != null) {
            buyer.withdraw(trade.getTotalAmount());
            UserEventData buyerData = buyer.getEventData(event.getId());
            buyerData.addShares(trade.getOptionIndex(), trade.getQuantity());

            String optionName = event.getOptions().get(trade.getOptionIndex());
            Transaction t = new Transaction(optionName, trade.getOptionIndex(),
                    trade.getQuantity(), trade.getTotalAmount(), buyer.getName(), 0);
            buyerData.addTransaction(t);
            event.addTransaction(t);
        }

        if (seller != null) {
            seller.deposit(trade.getTotalAmount());
            UserEventData sellerData = seller.getEventData(event.getId());
            sellerData.removeShares(trade.getOptionIndex(), trade.getQuantity());
        }
    }

    /**
     * Processes a mint trade: buyer pays into the event account and receives new shares.
     */
    private void processMintTrade(Event event, OrderBookManager.Trade trade) {
        User buyer = getUserByName(trade.getBuyer());

        if (buyer != null) {
            buyer.withdraw(trade.getTotalAmount());
            UserEventData buyerData = buyer.getEventData(event.getId());
            buyerData.addShares(trade.getOptionIndex(), trade.getQuantity());

            String optionName = event.getOptions().get(trade.getOptionIndex());
            Transaction t = new Transaction(optionName, trade.getOptionIndex(),
                    trade.getQuantity(), trade.getTotalAmount(), buyer.getName(), 0);
            buyerData.addTransaction(t);
            event.addTransaction(t);
        }

        // Mint payment goes to the event's contract account
        event.addToAccount(trade.getTotalAmount());
    }

    // ========================== EVENT CLOSING ==========================

    /**
     * Closes an event and resolves it with a winning option.
     *
     * @param event the event to close
     * @param mmUser the market maker user
     * @param winningIndex the index of the winning option
     * @return error message if failed, null if success
     */
    public String closeEvent(Event event, User mmUser, int winningIndex) {
        if (event.getStatus() != EventStatus.ACTIVE) {
            return "Event is not active.";
        }
        if (!mmUser.isMmFor(event.getId())) {
            return "Only the Market Maker can close this event.";
        }

        event.setStatus(EventStatus.CLOSED);
        String winner = event.getOptions().get(winningIndex);
        event.setWinningOption(winner);

        if (event.isLMSR()) {
            closeLmsrEvent(event, mmUser, winningIndex);
        } else {
            closeOrderBookEvent(event, mmUser, winningIndex);
        }

        return null; // Success
    }

    /**
     * Legacy close method for EX1 ConsoleApp compatibility.
     */
    public void closeEvent(Event e, int winningIndex) {
        e.setStatus(EventStatus.CLOSED);
        String winner = e.getOptions().get(winningIndex);
        e.setWinningOption(winner);

        int winningShares = (winningIndex == 0) ? e.getQYes() : e.getQNo();
        double payout = winningShares * 1.0;

        if (e.getCommissionType().equals("on-close")) {
            double commission = payout * (e.getCommission() / 100.0);
            e.setTotalCommissionCollected(e.getTotalCommissionCollected() + commission);
        }
    }

    /**
     * Closes an LMSR event: pays winners, handles on-close commission,
     * returns remaining subsidy to MM.
     */
    private void closeLmsrEvent(Event event, User mmUser, int winningIndex) {
        int winningShares = (winningIndex == 0) ? event.getQYes() : event.getQNo();
        double totalPayout = winningShares * 1.0; // $1 per winning share

        double commission = 0;
        if (event.getCommissionType().equals("on-close")) {
            commission = totalPayout * (event.getCommission() / 100.0);
            event.setTotalCommissionCollected(event.getTotalCommissionCollected() + commission);
            mmUser.deposit(commission);
        }

        double netPayout = totalPayout - commission;

        // Pay each user proportionally to their winning shares
        if (users != null) {
            for (User user : users) {
                UserEventData data = user.getEventData(event.getId());
                int userWinningShares = data.getShares(winningIndex);
                if (userWinningShares > 0) {
                    double userPayout = userWinningShares * (1.0 - (commission > 0 ? event.getCommission() / 100.0 : 0));
                    user.deposit(userPayout);
                    event.deductFromAccount(userPayout);
                }
            }
        }

        // Return remaining balance to MM (leftover subsidy)
        if (event.getAccountBalance() > 0) {
            mmUser.deposit(event.getAccountBalance());
            event.setAccountBalance(0);
        }
    }

    /**
     * Closes an Order Book event: pays winners from the event account,
     * handles on-close commission.
     */
    private void closeOrderBookEvent(Event event, User mmUser, int winningIndex) {
        double commission = 0;

        // Calculate total winning shares and payout
        int d = event.getObConfig().getD();

        // Clear all resting orders
        event.getYesBids().clear();
        event.getYesAsks().clear();
        event.getNoBids().clear();
        event.getNoAsks().clear();

        // Pay each winner
        if (users != null) {
            for (User user : users) {
                UserEventData data = user.getEventData(event.getId());
                int winShares = data.getShares(winningIndex);
                if (winShares > 0) {
                    double payout = winShares * d;

                    if (event.getCommissionType().equals("on-close")) {
                        double userCommission = payout * (event.getCommission() / 100.0);
                        commission += userCommission;
                        payout -= userCommission;
                    }

                    user.deposit(payout);
                    event.deductFromAccount(payout);
                }
            }
        }

        if (commission > 0) {
            event.setTotalCommissionCollected(event.getTotalCommissionCollected() + commission);
            mmUser.deposit(commission);
        }

        // Remaining balance stays in the event account (may be negative for LMSR)
    }

    // ========================== DISPLAY HELPERS ==========================

    /**
     * Legacy display method for EX1 ConsoleApp.
     */
    public void displayEventStatus(Event e) {
        System.out.println("\n--- Event: " + e.getName() + " ---");
        String opt0 = e.getOptions().get(0);
        String opt1 = e.getOptions().get(1);

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
                System.out.printf("  Bought %d shares of '%s' for %.2f\n",
                        t.getQuantity(), t.getOptionName(), t.getPricePaid());
            }
        }

        if (e.getStatus() == EventStatus.CLOSED) {
            System.out.println("STATUS: CLOSED. Winning Option: " + e.getWinningOption());
        }
    }

    // ========================== FILTERING ==========================

    /**
     * Returns events filtered by trading method.
     */
    public List<Event> getEventsByMethod(TradingMethod method) {
        List<Event> filtered = new ArrayList<>();
        if (events == null) return filtered;
        for (Event e : events) {
            if (e.getTradingMethod() == method) filtered.add(e);
        }
        return filtered;
    }

    /**
     * Returns events filtered by status.
     */
    public List<Event> getEventsByStatus(EventStatus status) {
        List<Event> filtered = new ArrayList<>();
        if (events == null) return filtered;
        for (Event e : events) {
            if (e.getStatus() == status) filtered.add(e);
        }
        return filtered;
    }

    /**
     * Returns events filtered by commission type.
     */
    public List<Event> getEventsByCommissionType(String type) {
        List<Event> filtered = new ArrayList<>();
        if (events == null) return filtered;
        for (Event e : events) {
            if (e.getCommissionType().equals(type)) filtered.add(e);
        }
        return filtered;
    }

    /**
     * Returns events matching all specified filters. Null filter = no filter.
     */
    public List<Event> getFilteredEvents(TradingMethod method, EventStatus status, String commissionType) {
        List<Event> filtered = new ArrayList<>();
        if (events == null) return filtered;
        for (Event e : events) {
            boolean match = true;
            if (method != null && e.getTradingMethod() != method) match = false;
            if (status != null && e.getStatus() != status) match = false;
            if (commissionType != null && !e.getCommissionType().equals(commissionType)) match = false;
            if (match) filtered.add(e);
        }
        return filtered;
    }
}