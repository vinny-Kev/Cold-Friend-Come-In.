package com.coldfriend.core;

import com.coldfriend.environment.BackdropSettings;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Read-only access to the game's tuning configuration (config/game.properties).
 * Missing or malformed required keys fail fast with a descriptive message.
 */
public final class GameConfig {

    private final Properties properties;

    private GameConfig(Properties properties) {
        this.properties = properties;
    }

    /**
     * Loads configuration from a properties file.
     *
     * @param file path to the configuration file
     * @return the loaded configuration
     * @throws IOException if the file cannot be read
     */
    public static GameConfig load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("configuration file not found");
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return new GameConfig(props);
    }

    /**
     * Creates a configuration from in-memory properties. Intended for tests.
     *
     * @param properties the configuration values
     * @return the configuration
     */
    public static GameConfig of(Properties properties) {
        return new GameConfig(properties);
    }

    public String getString(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing config key: " + key);
        }
        return value;
    }

    /**
     * @param key the key to look up
     * @return the value, or empty if the key is missing or blank
     */
    public Optional<String> optionalString(String key) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    public int getInt(String key) {
        String value = getString(key);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config key " + key + " is not an integer: " + value, e);
        }
    }

    public float getFloat(String key) {
        String value = getString(key);
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config key " + key + " is not a number: " + value, e);
        }
    }

    public boolean getBoolean(String key) {
        String value = getString(key).trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalStateException("Config key " + key + " is not true or false: " + value);
    }

    public Resolution getResolution(String key) {
        try {
            return Resolution.parse(getString(key));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Config key " + key + " is not a resolution: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the settings used when the player has no saved settings file.
     *
     * @return default settings from the {@code settings.default.*} keys
     */
    public GameSettings defaultSettings() {
        return new GameSettings(
                getInt("settings.default.masterVolume"),
                getInt("settings.default.musicVolume"),
                getInt("settings.default.sfxVolume"),
                getResolution("settings.default.resolution"),
                getBoolean("settings.default.fullscreen"),
                getBoolean("settings.default.vsync"),
                getIntensity("settings.default.fog"),
                getIntensity("settings.default.snow"),
                getBoolean("settings.default.cameraSway"));
    }

    /**
     * Builds the menu backdrop tuning from the {@code menu.*} keys.
     *
     * @return backdrop settings
     */
    public BackdropSettings backdropSettings() {
        return new BackdropSettings(
                getInt("menu.treeCount"),
                getInt("menu.snowParticleCount"),
                getFloat("menu.cameraDriftSpeed"),
                getFloat("menu.cameraDriftRadius"));
    }

    public Intensity getIntensity(String key) {
        try {
            return Intensity.parse(getString(key));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Config key " + key + " is not off, light, normal or heavy", e);
        }
    }
}
