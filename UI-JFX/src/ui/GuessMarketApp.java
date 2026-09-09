package ui;

import engine.MarketManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.User;

import java.io.File;

/**
 * Main JavaFX Application for Guess Market (Exercise 2).
 * Strictly conforms to the layout outlines in 'ex 2 scetch.pptx'.
 * Interacts exclusively with the MarketManager backend engine.
 */
public class GuessMarketApp extends Application {

    private final MarketManager manager = new MarketManager();
    private Stage primaryStage;
    private Scene mainScene;

    // Header Controls
    private Label filePathLabel;
    private ProgressBar progressBar;
    private Label progressStatusLabel;
    private Button loadFileButton;
    private ComboBox<String> activeUserComboBox;
    private ComboBox<ThemeManager.Theme> themeComboBox;

    // State
    private User activeUser;

    // Tab controllers
    private EventsViewController eventsViewController;
    private UsersViewController usersViewController;
    private TabPane mainTabPane;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Guess Market - Exercise 2");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        // Top Header
        root.setTop(createHeader());

        // Controllers for Events and Users tabs
        eventsViewController = new EventsViewController(manager, this);
        usersViewController = new UsersViewController(manager, this);

        // Center TabPane (Slide 1 & Slide 2 Tabs: Events | Users)
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainTabPane.getStyleClass().add("main-tab-pane");

        Tab eventsTab = new Tab("Events", eventsViewController.getRoot());
        Tab usersTab = new Tab("Users", usersViewController.getRoot());

        mainTabPane.getTabs().addAll(eventsTab, usersTab);
        root.setCenter(mainTabPane);

        // Scene setup
        mainScene = new Scene(root, 1280, 850);
        ThemeManager.applyTheme(mainScene, ThemeManager.Theme.LIGHT);

        stage.setScene(mainScene);
        stage.setMinWidth(950);
        stage.setMinHeight(650);
        stage.show();
    }

    // =========================================================================
    // HEADER (Title, Load File Button, File Path, ProgressBar, Act As, Skin)
    // =========================================================================

    private VBox createHeader() {
        VBox header = new VBox(8);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.getStyleClass().add("app-header");

        // Title row (Slide 1 & 2: Guess Market)
        Label appTitle = new Label("Guess Market");
        appTitle.getStyleClass().add("app-title");

        HBox titleBox = new HBox(appTitle);
        titleBox.setAlignment(Pos.CENTER);

        // Controls row
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Load File Button (Slide 1 & 2)
        loadFileButton = new Button("Load File");
        loadFileButton.getStyleClass().add("btn-load-file");
        loadFileButton.setOnAction(e -> handleLoadFile());

        // File path display (Slide 1 & 2: Currently Loaded File path)
        filePathLabel = new Label("No XML file currently loaded");
        filePathLabel.getStyleClass().add("file-path-display");
        filePathLabel.setTooltip(new Tooltip("Currently loaded file path"));

        // Progress indicators (Task & ProgressBar)
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(160);
        progressBar.setVisible(false);
        progressBar.getStyleClass().add("load-progress-bar");

        progressStatusLabel = new Label("");
        progressStatusLabel.getStyleClass().add("progress-status-label");
        progressStatusLabel.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Act As User selector
        Label actAsLabel = new Label("Act as:");
        actAsLabel.getStyleClass().add("act-as-label");

        activeUserComboBox = new ComboBox<>();
        activeUserComboBox.setPromptText("Select User");
        activeUserComboBox.setPrefWidth(160);
        activeUserComboBox.getStyleClass().add("active-user-combo");
        activeUserComboBox.setOnAction(e -> {
            String selectedName = activeUserComboBox.getValue();
            if (selectedName != null) {
                // Parse user name (may include [MM] or balance suffix)
                String rawName = selectedName.contains(" (") ? selectedName.substring(0, selectedName.indexOf(" (")) : selectedName;
                activeUser = manager.getUserByName(rawName);
                if (eventsViewController != null) eventsViewController.refresh();
            }
        });

        // Skin / Theme Switcher (Bonus #1)
        Label themeLabel = new Label("Skin:");
        themeLabel.getStyleClass().add("act-as-label");

        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll(ThemeManager.Theme.values());
        themeComboBox.setValue(ThemeManager.Theme.LIGHT);
        themeComboBox.setPrefWidth(130);
        themeComboBox.getStyleClass().add("theme-combo");
        themeComboBox.setOnAction(e -> {
            ThemeManager.Theme selected = themeComboBox.getValue();
            if (selected != null && mainScene != null) {
                ThemeManager.applyTheme(mainScene, selected);
            }
        });

        toolbar.getChildren().addAll(
                loadFileButton,
                filePathLabel,
                progressBar,
                progressStatusLabel,
                spacer,
                actAsLabel,
                activeUserComboBox,
                themeLabel,
                themeComboBox
        );

        header.getChildren().addAll(titleBox, toolbar);
        return header;
    }

    // =========================================================================
    // XML FILE LOADER WITH TASK & PROGRESSBAR
    // =========================================================================

    private void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Guess Market Exercise 2 XML File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        // Pre-select current directory
        File initialDir = new File(System.getProperty("user.dir"));
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) return;

        loadFileWithTask(selectedFile);
    }

    public void loadFileWithTask(File file) {
        loadFileButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        progressStatusLabel.setVisible(true);
        progressStatusLabel.setText("Initializing loader...");

        Task<String> loadTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateProgress(0.10, 1.0);
                updateMessage("Reading XML file structure...");
                Thread.sleep(400);   // artificial delay – EX2 requirement: 1-2 sec total

                updateProgress(0.40, 1.0);
                updateMessage("Validating XML schema & parsing nodes...");
                Thread.sleep(400);   // artificial delay – step 2

                updateProgress(0.65, 1.0);
                updateMessage("Verifying users, initial balances & MM bindings...");
                Thread.sleep(400);   // artificial delay – step 3

                // Actual backend load (runs on background thread – UI stays responsive)
                String summary = manager.loadFile(file.getAbsolutePath());

                updateProgress(1.0, 1.0);
                updateMessage("Load complete!");
                Thread.sleep(300);   // brief pause so user reads "Load complete!"

                return summary;
            }
        };

        progressBar.progressProperty().bind(loadTask.progressProperty());
        progressStatusLabel.textProperty().bind(loadTask.messageProperty());

        loadTask.setOnSucceeded(e -> {
            loadFileButton.setDisable(false);
            filePathLabel.setText(file.getAbsolutePath());
            progressBar.progressProperty().unbind();
            progressStatusLabel.textProperty().unbind();
            progressBar.setVisible(false);
            progressStatusLabel.setVisible(false);

            refreshAll();

            showNotification("File Loaded Successfully", loadTask.getValue(), Alert.AlertType.INFORMATION);
        });

        loadTask.setOnFailed(e -> {
            loadFileButton.setDisable(false);
            progressBar.progressProperty().unbind();
            progressStatusLabel.textProperty().unbind();
            progressBar.setVisible(false);
            progressStatusLabel.setVisible(false);

            Throwable error = loadTask.getException();
            String errorMsg = error != null && error.getMessage() != null ? error.getMessage() : "Unknown parsing error";
            showNotification("Failed to Load XML File", errorMsg, Alert.AlertType.ERROR);
        });

        Thread loaderThread = new Thread(loadTask, "XMLLoaderThread");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    // =========================================================================
    // STATE & REFRESH COORDINATION
    // =========================================================================

    public void refreshAll() {
        // Refresh active user combo items
        activeUserComboBox.getItems().clear();
        if (manager.getUsers() != null) {
            for (User u : manager.getUsers()) {
                String label = String.format("%s ($%.2f)%s",
                        u.getName(), u.getCash(), u.isMarketMaker() ? " [MM]" : "");
                activeUserComboBox.getItems().add(label);
            }

            // Set default active user if none selected
            if (activeUser == null && !manager.getUsers().isEmpty()) {
                activeUser = manager.getUsers().get(0);
                activeUserComboBox.getSelectionModel().select(0);
            } else if (activeUser != null) {
                // Re-fetch updated reference
                User updated = manager.getUserByName(activeUser.getName());
                if (updated != null) {
                    activeUser = updated;
                    for (int i = 0; i < manager.getUsers().size(); i++) {
                        if (manager.getUsers().get(i).getName().equalsIgnoreCase(activeUser.getName())) {
                            activeUserComboBox.getSelectionModel().select(i);
                            break;
                        }
                    }
                }
            }
        }

        // Refresh views
        if (eventsViewController != null) {
            eventsViewController.refresh();
        }
        if (usersViewController != null) {
            usersViewController.refresh();
        }
    }

    public User getActiveUser() {
        return activeUser;
    }

    public MarketManager getManager() {
        return manager;
    }

    public void showNotification(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }

    // =========================================================================
    // MAIN ENTRY POINT
    // =========================================================================

    public static void main(String[] args) {
        launch(args);
    }
}
