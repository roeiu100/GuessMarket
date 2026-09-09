package ui;

import javafx.scene.Scene;

/**
 * Manages UI themes/skins for the Guess Market JavaFX application.
 * Supports multiple skins: Light Modern (default), Dark Slate, and Emerald Classic.
 */
public class ThemeManager {

    public enum Theme {
        LIGHT("Light Modern", "/ui/theme-light.css"),
        DARK("Dark Slate", "/ui/theme-dark.css"),
        EMERALD("Emerald Classic", "/ui/theme-emerald.css");

        private final String displayName;
        private final String cssPath;

        Theme(String displayName, String cssPath) {
            this.displayName = displayName;
            this.cssPath = cssPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssPath() {
            return cssPath;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static Theme currentTheme = Theme.LIGHT;

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void applyTheme(Scene scene, Theme theme) {
        if (scene == null) return;
        currentTheme = theme;

        scene.getStylesheets().clear();

        // Always add base style first
        String baseCss = ThemeManager.class.getResource("/ui/style.css") != null ?
                ThemeManager.class.getResource("/ui/style.css").toExternalForm() : null;
        if (baseCss != null) {
            scene.getStylesheets().add(baseCss);
        }

        // Add theme specific css
        String themeCss = ThemeManager.class.getResource(theme.getCssPath()) != null ?
                ThemeManager.class.getResource(theme.getCssPath()).toExternalForm() : null;
        if (themeCss != null) {
            scene.getStylesheets().add(themeCss);
        }
    }
}
