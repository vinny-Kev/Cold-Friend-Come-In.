package com.coldfriend.core;

import com.coldfriend.assets.ModelCatalog;
import com.coldfriend.input.InputController;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.player.PlayerId;
import com.coldfriend.state.BackdropState;
import com.coldfriend.state.InventoryState;
import com.coldfriend.state.MainMenuState;
import com.coldfriend.state.PauseState;
import com.coldfriend.assets.IconCatalog;
import com.coldfriend.state.PlayOptions;
import com.coldfriend.state.ScreenNavigator;
import com.coldfriend.state.SettingsState;
import com.coldfriend.state.WorldPlayState;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.WorldData;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.FileLocator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalInt;
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
    private static final String SEED_KEY = "world.seed";
    private static final String SPAWN_KEY = "dev.spawn";
    private static final String TIME_KEY = "dev.time";
    private static final String SCENARIO_KEY = "dev.scenario";
    private static final String CREATURE_KEY = "dev.creature";
    private static final String FPS_KEY = "dev.showFps";
    private static final String AUTOSTART_KEY = "dev.autostart";
    private static final String SCREENSHOT_KEY = "dev.screenshot";
    private static final float SCREENSHOT_DELAY_SECONDS = 9f;
    private static final float QUIT_AFTER_SCREENSHOT_SECONDS = 1.5f;
    private static final Path CATALOG_FILE = Path.of(ASSET_FOLDER, "models", "catalog.properties");
    private static final Path ITEMS_FILE = Path.of("config", "items.properties");
    private static final Path ICONS_FILE = Path.of(ASSET_FOLDER, "ui", "icons.properties");

    private final GameConfig config;
    private final SettingsStore settingsStore;
    private GameSettings settings;
    private InputController inputController;
    private BackdropState backdropState;
    private MainMenuState mainMenuState;
    private SettingsState settingsState;
    private PauseState pauseState;
    private WorldPlayState worldState;
    private WorldData currentWorld;
    private ModelCatalog catalog;
    private ItemCatalog items;
    private IconCatalog icons;
    private InventoryState inventoryState;
    private ScreenshotAppState screenshot;
    private float developmentClock;
    private boolean screenshotTaken;

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
        setDisplayFps(config.optionalString(FPS_KEY).isPresent());
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

        backdropState = new BackdropState(config.backdropSettings(), settings);
        mainMenuState = new MainMenuState(this, inputController);
        settingsState = new SettingsState(this, inputController);
        pauseState = new PauseState(this, inputController);
        stateManager.attach(backdropState);
        stateManager.attach(mainMenuState);
        stateManager.attach(settingsState);
        stateManager.attach(pauseState);

        settingsState.setEnabled(false);
        pauseState.setEnabled(false);
        mainMenuState.setEnabled(true);
        LOG.info("Main menu ready");
        if (config.optionalString(AUTOSTART_KEY).isPresent()) {
            startNewGame();
        }
        config.optionalString(SCREENSHOT_KEY).ifPresent(this::prepareScreenshot);
    }

    /** Development only: saves one picture of the game's own frame to a file, then quits. */
    private void prepareScreenshot(String path) {
        Path target = Path.of(path).toAbsolutePath();
        screenshot = new ScreenshotAppState(target.getParent() + File.separator,
                target.getFileName().toString());
        screenshot.setIsNumbered(false);
        stateManager.attach(screenshot);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (screenshot == null) {
            return;
        }
        developmentClock += tpf;
        if (!screenshotTaken && developmentClock >= SCREENSHOT_DELAY_SECONDS) {
            screenshotTaken = true;
            screenshot.takeScreenshot();
        } else if (screenshotTaken && developmentClock >= SCREENSHOT_DELAY_SECONDS + QUIT_AFTER_SCREENSHOT_SECONDS) {
            stop();
        }
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
        // Settings are opened from the pause menu while a world is being played, and from the main menu otherwise.
        if (worldState != null) {
            pauseState.setEnabled(true);
        } else {
            mainMenuState.setEnabled(true);
        }
    }

    @Override
    public void showSettings() {
        mainMenuState.setEnabled(false);
        pauseState.setEnabled(false);
        settingsState.setEnabled(true);
    }

    @Override
    public void openInventory() {
        if (worldState == null || inventoryState != null) {
            return;
        }
        worldState.setPaused(true);
        inventoryState = new InventoryState(this, inputController, worldState.simulation(), PlayerId.LOCAL);
        stateManager.attach(inventoryState);
    }

    @Override
    public void closeInventory() {
        if (inventoryState != null) {
            stateManager.detach(inventoryState);
            inventoryState = null;
        }
        if (worldState != null) {
            worldState.setPaused(false);
        }
    }

    @Override
    public void beginPlacement(int slot) {
        closeInventory();
        if (worldState != null) {
            worldState.startPlacement(slot);
        }
    }

    @Override
    public void pauseGame() {
        if (worldState != null) {
            worldState.setPaused(true);
            pauseState.setEnabled(true);
        }
    }

    @Override
    public void resumeGame() {
        pauseState.setEnabled(false);
        if (worldState != null) {
            worldState.setPaused(false);
        }
    }

    @Override
    public void startNewGame() {
        // Wait one frame so the "Generating" message is drawn before the world build blocks the loop.
        enqueue(this::enterNewWorld);
    }

    private void enterNewWorld() {
        enterWorld(config.optionalString(SEED_KEY).map(WorldSeed::parse).orElseGet(WorldSeed::random), null,
                config.optionalString(SCENARIO_KEY).orElse(""));
    }

    @Override
    public void restartGame() {
        if (currentWorld == null) {
            return;
        }
        // Detach after this frame's update, not during it. A development script is not run a second time.
        enqueue(() -> {
            if (inventoryState != null) {
                stateManager.detach(inventoryState);
                inventoryState = null;
            }
            if (worldState != null) {
                stateManager.detach(worldState);
                worldState = null;
            }
            enterWorld(new WorldSeed(currentWorld.seed()), currentWorld, "");
        });
    }

    /**
     * @param seed the world's seed, for the log
     * @param existing a world to play again (it never changes, so a restart can reuse it), or null to generate one
     * @param scenario a development script, or empty
     */
    private void enterWorld(WorldSeed seed, WorldData existing, String scenario) {
        try {
            if (catalog == null) {
                catalog = new ModelCatalog(assetManager, CATALOG_FILE);
            }
            if (items == null) {
                items = ItemCatalog.load(ITEMS_FILE);
            }
            if (icons == null) {
                icons = new IconCatalog(assetManager, ICONS_FILE);
            }
            WorldData world = existing != null ? existing : WorldGenerator.generate(seed, WorldGenParameters.DEFAULT);
            mainMenuState.setEnabled(false);
            settingsState.setEnabled(false);
            pauseState.setEnabled(false);
            backdropState.setEnabled(false);
            worldState = new WorldPlayState(world, catalog, icons, items, this, new PlayOptions(
                    config.optionalString(SPAWN_KEY).orElse("cabin"),
                    startMinute(),
                    scenario,
                    config.optionalString(CREATURE_KEY).isPresent()));
            currentWorld = world;
            stateManager.attach(worldState);
        } catch (IOException | RuntimeException e) {
            LOG.error("Could not start a new game with seed {}", seed, e);
            showMainMenu();
        }
    }

    /** @return the start time from {@code dev.time}, or the normal new-game time if it is blank or not understood */
    private int startMinute() {
        int normal = PlayOptions.NORMAL.startMinute();
        return config.optionalString(TIME_KEY).map(value -> {
            OptionalInt parsed = PlayOptions.parseStartTime(value);
            if (parsed.isEmpty()) {
                LOG.warn("Unknown dev.time '{}'; starting at the normal time", value);
            }
            return parsed.orElse(normal);
        }).orElse(normal);
    }

    @Override
    public void leaveWorld() {
        currentWorld = null;
        if (inventoryState != null) {
            stateManager.detach(inventoryState);
            inventoryState = null;
        }
        pauseState.setEnabled(false);
        settingsState.setEnabled(false);
        if (worldState != null) {
            stateManager.detach(worldState);
            worldState = null;
        }
        backdropState.setEnabled(true);
        showMainMenu();
    }

    @Override
    public boolean saveAndApplySettings(GameSettings updated) {
        GameSettings previous = settings;
        settings = updated;
        backdropState.apply(updated);
        if (worldState != null) {
            worldState.apply(updated);
        }
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
}
