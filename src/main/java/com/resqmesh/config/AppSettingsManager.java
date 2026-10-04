package com.resqmesh.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Service managing persistence, serialization, and retrieval of AppSettings to/from JSON.
 * Gracefully handles missing, unreadable, or malformed settings files by falling back to safe defaults.
 */
public class AppSettingsManager {

    public static final String DEFAULT_FILE_NAME = "resqmesh-settings.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File settingsFile;
    private AppSettings settings;

    public AppSettingsManager() {
        this(new File(DEFAULT_FILE_NAME));
    }

    public AppSettingsManager(File settingsFile) {
        if (settingsFile == null) {
            throw new IllegalArgumentException("Settings file cannot be null.");
        }
        this.settingsFile = settingsFile;
        this.settings = loadSettings();
    }

    /**
     * Loads settings from the target JSON file.
     * If the file does not exist or contains invalid JSON, returns a new AppSettings instance with default values.
     *
     * @return loaded or default AppSettings
     */
    public AppSettings loadSettings() {
        if (!settingsFile.exists() || !settingsFile.isFile()) {
            this.settings = new AppSettings();
            return this.settings;
        }

        try {
            String json = Files.readString(settingsFile.toPath(), StandardCharsets.UTF_8);
            if (json == null || json.trim().isEmpty()) {
                this.settings = new AppSettings();
                return this.settings;
            }

            AppSettings loaded = GSON.fromJson(json, AppSettings.class);
            if (loaded == null) {
                this.settings = new AppSettings();
            } else {
                // Ensure values are clamped/validated
                loaded.setBatteryDrainPerHop(loaded.getBatteryDrainPerHop());
                loaded.setReplaySpeed(loaded.getReplaySpeed());
                this.settings = loaded;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not read settings from " + settingsFile.getName() + ": " + e.getMessage());
            this.settings = new AppSettings();
        }

        return this.settings;
    }

    /**
     * Saves the provided settings to the target JSON file.
     *
     * @param newSettings settings to persist
     * @throws ConfigurationException if saving fails
     */
    public void saveSettings(AppSettings newSettings) throws ConfigurationException {
        if (newSettings == null) {
            throw new IllegalArgumentException("Cannot save null settings.");
        }

        this.settings = new AppSettings(newSettings);
        try {
            if (settingsFile.getParentFile() != null && !settingsFile.getParentFile().exists()) {
                settingsFile.getParentFile().mkdirs();
            }
            String json = GSON.toJson(this.settings);
            Files.writeString(settingsFile.toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to persist application settings to " + settingsFile.getAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Saves the current in-memory settings to disk.
     *
     * @throws ConfigurationException if saving fails
     */
    public void saveCurrentSettings() throws ConfigurationException {
        if (this.settings == null) {
            this.settings = new AppSettings();
        }
        saveSettings(this.settings);
    }

    /**
     * Resets application settings to factory defaults and persists the changes.
     *
     * @return the reset AppSettings instance
     * @throws ConfigurationException if saving fails
     */
    public AppSettings resetToDefaults() throws ConfigurationException {
        AppSettings defaults = new AppSettings();
        saveSettings(defaults);
        return defaults;
    }

    public AppSettings getSettings() {
        if (this.settings == null) {
            this.settings = new AppSettings();
        }
        return this.settings;
    }

    public File getSettingsFile() {
        return settingsFile;
    }
}
