package orderbook;

import models.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the Order Book matching logic for an event.
 * Handles: resale matching, minting (when allowed), partial fills, and order rejection.
 *
 * The matching algorithm follows these rules per the CLOB simulation:
 * 1. When a BUY order comes in, match against resting SELL (ask) orders on the same option book
 * 2. When a SELL order comes in, match against resting BUY (bid) orders on the same option book
 * 3. After same-book matching, check cross-book minting opportunity (if allowed):
 *    - A YES BUY + NO BUY whose prices sum to >= d can trigger a mint
 * 4. Unmatched remainder rests in the book
 */
public class OrderBookManager {

    /**
     * Result of processing an order.
     */
    public static class ProcessResult {
        private final List<Trade> trades;
        private final String errorMessage;
        private final boolean rejected;

        public ProcessResult(List<Trade> trades) {
            this.trades = trades;
            this.errorMessage = null;
            this.rejected = false;
        }

        public ProcessResult(String errorMessage) {
            this.trades = new ArrayList<>();
            this.errorMessage = errorMessage;
            this.rejected = true;
        }

        public List<Trade> getTrades() { return trades; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isRejected() { return rejected; }
    }

    /**
     * Represents a single executed trade.
     */
    public static class Trade {
        public enum TradeType { RESALE, MINT }

        private final TradeType type;
        private final String buyer;
        private final String seller;
        private final int optionIndex;
        private final int quantity;
        private final double pricePerShare;
        private final double totalAmount;

        public Trade(TradeType type, String buyer, String seller,
                     int optionIndex, int quantity, double pricePerShare) {
            this.type = type;
            this.buyer = buyer;
            this.seller = seller;
            this.optionIndex = optionIndex;
            this.quantity = quantity;
            this.pricePerShare = pricePerShare;
            this.totalAmount = quantity * pricePerShare;
        }

        public TradeType getType() { return type; }
        public String getBuyer() { return buyer; }
        public String getSeller() { return seller; }
        public int getOptionIndex() { return optionIndex; }
        public int getQuantity() { return quantity; }
        public double getPricePerShare() { return pricePerShare; }
        public double getTotalAmount() { return totalAmount; }
    }

    /**
     * Processes an incoming order against the event's order books.
     *
     * @param event the event containing the order books
     * @param order the incoming order
     * @return result containing executed trades or rejection
     */
    public static ProcessResult processOrder(Event event, Order order) {
        OrderBookConfig config = event.getObConfig();
        double maxPrice = config.getMaxPrice();

        // Validate price range: 0.01 <= price <= d - 0.01
        if (order.getPricePerShare() < 0.01 || order.getPricePerShare() > maxPrice) {
            return new ProcessResult(
                    String.format("Price must be between $0.01 and $%.2f. " +
                            "A share can never be worth more than the $%d it pays out.",
                            maxPrice, config.getD()));
        }

        List<Trade> allTrades = new ArrayList<>();
        OrderBook book = getBookForOption(event, order.getOptionIndex());

        if (order.getOrderType() == OrderType.BUY) {
            // Match against resting asks (sells) on the same option book
            matchBuyAgainstAsks(event, order, book, allTrades);

            // If there's remaining quantity and minting is allowed, try cross-book mint
            if (order.getRemainingQuantity() > 0 && config.isAllowMint()) {
                tryCrossBookMint(event, order, allTrades);
            }

            // If there's still remaining quantity, rest the order as a bid
            if (order.getRemainingQuantity() > 0) {
                book.insertBid(order);
            }
        } else {
            // SELL order: match against resting bids (buys) on the same option book
            matchSellAgainstBids(event, order, book, allTrades);

            // If there's still remaining quantity, rest the order as an ask
            if (order.getRemainingQuantity() > 0) {
                book.insertAsk(order);
            }
        }

        // Clean up fully filled orders
        book.cleanFilledOrders();
        getBookForOption(event, 1 - order.getOptionIndex()).cleanFilledOrders();

        return new ProcessResult(allTrades);
    }

    /**
     * Matches a BUY order against resting ASK orders.
     * Walks through asks best-price-first, filling as many shares as possible.
     */
    private static void matchBuyAgainstAsks(Event event, Order buyOrder,
                                             OrderBook book, List<Trade> trades) {
        List<Order> asks = book.getAsks();

        while (buyOrder.getRemainingQuantity() > 0 && !asks.isEmpty()) {
            Order bestAsk = asks.get(0);

            // Check if buyer's price meets or exceeds the ask price
            if (buyOrder.getPricePerShare() < bestAsk.getPricePerShare()) {
                break; // No match possible
            }

            // Execute trade at the ask price (resting order's price)
            int fillQty = Math.min(buyOrder.getRemainingQuantity(), bestAsk.getRemainingQuantity());
            double tradePrice = bestAsk.getPricePerShare();

            buyOrder.fill(fillQty);
            bestAsk.fill(fillQty);

            trades.add(new Trade(Trade.TradeType.RESALE,
                    buyOrder.getUserName(), bestAsk.getUserName(),
                    buyOrder.getOptionIndex(), fillQty, tradePrice));

            // Update last trade price
            book.setLastTradePrice(tradePrice);
            event.setLastTradePrice(buyOrder.getOptionIndex(), tradePrice);

            // Remove filled ask
            if (bestAsk.isFilled()) {
                asks.remove(0);
            }
        }
    }

    /**
     * Matches a SELL order against resting BID orders.
     * Walks through bids best-price-first (highest first), filling as many shares as possible.
     */
    private static void matchSellAgainstBids(Event event, Order sellOrder,
                                              OrderBook book, List<Trade> trades) {
        List<Order> bids = book.getBids();

        while (sellOrder.getRemainingQuantity() > 0 && !bids.isEmpty()) {
            Order bestBid = bids.get(0);

            // Check if seller's minimum price is met
            if (sellOrder.getPricePerShare() > bestBid.getPricePerShare()) {
                break; // No match possible
            }

            // Execute trade at the bid price (resting order's price)
            int fillQty = Math.min(sellOrder.getRemainingQuantity(), bestBid.getRemainingQuantity());
            double tradePrice = bestBid.getPricePerShare();

            sellOrder.fill(fillQty);
            bestBid.fill(fillQty);

            trades.add(new Trade(Trade.TradeType.RESALE,
                    bestBid.getUserName(), sellOrder.getUserName(),
                    sellOrder.getOptionIndex(), fillQty, tradePrice));

            // Update last trade price
            book.setLastTradePrice(tradePrice);
            event.setLastTradePrice(sellOrder.getOptionIndex(), tradePrice);

            // Remove filled bid
            if (bestBid.isFilled()) {
                bids.remove(0);
            }
        }
    }

    /**
     * Attempts a cross-book mint: if there's a resting bid on the OPPOSITE option
     * and the incoming order is a BUY on one side, and the two prices sum to >= d,
     * mint new share pairs.
     *
     * The resting order keeps its full price; the incoming order pays d - restingPrice.
     */
    private static void tryCrossBookMint(Event event, Order incomingBuy, List<Trade> trades) {
        int oppositeOption = 1 - incomingBuy.getOptionIndex();
        OrderBookConfig config = event.getObConfig();
        int d = config.getD();

        OrderBook oppositeBook = getBookForOption(event, oppositeOption);
        List<Order> oppositeBids = oppositeBook.getBids();

        while (incomingBuy.getRemainingQuantity() > 0 && !oppositeBids.isEmpty()) {
            Order oppBid = oppositeBids.get(0);

            // Check if the two prices together reach or exceed d (course spec allows combined >= d)
            double combined = incomingBuy.getPricePerShare() + oppBid.getPricePerShare();
            if (combined < d) {
                break; // Can't mint
            }

            // Mint: smaller of the two quantities
            int mintQty = Math.min(incomingBuy.getRemainingQuantity(), oppBid.getRemainingQuantity());

            // The resting order keeps its price; incoming pays the complement
            double restingPrice = oppBid.getPricePerShare();
            double incomingPrice = d - restingPrice;

            incomingBuy.fill(mintQty);
            oppBid.fill(mintQty);

            // Record as MINT trade for the incoming side (YES or NO)
            trades.add(new Trade(Trade.TradeType.MINT,
                    incomingBuy.getUserName(), null,
                    incomingBuy.getOptionIndex(), mintQty, incomingPrice));

            // Record MINT for the opposite side
            trades.add(new Trade(Trade.TradeType.MINT,
                    oppBid.getUserName(), null,
                    oppositeOption, mintQty, restingPrice));

            // Update last trade prices for both options
            OrderBook incomingBook = getBookForOption(event, incomingBuy.getOptionIndex());
            incomingBook.setLastTradePrice(incomingPrice);
            event.setLastTradePrice(incomingBuy.getOptionIndex(), incomingPrice);
            oppositeBook.setLastTradePrice(restingPrice);
            event.setLastTradePrice(oppositeOption, restingPrice);

            // Remove filled opposite bid
            if (oppBid.isFilled()) {
                oppositeBids.remove(0);
            }
        }
    }

    /**
     * Returns the OrderBook for the given option index within an event.
     */
    private static OrderBook getBookForOption(Event event, int optionIndex) {
        return new OrderBook(event.getBids(optionIndex), event.getAsks(optionIndex));
    }

    /**
     * Performs the initial minting for the Market Maker when activating an OB event.
     * The MM pays initial * d dollars and receives 'initial' pairs of shares.
     *
     * @param event the OB event being activated
     * @param mmUser the Market Maker user
     * @return error message if failed, null if success
     */
    public static String performInitialMint(Event event, User mmUser) {
        OrderBookConfig config = event.getObConfig();
        int initialPairs = config.getInitial();
        int d = config.getD();

        if (initialPairs == 0) {
            return null; // No initial minting needed
        }

        double cost = initialPairs * d;
        if (!mmUser.canAfford(cost)) {
            return String.format("Market Maker '%s' cannot afford the initial mint cost of $%.2f (balance: $%.2f)",
                    mmUser.getName(), cost, mmUser.getCash());
        }

        // Transfer money from MM to event account
        mmUser.withdraw(cost);
        event.addToAccount(cost);

        // Give MM the initial shares
        UserEventData mmData = mmUser.getEventData(event.getId());
        mmData.addShares(0, initialPairs); // YES shares
        mmData.addShares(1, initialPairs); // NO shares

        return null; // Success
    }
}
