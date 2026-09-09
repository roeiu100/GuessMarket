package ui;

/**
 * Main launcher entry point that does not extend javafx.application.Application.
 * This prevents the "JavaFX runtime components are missing" error when packaged into an executable JAR.
 */
public class MainLauncher {
    public static void main(String[] args) {
        GuessMarketApp.main(args);
    }
}
