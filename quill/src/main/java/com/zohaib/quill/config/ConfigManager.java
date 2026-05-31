package com.zohaib.quill.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * ConfigManager — Responsible for loading, accessing, and persisting all application
 * configuration from the {@code config.properties} file. It first looks for a writable
 * copy next to the running JAR (so users can edit it without touching the classpath),
 * and falls back to the bundled classpath resource for first-run defaults.
 *
 * <p>Settings managed:
 * <ul>
 *   <li>GROQ_API_KEY — Groq API key (never hardcoded)</li>
 *   <li>THEME — "dark" or "light"</li>
 *   <li>FONT_SIZE — editor font size in points</li>
 *   <li>TAB_SIZE — number of spaces per tab (2 or 4)</li>
 * </ul>
 *
 * @author Zohaib
 * @version 1.0
 */
public class ConfigManager {

    // Keys
    public static final String KEY_API_KEY   = "GROQ_API_KEY";
    public static final String KEY_THEME     = "THEME";
    public static final String KEY_FONT_SIZE = "FONT_SIZE";
    public static final String KEY_TAB_SIZE  = "TAB_SIZE";
    public static final String KEY_UI_SCALE  = "UI_SCALE";

    // Defaults
    private static final String DEFAULT_THEME     = "dark";
    private static final int    DEFAULT_FONT_SIZE = 14;
    private static final int    DEFAULT_TAB_SIZE  = 4;
    private static final String DEFAULT_UI_SCALE  = "auto";

    private final Properties properties = new Properties();
    private Path configFilePath;

    /**
     * Constructs a ConfigManager and immediately loads configuration.
     * Searches for {@code config.properties} next to the JAR first, then
     * falls back to the classpath resource.
     */
    public ConfigManager() {
        resolveConfigPath();
        load();
    }

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    /**
     * Determines the path to the external {@code config.properties} file.
     * Uses the JAR's parent directory so users can edit the file easily.
     */
    private void resolveConfigPath() {
        try {
            Path jarDir = Path.of(
                ConfigManager.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
            ).getParent();
            configFilePath = jarDir != null
                ? jarDir.resolve("config.properties")
                : Path.of("config.properties");
        } catch (Exception e) {
            configFilePath = Path.of("config.properties");
        }
    }

    /**
     * Loads properties from the external file (if it exists) or from the
     * classpath resource. If neither exists, applies built-in defaults.
     */
    private void load() {
        // Try external file first
        if (Files.exists(configFilePath)) {
            try (InputStream in = Files.newInputStream(configFilePath)) {
                properties.load(in);
                return;
            } catch (IOException e) {
                System.err.println("[ConfigManager] Could not read external config: " + e.getMessage());
            }
        }

        // Fallback: classpath resource bundled in JAR
        try (InputStream in = ConfigManager.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                properties.load(in);
                // Write out so user can easily find and edit it
                save();
                return;
            }
        } catch (IOException e) {
            System.err.println("[ConfigManager] Could not read classpath config: " + e.getMessage());
        }

        // Last resort: apply defaults
        applyDefaults();
    }

    /** Applies hard-coded defaults to the Properties object. */
    private void applyDefaults() {
        properties.setProperty(KEY_API_KEY,   "your_key_here");
        properties.setProperty(KEY_THEME,     DEFAULT_THEME);
        properties.setProperty(KEY_FONT_SIZE, String.valueOf(DEFAULT_FONT_SIZE));
        properties.setProperty(KEY_TAB_SIZE,  String.valueOf(DEFAULT_TAB_SIZE));
        properties.setProperty(KEY_UI_SCALE,  DEFAULT_UI_SCALE);
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    /**
     * Saves the current properties to the external {@code config.properties} file.
     */
    public void save() {
        try (OutputStream out = Files.newOutputStream(
                configFilePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            properties.store(out, "Quill Configuration — edit this file to customise your editor");
        } catch (IOException e) {
            System.err.println("[ConfigManager] Could not save config: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    /**
     * Returns the raw Groq API key string, falling back to the GROQ_API_KEY env variable if not configured in the properties file.
     *
     * @return API key, or "your_key_here" if not set
     */
    public String getApiKey() {
        String key = properties.getProperty(KEY_API_KEY, "your_key_here");
        if (key == null || key.trim().isEmpty() || "your_key_here".equals(key)) {
            String envKey = System.getenv("GROQ_API_KEY");
            if (envKey != null && !envKey.trim().isEmpty()) {
                return envKey.trim();
            }
        }
        return key;
    }

    /**
     * Returns {@code true} if a real API key has been configured.
     *
     * @return true when the key is non-blank and not the placeholder
     */
    public boolean isApiKeyConfigured() {
        String key = getApiKey().trim();
        return !key.isEmpty() && !key.equals("your_key_here");
    }

    /**
     * Returns the configured theme name ("dark" or "light").
     *
     * @return theme string
     */
    public String getTheme() {
        return properties.getProperty(KEY_THEME, DEFAULT_THEME);
    }

    /**
     * Returns the configured editor font size.
     *
     * @return font size in points
     */
    public int getFontSize() {
        try {
            return Integer.parseInt(properties.getProperty(KEY_FONT_SIZE,
                String.valueOf(DEFAULT_FONT_SIZE)));
        } catch (NumberFormatException e) {
            return DEFAULT_FONT_SIZE;
        }
    }

    /**
     * Returns the configured tab size.
     *
     * @return number of spaces per tab
     */
    public int getTabSize() {
        try {
            return Integer.parseInt(properties.getProperty(KEY_TAB_SIZE,
                String.valueOf(DEFAULT_TAB_SIZE)));
        } catch (NumberFormatException e) {
            return DEFAULT_TAB_SIZE;
        }
    }

    /**
     * Returns the configured UI scale factor ("auto", "1.0", "1.5", "2.0", etc.).
     *
     * @return UI scale setting
     */
    public String getUIScale() {
        return properties.getProperty(KEY_UI_SCALE, DEFAULT_UI_SCALE);
    }

    // -----------------------------------------------------------------------
    // Setters (also persists immediately)
    // -----------------------------------------------------------------------

    /**
     * Sets and persists the Groq API key.
     *
     * @param key the API key value
     */
    public void setApiKey(String key) {
        properties.setProperty(KEY_API_KEY, key != null ? key.trim() : "");
        save();
    }

    /**
     * Sets and persists the theme.
     *
     * @param theme "dark" or "light"
     */
    public void setTheme(String theme) {
        properties.setProperty(KEY_THEME, theme);
        save();
    }

    /**
     * Sets and persists the font size.
     *
     * @param size font size in points
     */
    public void setFontSize(int size) {
        properties.setProperty(KEY_FONT_SIZE, String.valueOf(size));
        save();
    }

    /**
     * Sets and persists the tab size.
     *
     * @param size number of spaces per tab
     */
    public void setTabSize(int size) {
        properties.setProperty(KEY_TAB_SIZE, String.valueOf(size));
        save();
    }

    /**
     * Sets and persists the UI scale factor.
     *
     * @param scale "auto", "1.0", "1.5", "2.0", etc.
     */
    public void setUIScale(String scale) {
        properties.setProperty(KEY_UI_SCALE, scale != null ? scale.trim() : DEFAULT_UI_SCALE);
        save();
    }

    /**
     * Returns the absolute path of the config file used at runtime.
     *
     * @return config file path as string
     */
    public String getConfigFilePath() {
        return configFilePath.toAbsolutePath().toString();
    }
}
