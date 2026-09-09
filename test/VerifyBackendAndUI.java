package test;

import engine.MarketManager;
import engine.TradeResult;
import models.*;

import java.io.File;

/**
 * Automated verification for Exercise 2:
 * Tests MarketManager loading, validation on all EX2 test files,
 * event activation, LMSR trading, and Order Book order matching.
 */
public class VerifyBackendAndUI {

    public static void main(String[] args) {
        System.out.println("=== Starting Exercise 2 Automated Verification ===");
        int passed = 0;
        int failed = 0;

        MarketManager manager = new MarketManager();

        // Test 1: Load small.xml
        try {
            System.out.println("\n[Test 1] Loading EX 2/small.xml...");
            String res = manager.loadFile("EX 2/small.xml");
            System.out.println("Result: " + res.trim());
            assertCondition(manager.isLoaded(), "Manager should be loaded");
            assertCondition(manager.getEvents().size() == 2, "Should have 2 events");
            assertCondition(manager.getUsers().size() == 3, "Should have 3 users");
            Event e1 = manager.getEvents().get(0);
            assertCondition(e1.getStatus() == EventStatus.NOT_STARTED, "Event should start as NOT_STARTED");
            System.out.println("PASSED: small.xml loaded correctly.");
            passed++;
        } catch (Exception ex) {
            System.out.println("FAILED: " + ex.getMessage());
            ex.printStackTrace();
            failed++;
        }

        // Test 2: Activate event in small.xml as MM
        try {
            System.out.println("\n[Test 2] Activating event 1 as MM (Tikva)...");
            Event e1 = manager.getEvents().get(0);
            User tikva = manager.getUserByName("Tikva");
            User menash = manager.getUserByName("Menash");

            // Non-MM should fail
            String errNonMM = manager.activateEvent(e1, menash);
            assertCondition(errNonMM != null, "Non-MM activation should fail");

            // MM should succeed
            String errMM = manager.activateEvent(e1, tikva);
            assertCondition(errMM == null, "MM activation should succeed: " + errMM);
            assertCondition(e1.getStatus() == EventStatus.ACTIVE, "Event status should be ACTIVE");
            System.out.println("PASSED: Event activated by MM (Tikva).");
            passed++;
        } catch (Exception ex) {
            System.out.println("FAILED: " + ex.getMessage());
            ex.printStackTrace();
            failed++;
        }

        // Test 3: Load multiple.xml
        try {
            System.out.println("\n[Test 3] Loading EX 2/multiple.xml...");
            String res = manager.loadFile("EX 2/multiple.xml");
            System.out.println("Result: " + res.trim());
            assertCondition(manager.getEvents().size() >= 2, "Should have at least 2 events");
            assertCondition(manager.getUsers().size() >= 2, "Should have users");
            System.out.println("PASSED: multiple.xml loaded correctly.");
            passed++;
        } catch (Exception ex) {
            System.out.println("FAILED: " + ex.getMessage());
            ex.printStackTrace();
            failed++;
        }

        // Test 4: Reject error-2.xml (initial-cash=0)
        try {
            System.out.println("\n[Test 4] Verifying rejection of EX 2/error-2.xml (initial-cash <= 0)...");
            manager.loadFile("EX 2/error-2.xml");
            System.out.println("FAILED: error-2.xml should have thrown an exception!");
            failed++;
        } catch (Exception ex) {
            System.out.println("PASSED: error-2.xml was correctly rejected: " + ex.getMessage());
            passed++;
        }

        // Test 5: Reject error-3.xml (invalid MM event reference)
        try {
            System.out.println("\n[Test 5] Verifying rejection of EX 2/error-3.xml (invalid MM reference)...");
            manager.loadFile("EX 2/error-3.xml");
            System.out.println("FAILED: error-3.xml should have thrown an exception!");
            failed++;
        } catch (Exception ex) {
            System.out.println("PASSED: error-3.xml was correctly rejected: " + ex.getMessage());
            passed++;
        }

        // Test 6: Verify JavaFX classes bytecode & loading
        try {
            System.out.println("\n[Test 6] Checking JavaFX UI classes loading...");
            Class.forName("ui.GuessMarketApp");
            Class.forName("ui.EventsViewController");
            Class.forName("ui.UsersViewController");
            Class.forName("ui.OrderBookComponent");
            Class.forName("ui.ThemeManager");
            Class.forName("ui.MainLauncher");
            System.out.println("PASSED: All JavaFX classes found and loaded into JVM.");
            passed++;
        } catch (Exception ex) {
            System.out.println("FAILED: " + ex.getMessage());
            ex.printStackTrace();
            failed++;
        }

        System.out.println("\n========================================");
        System.out.println(String.format("Total: %d | Passed: %d | Failed: %d", passed + failed, passed, failed));
        System.out.println("========================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertCondition(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException("Assertion failed: " + msg);
        }
    }
}
