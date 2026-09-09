package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a user in the Guess Market system.
 * Each user has a unique name, a cash balance, and may be a
 * Market Maker (MM) for one or more events.
 */
public class User {
    private String name;
    private double cash;
    private double initialCash;
    private List<Integer> mmEventIds;   // Event IDs this user is MM for
    private Map<Integer, UserEventData> eventDataMap; // Per-event participation data
    private boolean blocked;            // True if user went to negative balance

    public User(String name, double initialCash) {
        this.name = name;
        this.cash = initialCash;
        this.initialCash = initialCash;
        this.mmEventIds = new ArrayList<>();
        this.eventDataMap = new HashMap<>();
        this.blocked = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCash() { return cash; }
    public void setCash(double cash) { this.cash = cash; }

    public double getInitialCash() { return initialCash; }
    public void setInitialCash(double initialCash) { this.initialCash = initialCash; }

    public List<Integer> getMmEventIds() { return mmEventIds; }
    public void setMmEventIds(List<Integer> mmEventIds) { this.mmEventIds = mmEventIds; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    /**
     * Checks if this user is the Market Maker for the given event ID.
     */
    public boolean isMmFor(int eventId) {
        return mmEventIds.contains(eventId);
    }

    /**
     * Checks if this user is a Market Maker for any event.
     */
    public boolean isMarketMaker() {
        return !mmEventIds.isEmpty();
    }

    /**
     * Checks if this user can afford a given amount.
     */
    public boolean canAfford(double amount) {
        return cash >= amount;
    }

    /**
     * Deducts money from the user's account.
     * If balance goes negative, marks the user as blocked.
     */
    public void withdraw(double amount) {
        this.cash -= amount;
        if (this.cash < 0) {
            this.blocked = true;
        }
    }

    /**
     * Adds money to the user's account.
     */
    public void deposit(double amount) {
        this.cash += amount;
    }

    /**
     * Gets or creates the per-event participation data for a given event.
     */
    public UserEventData getEventData(int eventId) {
        return eventDataMap.computeIfAbsent(eventId, k -> new UserEventData());
    }

    /**
     * Checks if the user is participating in a given event.
     */
    public boolean isParticipatingIn(int eventId) {
        return eventDataMap.containsKey(eventId);
    }

    /**
     * Returns all event IDs the user is participating in.
     */
    public Map<Integer, UserEventData> getEventDataMap() {
        return eventDataMap;
    }
}
