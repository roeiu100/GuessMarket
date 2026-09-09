package orderbook;

import models.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages the order book for a single option (e.g., YES or NO) within an event.
 * Maintains sorted bid (buy) and ask (sell) lists.
 */
public class OrderBook {
    private final List<Order> bids; // Sorted descending by price (best bid first)
    private final List<Order> asks; // Sorted ascending by price (best ask first)
    private Double lastTradePrice;

    public OrderBook(List<Order> bids, List<Order> asks) {
        this.bids = bids;
        this.asks = asks;
        this.lastTradePrice = null;
    }

    public List<Order> getBids() { return bids; }
    public List<Order> getAsks() { return asks; }

    public Double getLastTradePrice() { return lastTradePrice; }
    public void setLastTradePrice(Double price) { this.lastTradePrice = price; }

    /**
     * Returns the best (highest) bid price, or null if no bids exist.
     */
    public Double getBestBidPrice() {
        return bids.isEmpty() ? null : bids.get(0).getPricePerShare();
    }

    /**
     * Returns the best (lowest) ask price, or null if no asks exist.
     */
    public Double getBestAskPrice() {
        return asks.isEmpty() ? null : asks.get(0).getPricePerShare();
    }

    /**
     * Returns the mid price: (bestBid + bestAsk) / 2, or null.
     */
    public Double getMidPrice() {
        Double bid = getBestBidPrice();
        Double ask = getBestAskPrice();
        if (bid == null || ask == null) return null;
        return (bid + ask) / 2.0;
    }

    /**
     * Returns the spread: bestAsk - bestBid, or null.
     */
    public Double getSpread() {
        Double bid = getBestBidPrice();
        Double ask = getBestAskPrice();
        if (bid == null || ask == null) return null;
        return ask - bid;
    }

    /**
     * Inserts a BUY order into the bids list, maintaining descending price order.
     * Within the same price, earlier orders come first (FIFO).
     */
    public void insertBid(Order order) {
        int idx = 0;
        while (idx < bids.size() && bids.get(idx).getPricePerShare() > order.getPricePerShare()) {
            idx++;
        }
        // At equal prices, insert after existing ones (FIFO)
        while (idx < bids.size() && bids.get(idx).getPricePerShare() == order.getPricePerShare()) {
            idx++;
        }
        bids.add(idx, order);
    }

    /**
     * Inserts a SELL order into the asks list, maintaining ascending price order.
     * Within the same price, earlier orders come first (FIFO).
     */
    public void insertAsk(Order order) {
        int idx = 0;
        while (idx < asks.size() && asks.get(idx).getPricePerShare() < order.getPricePerShare()) {
            idx++;
        }
        // At equal prices, insert after existing ones (FIFO)
        while (idx < asks.size() && asks.get(idx).getPricePerShare() == order.getPricePerShare()) {
            idx++;
        }
        asks.add(idx, order);
    }

    /**
     * Removes all fully-filled orders from both sides.
     */
    public void cleanFilledOrders() {
        bids.removeIf(Order::isFilled);
        asks.removeIf(Order::isFilled);
    }
}
