package com.coldfriend.core;

import com.coldfriend.environment.BackdropSettings;
import com.coldfriend.input.InputController;
import com.coldfriend.state.BackdropState;
import com.coldfriend.state.MainMenuState;
import com.coldfriend.state.ScreenNavigator;
import com.coldfriend.state.SettingsState;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application root. Owns the input controller, the screen states, and the player settings. Start-up order:
 * input, then backdrop, then menus; the main menu is the only screen enabled at first.
 */
public final class GameApp extends SimpleApplication implements ScreenNavigator {

    private static final Logger LOG = LoggerFactory.getLogger(GameApp.class);
    private static final String ASSET_FOLDER = "assets";
    private static final String TITLE_KEY = "window.title";

    private final GameConfig config;
    private final SettingsStore settingsStore;
    private GameSettings settings;
    private InputController inputController;
    private BackdropState backdropState;
    private MainMenuState mainMenuState;
    private SettingsState settingsState;

    /**
     * @param config tuning configuration
     * @param settingsStore where player settings are saved
     * @param settings the settings in effect at start-up
     */
    public GameApp(GameConfig config, SettingsStore settingsStore, GameSettings settings) {
        this.config = config;
        this.settingsStore = settingsStore;
        this.settings = settings;
        setShowSettings(false);
        setDisplayStatView(false);
        setDisplayFps(false);
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator(ASSET_FOLDER, FileLocator.class);
        // jME maps Esc to quit and turns on a free-fly camera that hides the cursor. The menus use Esc for Back
        // and need the cursor, so both defaults are removed here.
        inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
        getFlyByCamera().setEnabled(false);

        inputController = new InputController(inputManager);
        inputController.attach();

        backdropState = new BackdropState(backdropSettings());
        mainMenuState = new MainMenuState(this, inputController);
        settingsState = new SettingsState(this, inputController);
        stateManager.attach(backdropState);
        stateManager.attach(mainMenuState);
        stateManager.attach(settingsState);

        settingsState.setEnabled(false);
        mainMenuState.setEnabled(true);
        LOG.info("Main menu ready");
    }

    @Override
    public void destroy() {
        if (inputController != null) {
            inputController.detach();
        }
        super.destroy();
    }

    @Override
    public GameSettings currentSettings() {
        return settings;
    }

    @Override
    public void showMainMenu() {
        settingsState.setEnabled(false);
        mainMenuState.setEnabled(true);
    }

    @Override
    public void showSettings() {
        mainMenuState.setEnabled(false);
        settingsState.setEnabled(true);
    }

    @Override
    public boolean saveAndApplySettings(GameSettings updated) {
        GameSettings previous = settings;
        settings = updated;
        boolean saved = true;
        try {
            settingsStore.save(updated);
        } catch (IOException e) {
            saved = false;
            LOG.error("Could not save settings to disk; they remain active for this session", e);
        }
        if (previous.requiresDisplayRestart(updated)) {
            LOG.info("Display settings changed; restarting the display with {}", updated);
            setSettings(AppSettingsFactory.create(config.getString(TITLE_KEY), updated));
            restart();
        }
        return saved;
    }

    @Override
    public void quit() {
        LOG.info("Quit requested");
        stop();
    }

    private BackdropSettings backdropSettings() {
        return new BackdropSettings(
                config.getInt("menu.treeCount"),
                config.getInt("menu.snowParticleCount"),
                config.getFloat("menu.cameraDriftSpeed"),
                config.getFloat("menu.cameraDriftRadius"));
    }
}
