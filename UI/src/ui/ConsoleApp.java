package ui;

import engine.MarketManager;
import models.Event;
import java.util.Scanner;

public class ConsoleApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MarketManager manager = new MarketManager();
        boolean running = true;
        
        while (running) {
            System.out.println("\n--- Guess Market ---");
            System.out.println("1. Load XML File");
            System.out.println("2. Show Events");
            System.out.println("3. Event State");
            System.out.println("4. Buy Shares");
            System.out.println("5. Close Event");
            System.out.println("6. Exit");
            System.out.print("Choose (1-6): ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    System.out.print("Enter XML path: ");
                    String path = scanner.nextLine().trim();
                    try {
                        manager.loadEvents(path);
                        System.out.println("Loaded " + manager.getEvents().size() + " events.");
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                case "2":
                    if (manager.getEvents() == null) { System.out.println("Load a file first."); break; }
                    for (int i = 0; i < manager.getEvents().size(); i++) {
                        Event e = manager.getEvents().get(i);
                        System.out.printf("%d. [%s] %s | Comm: %d%% (%s) | Status: %s\n", 
                            i + 1, e.getId(), e.getName(), e.getCommission(), e.getCommissionType(), e.isActive() ? "Active" : "Closed");
                    }
                    break;
                case "3":
                    if (manager.getEvents() == null) { System.out.println("Load a file first."); break; }
                    System.out.print("Enter Event Number (1-" + manager.getEvents().size() + "): ");
                    int stateIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    manager.displayEventStatus(manager.getEvents().get(stateIdx));
                    break;
                case "4":
                    if (manager.getEvents() == null) { System.out.println("Load a file first."); break; }
                    System.out.print("Enter Event Number (1-" + manager.getEvents().size() + "): ");
                    int buyIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    Event buyEvent = manager.getEvents().get(buyIdx);
                    
                    System.out.println("1. " + buyEvent.getOptions().get(0));
                    System.out.println("2. " + buyEvent.getOptions().get(1));
                    System.out.print("Choose Option (1 or 2): ");
                    int optIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    
                    System.out.print("Enter quantity: ");
                    int qty = Integer.parseInt(scanner.nextLine().trim());
                    
                    manager.buyShares(buyEvent, optIdx, qty);
                    break;
                case "5":
                    if (manager.getEvents() == null) { System.out.println("Load a file first."); break; }
                    System.out.print("Enter Event Number to close: ");
                    int closeIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    Event closeEvent = manager.getEvents().get(closeIdx);
                    
                    System.out.println("Who won?");
                    System.out.println("1. " + closeEvent.getOptions().get(0));
                    System.out.println("2. " + closeEvent.getOptions().get(1));
                    System.out.print("Choose winner (1 or 2): ");
                    int winIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    
                    manager.closeEvent(closeEvent, winIdx);
                    break;
                case "6":
                    running = false;
                    System.out.println("Goodbye.");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
        scanner.close();
    }
}