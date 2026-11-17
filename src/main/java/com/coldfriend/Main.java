package com.coldfriend;

import com.coldfriend.core.AppSettingsFactory;
import com.coldfriend.core.GameApp;
import com.coldfriend.core.GameConfig;
import com.coldfriend.core.GameSettings;
import com.coldfriend.core.SettingsStore;
import com.jme3.system.AppSettings;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Process entry point: loads configuration and starts the game application. */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final Path CONFIG_FILE = Path.of("config", "game.properties");
    private static final Path SETTINGS_FILE = Path.of("config", "settings.properties");
    private static final int EXIT_CONFIG_ERROR = 1;

    private Main() {
    }

    /**
     * Starts the application.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        GameConfig config = loadConfig();
        SettingsStore settingsStore = new SettingsStore(SETTINGS_FILE, config.defaultSettings());
        GameSettings settings = settingsStore.load();
        AppSettings appSettings = AppSettingsFactory.create(config.getString("window.title"), settings);

        GameApp app = new GameApp(config, settingsStore, settings);
        app.setSettings(appSettings);
        LOG.info("Starting {} with settings {}", config.getString("window.title"), settings);
        app.start();
    }

    private static GameConfig loadConfig() {
        try {
            GameConfig config = GameConfig.load(CONFIG_FILE);
            // Read every start-up value now, so a typo fails here with a clear message instead of later.
            config.getString("window.title");
            config.defaultSettings();
            config.backdropSettings();
            return config;
        } catch (IOException | IllegalStateException e) {
            LOG.error("Cannot load configuration from {}: {}", CONFIG_FILE.toAbsolutePath(), e.getMessage(), e);
            System.exit(EXIT_CONFIG_ERROR);
            throw new IllegalStateException("unreachable", e);
        }
    }
}
