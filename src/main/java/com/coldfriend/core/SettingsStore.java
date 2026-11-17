package com.coldfriend.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and saves {@link GameSettings} as a properties file (config/settings.properties).
 * A missing or unreadable file yields defaults; invalid individual values fall back to their default.
 */
public final class SettingsStore {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsStore.class);

    private static final String KEY_MASTER_VOLUME = "masterVolume";
    private static final String KEY_MUSIC_VOLUME = "musicVolume";
    private static final String KEY_SFX_VOLUME = "sfxVolume";
    private static final String KEY_RESOLUTION = "resolution";
    private static final String KEY_FULLSCREEN = "fullscreen";
    private static final String KEY_VSYNC = "vsync";

    private final Path file;
    private final GameSettings defaults;

    /**
     * @param file the settings file location
     * @param defaults settings used when the file is missing or a value is invalid
     */
    public SettingsStore(Path file, GameSettings defaults) {
        this.file = file;
        this.defaults = defaults;
    }

    /**
     * Reads settings from disk.
     *
     * @return the stored settings, or defaults if none can be read
     */
    public GameSettings load() {
        if (!Files.isRegularFile(file)) {
            LOG.info("No settings file at {}; using defaults", file.toAbsolutePath());
            return defaults;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            LOG.warn("Cannot read settings file {}; using defaults", file.toAbsolutePath(), e);
            return defaults;
        }
        return new GameSettings(
                readVolume(props, KEY_MASTER_VOLUME, defaults.masterVolume()),
                readVolume(props, KEY_MUSIC_VOLUME, defaults.musicVolume()),
                readVolume(props, KEY_SFX_VOLUME, defaults.sfxVolume()),
                readResolution(props),
                readBoolean(props, KEY_FULLSCREEN, defaults.fullscreen()),
                readBoolean(props, KEY_VSYNC, defaults.vsync()));
    }

    /**
     * Writes settings to disk, creating the parent folder if needed.
     *
     * @param settings the settings to store
     * @throws IOException if the file cannot be written
     */
    public void save(GameSettings settings) throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_MASTER_VOLUME, Integer.toString(settings.masterVolume()));
        props.setProperty(KEY_MUSIC_VOLUME, Integer.toString(settings.musicVolume()));
        props.setProperty(KEY_SFX_VOLUME, Integer.toString(settings.sfxVolume()));
        props.setProperty(KEY_RESOLUTION, settings.resolution().toString());
        props.setProperty(KEY_FULLSCREEN, Boolean.toString(settings.fullscreen()));
        props.setProperty(KEY_VSYNC, Boolean.toString(settings.vsync()));

        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        // Write a temporary file first, so a crash mid-write cannot leave a half-written settings file.
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            props.store(writer, "Cold Friend, Come In - player settings (written by the game)");
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        LOG.info("Saved settings to {}", file.toAbsolutePath());
    }

    private static int readVolume(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOG.warn("Invalid volume for {}: '{}'; using {}", key, value, fallback);
            return fallback;
        }
    }

    private Resolution readResolution(Properties props) {
        String value = props.getProperty(KEY_RESOLUTION);
        if (value == null) {
            return defaults.resolution();
        }
        try {
            return Resolution.parse(value);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid resolution '{}'; using {}", value, defaults.resolution());
            return defaults.resolution();
        }
    }

    private static boolean readBoolean(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (!"true".equalsIgnoreCase(trimmed) && !"false".equalsIgnoreCase(trimmed)) {
            LOG.warn("Invalid boolean for {}: '{}'; using {}", key, value, fallback);
            return fallback;
        }
        return Boolean.parseBoolean(trimmed);
    }
}
