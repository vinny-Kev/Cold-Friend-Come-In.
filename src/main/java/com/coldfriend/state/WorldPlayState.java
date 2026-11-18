package com.coldfriend.state;

import com.coldfriend.assets.PropCatalog;
import com.coldfriend.audio.Ambience;
import com.coldfriend.audio.Footsteps;
import com.coldfriend.core.GameSettings;
import com.coldfriend.input.GameplayInput;
import com.coldfriend.interaction.ActionResult;
import com.coldfriend.interaction.Interactable;
import com.coldfriend.interaction.InteractableKind;
import com.coldfriend.interaction.InteractableRef;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemDefinition;
import com.coldfriend.item.ItemStack;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerMovement;
import com.coldfriend.player.StepCounter;
import com.coldfriend.player.PlayerState;
import com.coldfriend.survival.HearthStatus;
import com.coldfriend.creature.CreatureStatus;
import com.coldfriend.assets.IconCatalog;
import com.coldfriend.ui.ShadowText;
import com.coldfriend.world.render.CreatureDebugView;
import com.coldfriend.ui.HotbarView;
import com.coldfriend.ui.SurvivalHud;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.SurvivalActions;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.items.PlacementCheck;
import com.coldfriend.world.items.PlacementValidator;
import com.coldfriend.world.items.WorldItem;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.render.CarView;
import com.coldfriend.world.render.PlacementPreview;
import com.coldfriend.world.render.WorldItemsView;
import com.coldfriend.world.render.WorldScene;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.Camera;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The first-person game screen. Builds the drawn world, runs the {@link WorldSimulation}, and presents it: it
 * reads input, hands it to the simulation, and moves the camera to follow the player. Every gameplay decision
 * is in the simulation, never here: dropping, picking up and placing are requests the simulation validates.
 * Placement mode is presentation too: it shows a preview and sends the confirmed spot to the simulation. Owns its
 * input mappings and releases them when disabled or paused.
 */
public final class WorldPlayState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(WorldPlayState.class);
    private static final String FONT_PATH = "Interface/Fonts/Default.fnt";
    private static final ColorRGBA HUD_COLOR = new ColorRGBA(0.95f, 0.95f, 1f, 0.9f);
    private static final ColorRGBA BAD_COLOR = new ColorRGBA(1f, 0.45f, 0.4f, 1f);
    private static final ColorRGBA GOOD_COLOR = new ColorRGBA(0.6f, 1f, 0.65f, 1f);
    private static final float FOV_DEGREES = 70f;
    private static final float NEAR = 0.1f;
    private static final float FAR = 600f;
    private static final float EYE_HEIGHT = 1.65f;
    /** Eye height while seated in the car, below the roof line. */
    private static final float DRIVER_EYE_HEIGHT = 0.95f;
    private static final float FOG_DENSITY = 1.0f;
    private static final float FOG_DISTANCE = 140f;
    /** Distance walked between two footsteps, in metres. Tuning value; sprinting uses the faster sound. */
    private static final float STEP_STRIDE = 2.5f;
    private static final float PROMPT_SCALE = 1.4f;
    private static final float PROMPT_BOTTOM = 120f;
    private static final float CROSSHAIR_SCALE = 1.2f;
    private static final String CROSSHAIR = "+";
    private static final float HUD_SCALE = 1.1f;
    private static final float HUD_MARGIN = 20f;
    private static final float TOAST_SECONDS = 2.8f;
    private static final float TOAST_BOTTOM = 180f;
    private static final float ROTATE_STEP = (float) (Math.PI / 12.0);
    private static final float AIM_STEP = 0.1f;
    private static final float AIM_START = 0.3f;
    private static final float SCENARIO_DELAY = 3f;
    private static final float SCENARIO_LOOK_DOWN = -1.0f;
    private static final float SCENARIO_PLACE_LOOK = -0.7f;
    private static final int NO_SLOT = -1;
    private static final float RESTART_DELAY = 1.5f;

    private final WorldData world;
    private final PropCatalog catalog;
    private final ItemCatalog itemCatalog;
    private final ScreenNavigator navigator;
    private final PlayOptions options;
    private WorldSimulation simulation;
    private WorldScene scene;
    private WorldItemsView itemsView;
    private PlacementPreview preview;
    private GameplayInput gameplayInput;
    private FilterPostProcessor fogProcessor;
    private FogFilter fog;
    private PlayerState player;
    private ShadowText prompt;
    private ShadowText crosshair;
    private ShadowText selectedText;
    private ShadowText toast;
    private SurvivalHud survivalHud;
    private final IconCatalog icons;
    private HotbarView hotbar;
    private CarView carView;
    private float hotbarWidth = -1f;
    private float hotbarHeight = -1f;
    private CreatureDebugView creatureView;
    private ShadowText creatureText;
    private SkyLighting sky;
    private boolean paused;
    private Footsteps footsteps;
    private Ambience ambience;
    private final StepCounter steps = new StepCounter(STEP_STRIDE);
    private float lastX;
    private float lastZ;
    private int placingSlot = NO_SLOT;
    private float placementYaw;
    private float toastLeft;
    private float scenarioClock;
    private float deadSeconds;
    private boolean scenarioDone;
    private float scenarioLook;

    /**
     * @param world the generated world to play in
     * @param catalog how to draw the world's scenery and items
     * @param itemCatalog the item definitions
     * @param navigator used to pause, open the inventory and leave the world
     * @param options where the player starts and the time of day
     */
    public WorldPlayState(WorldData world, PropCatalog catalog, IconCatalog icons, ItemCatalog itemCatalog,
            ScreenNavigator navigator, PlayOptions options) {
        this.options = options;
        this.world = world;
        this.catalog = catalog;
        this.icons = icons;
        this.itemCatalog = itemCatalog;
        this.navigator = navigator;
    }

    /** @return the simulation this screen presents; the inventory screen reads and changes it */
    public WorldSimulation simulation() {
        return simulation;
    }

    /**
     * Stops or restarts the game without leaving the world. While paused the simulation stands still and the
     * mouse is free for menus.
     *
     * @param pause true to pause
     */
    public void setPaused(boolean pause) {
        if (pause == paused) {
            return;
        }
        paused = pause;
        if (pause) {
            gameplayInput.detach();
            prompt.setText("");
            if (ambience != null) {
                ambience.stop();
            }
        } else {
            simulation.resetFrameTiming();
            gameplayInput.attach();
            gameplayInput.clear();
            if (ambience != null) {
                ambience.start();
            }
        }
    }

    /**
     * Enters placement mode for the item in an inventory slot. Does nothing if the slot is empty or holds
     * something that cannot be placed.
     *
     * @param slot the inventory slot
     */
    public void startPlacement(int slot) {
        Optional<ItemStack> held = simulation.inventory(PlayerId.LOCAL).slot(slot);
        if (held.isEmpty() || !itemCatalog.get(held.get().item()).canPlace()) {
            showToast("That cannot be placed.");
            return;
        }
        ItemDefinition definition = itemCatalog.get(held.get().item());
        placingSlot = slot;
        placementYaw = player.yaw();
        preview.show(catalog.itemModel(definition.modelKey()), definition.placeRadius());
        simulation.inventory(PlayerId.LOCAL).select(slot);
    }

    /**
     * Applies settings that change while playing.
     *
     * @param updated the settings now in effect
     */
    public void apply(GameSettings updated) {
        if (footsteps != null) {
            footsteps.setVolume(updated.effectVolume());
        }
        if (ambience != null) {
            ambience.setVolume(updated.effectVolume());
        }
        if (fog == null) {
            return;
        }
        float factor = updated.fog().factor();
        fog.setFogDensity(FOG_DENSITY * factor);
        fog.setEnabled(factor > 0f);
    }

    @Override
    protected void initialize(Application app) {
        SimpleApplication simple = (SimpleApplication) app;
        simulation = new WorldSimulation(world, itemCatalog, options.startMinute());
        player = simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, options.spawn()));
        scene = new WorldScene(world, catalog, app.getAssetManager());
        simulation.car().ifPresent(car -> carView = scene.addCar(car, world.terrain().heightAt(car.x(), car.z()),
                app.getAssetManager()));
        sky = new SkyLighting(simulation.clockParameters());
        sky.addTo(scene.root());
        itemsView = new WorldItemsView(catalog, itemCatalog);
        scene.root().attachChild(itemsView.root());
        preview = new PlacementPreview(app.getAssetManager());
        scene.root().attachChild(preview.root());
        try {
            footsteps = new Footsteps(app.getAssetManager(), scene.root());
        } catch (RuntimeException e) {
            LOG.warn("Footsteps are unavailable; the game continues without them: {}", e.getMessage());
        }
        try {
            ambience = new Ambience(scene.root());
        } catch (RuntimeException e) {
            LOG.warn("Wind is unavailable; the game continues without it: {}", e.getMessage());
        }
        gameplayInput = new GameplayInput(simple.getInputManager());
        fog = new FogFilter(sky.update(simulation.minuteOfDay()), FOG_DENSITY, FOG_DISTANCE);
        fogProcessor = new FilterPostProcessor(app.getAssetManager());
        fogProcessor.addFilter(fog);
        apply(navigator.currentSettings());
        BitmapFont font = app.getAssetManager().loadFont(FONT_PATH);
        prompt = hudText(font, PROMPT_SCALE);
        crosshair = hudText(font, CROSSHAIR_SCALE);
        crosshair.setText(CROSSHAIR);
        selectedText = hudText(font, HUD_SCALE);
        toast = hudText(font, PROMPT_SCALE);
        survivalHud = new SurvivalHud(font);
        hotbar = new HotbarView(app.getAssetManager(), font, icons);
        if (options.showCreature()) {
            creatureView = new CreatureDebugView(app.getAssetManager());
            scene.root().attachChild(creatureView.root());
            creatureText = hudText(font, HUD_SCALE);
        }
        LOG.info("World ready to play: seed={} props={} items={} player at {},{} cabin at {},{}", world.seed(),
                world.placements().size(), simulation.worldItems().size(), player.x(), player.z(),
                world.cabin().footprint().x(), world.cabin().footprint().z());
    }

    @Override
    protected void onEnable() {
        SimpleApplication simple = (SimpleApplication) getApplication();
        simple.getRootNode().attachChild(scene.root());
        simple.getGuiNode().attachChild(prompt);
        simple.getGuiNode().attachChild(crosshair);
        simple.getGuiNode().attachChild(selectedText);
        simple.getGuiNode().attachChild(toast);
        simple.getGuiNode().attachChild(survivalHud.root());
        simple.getGuiNode().attachChild(hotbar.root());
        if (creatureText != null) {
            simple.getGuiNode().attachChild(creatureText);
        }
        survivalHud.refreshSoon();
        updateLighting();
        simple.getViewPort().addProcessor(fogProcessor);
        Camera camera = simple.getCamera();
        camera.setFrustumPerspective(FOV_DEGREES, (float) camera.getWidth() / camera.getHeight(), NEAR, FAR);
        paused = false;
        simulation.resetFrameTiming();
        if (ambience != null) {
            ambience.start();
        }
        steps.reset();
        lastX = player.x();
        lastZ = player.z();
        gameplayInput.attach();
        followPlayer(camera);
        centreHud(camera);
    }

    @Override
    protected void onDisable() {
        SimpleApplication simple = (SimpleApplication) getApplication();
        gameplayInput.detach();
        simple.getViewPort().removeProcessor(fogProcessor);
        prompt.removeFromParent();
        crosshair.removeFromParent();
        selectedText.removeFromParent();
        toast.removeFromParent();
        survivalHud.root().removeFromParent();
        hotbar.root().removeFromParent();
        if (creatureText != null) {
            creatureText.removeFromParent();
        }
        scene.detach();
        if (ambience != null) {
            ambience.stop();
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (footsteps != null) {
            footsteps.cleanup();
            footsteps = null;
        }
        if (ambience != null) {
            ambience.cleanup();
            ambience = null;
        }
        simulation = null;
        scene = null;
        itemsView = null;
        preview = null;
        gameplayInput = null;
        fogProcessor = null;
        fog = null;
        player = null;
        prompt = null;
        crosshair = null;
        selectedText = null;
        toast = null;
        survivalHud = null;
        creatureView = null;
        creatureText = null;
        sky = null;
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || paused) {
            return;
        }
        Camera camera = ((SimpleApplication) getApplication()).getCamera();
        if (placingSlot != NO_SLOT && simulation.survival(PlayerId.LOCAL).dead()) {
            endPlacement();
        }
        if (placingSlot != NO_SLOT) {
            handlePlacementInput();
        } else if (!handleGameplayKeys()) {
            return;
        }
        PlayerInput input = gameplayInput.consume();
        if (scenarioLook != 0f) {
            input = new PlayerInput(input.forward(), input.strafe(), input.turn(), input.look() + scenarioLook,
                    input.sprint());
            scenarioLook = 0f;
        }
        simulation.update(tpf, Map.of(PlayerId.LOCAL, input));
        float carAlpha = simulation.interpolation();
        simulation.vehicle().ifPresent(car -> {
            float carX = interpolate(car.previousX(), car.x(), carAlpha);
            float carZ = interpolate(car.previousZ(), car.z(), carAlpha);
            float yawDelta = (float) Math.atan2(Math.sin(car.yaw() - car.previousYaw()),
                    Math.cos(car.yaw() - car.previousYaw()));
            float carYaw = car.previousYaw() + yawDelta * carAlpha;
            carView.place(carX, carZ, carYaw, world.terrain().heightAt(carX, carZ));
        });
        deadSeconds = simulation.survival(PlayerId.LOCAL).dead() ? deadSeconds + tpf : 0f;
        playFootsteps(input);
        scene.setCabinDoorOpen(simulation.isCabinDoorOpen());
        scene.setCabinFireLevel((float) simulation.hearthLevel());
        scene.animate(tpf);
        updateLighting();
        itemsView.sync(simulation.worldItems());
        followPlayer(camera);
        if (placingSlot != NO_SLOT) {
            updatePlacementPreview(camera);
        } else {
            showPrompt(camera);
        }
        updateHud(tpf, camera);
        hotbar.refresh(simulation.inventory(PlayerId.LOCAL));
        if (camera.getWidth() != hotbarWidth || camera.getHeight() != hotbarHeight) {
            hotbarWidth = camera.getWidth();
            hotbarHeight = camera.getHeight();
            hotbar.layout(hotbarWidth, hotbarHeight);
        }
        if (creatureView != null) {
            showCreature(camera);
        }
        if (survivalHud.due(tpf)) {
            survivalHud.show(simulation.day(), simulation.minuteOfDay(), simulation.phase(),
                    simulation.survival(PlayerId.LOCAL), simulation.hearth(), camera.getWidth(), camera.getHeight());
        }
        runScenario(tpf);
    }

    /** Development only: marks the creature and writes what it is doing. Reads a snapshot; changes nothing. */
    private void showCreature(Camera camera) {
        java.util.Optional<CreatureStatus> status = simulation.creatureStatus();
        if (status.isEmpty()) {
            creatureText.setText("Creature: none in this world");
            return;
        }
        CreatureStatus now = status.get();
        creatureView.update(now, world.terrain().heightAt(now.x(), now.z()));
        creatureText.setText(String.format(java.util.Locale.ROOT,
                "Creature %s  at %.0f, %.0f  memories %d  route left %d",
                now.mode(), now.x(), now.z(), now.observations(), now.routePointsLeft()));
        creatureText.setLocalTranslation(HUD_MARGIN, camera.getHeight() - HUD_MARGIN - 120f, 0f);
    }

    /** Sets the sky, sun, ambient light and fog colour for the current game time. */
    private void updateLighting() {
        ColorRGBA skyColor = sky.update(simulation.minuteOfDay());
        ((SimpleApplication) getApplication()).getViewPort().setBackgroundColor(skyColor);
        fog.setFogColor(skyColor);
    }

    /** @return false if the screen changed and this frame should stop here */
    private boolean handleGameplayKeys() {
        if (gameplayInput.consumePause()) {
            navigator.pauseGame();
            return false;
        }
        if (simulation.survival(PlayerId.LOCAL).dead()) {
            // Enter or a click starts again, after a moment so a key held at the moment of death cannot skip the message.
            if (gameplayInput.consumeConfirm() && deadSeconds >= RESTART_DELAY) {
                navigator.restartGame();
                return false;
            }
            gameplayInput.consumeInteract();
            gameplayInput.consumeUse();
            gameplayInput.consumeInventory();
            return true;
        }
        if (gameplayInput.consumeInventory()) {
            navigator.openInventory();
            return false;
        }
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        int slot = gameplayInput.consumeSlot();
        if (slot >= 0 && slot < Inventory.HOTBAR_SLOTS) {
            inventory.select(slot);
        }
        if (gameplayInput.consumeDrop()) {
            Optional<ItemStack> held = inventory.selectedStack();
            if (held.isPresent()) {
                int quantity = gameplayInput.sprinting() ? held.get().quantity() : 1;
                showToast(simulation.dropItem(PlayerId.LOCAL, inventory.selected(), quantity).message());
            }
        }
        if (gameplayInput.consumePlace()) {
            startPlacement(inventory.selected());
        }
        if (gameplayInput.consumeUse() && inventory.selectedStack().isPresent()) {
            showToast(simulation.useItem(PlayerId.LOCAL, inventory.selected()).message());
        }
        Optional<Interactable> target = simulation.interactableFor(PlayerId.LOCAL);
        if (gameplayInput.consumeInteract() && target.isPresent()) {
            showToast(simulation.interact(new InteractionRequest(PlayerId.LOCAL, target.get().ref())).message());
        }
        gameplayInput.consumeRotate();
        gameplayInput.consumeConfirm();
        gameplayInput.consumeCancel();
        gameplayInput.consumeWheelTurn();
        return true;
    }

    private void handlePlacementInput() {
        gameplayInput.consumeInventory();
        gameplayInput.consumeInteract();
        gameplayInput.consumeUse();
        gameplayInput.consumeDrop();
        gameplayInput.consumePlace();
        gameplayInput.consumeSlot();
        if (gameplayInput.consumeRotate()) {
            placementYaw += ROTATE_STEP;
        }
        placementYaw += gameplayInput.consumeWheelTurn();
        if (gameplayInput.consumeCancel() || gameplayInput.consumePause()) {
            endPlacement();
            showToast("Placement cancelled.");
            return;
        }
        if (gameplayInput.consumeConfirm()) {
            confirmPlacement(((SimpleApplication) getApplication()).getCamera());
        }
    }

    /** Sends the previewed spot to the simulation, which checks it again. Stays in placement mode while items remain. */
    private void confirmPlacement(Camera camera) {
        float[] spot = aimPoint(camera);
        ActionResult result = simulation.placeItem(PlayerId.LOCAL, placingSlot, spot[0], spot[1], placementYaw);
        showToast(result.message());
        if (result.success() && simulation.inventory(PlayerId.LOCAL).slot(placingSlot).isEmpty()) {
            endPlacement();
        }
    }

    private void endPlacement() {
        placingSlot = NO_SLOT;
        preview.hide();
    }

    /** Where the player's line of sight meets the ground, floor or table top within reach. */
    private float[] aimPoint(Camera camera) {
        Vector3f eye = camera.getLocation();
        Vector3f dir = camera.getDirection();
        float limit = PlacementValidator.REACH + 2f;
        for (float t = AIM_START; t <= limit; t += AIM_STEP) {
            float x = eye.x + dir.x * t;
            float z = eye.z + dir.z * t;
            float y = eye.y + dir.y * t;
            if (y <= simulation.restHeight(x, z) + AIM_STEP / 2f) {
                return new float[] {x, z};
            }
        }
        float horizontal = (float) Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float scale = horizontal < 0.001f ? 0f : limit / horizontal;
        return new float[] {eye.x + dir.x * scale, eye.z + dir.z * scale};
    }

    private void updatePlacementPreview(Camera camera) {
        Optional<ItemStack> held = simulation.inventory(PlayerId.LOCAL).slot(placingSlot);
        if (held.isEmpty()) {
            endPlacement();
            return;
        }
        float[] spot = aimPoint(camera);
        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, placingSlot, spot[0], spot[1]);
        float y = check.valid() ? check.y() : simulation.restHeight(spot[0], spot[1]);
        preview.update(spot[0], y, spot[1], placementYaw, check.valid());
        String name = itemCatalog.get(held.get().item()).name();
        prompt.setColor(check.valid() ? GOOD_COLOR : BAD_COLOR);
        prompt.setText(check.valid()
                ? "Place " + name + ":  [Click / Enter] place   [R / wheel] rotate   [Esc / right click] cancel"
                : check.problem().message() + "   [R / wheel] rotate   [Esc / right click] cancel");
        centrePrompt(camera);
    }

    private void showPrompt(Camera camera) {
        prompt.setColor(HUD_COLOR);
        Optional<Interactable> target = simulation.interactableFor(PlayerId.LOCAL);
        if (target.isEmpty()) {
            prompt.setText("");
            return;
        }
        if (target.get().kind() == InteractableKind.ITEM) {
            prompt.setText("[E] Pick up " + describeItem(target.get().ref()));
        } else if (target.get().kind() == InteractableKind.HEARTH) {
            prompt.setText(stovePrompt());
        } else if (target.get().kind() == InteractableKind.VEHICLE) {
            prompt.setText(simulation.isDriving() ? "[W/S] Drive   [A/D] Steer   [E] Get out" : "[E] Get in the car");
        } else {
            prompt.setText(simulation.isCabinDoorOpen() ? "[E] Close the door" : "[E] Open the door");
        }
        centrePrompt(camera);
    }

    private String describeItem(InteractableRef ref) {
        for (WorldItem item : simulation.worldItems()) {
            if (InteractableRef.item(item.id()).equals(ref)) {
                String name = itemCatalog.get(item.stack().item()).name();
                return item.stack().quantity() > 1 ? name + " x" + item.stack().quantity() : name;
            }
        }
        return "item";
    }

    /** The prompt at the stove; the simulation decides what E actually does, this only describes it. */
    private String stovePrompt() {
        HearthStatus hearth = simulation.hearth();
        if (!hearth.state().lit() && hearth.fuel() > 0) {
            return "[E] Light the stove";
        }
        int wood = simulation.inventory(PlayerId.LOCAL).count(SurvivalActions.FIREWOOD);
        return wood > 0 ? "[E] Add firewood (" + wood + " left)" : "Stove: you have no firewood";
    }

    private void centrePrompt(Camera camera) {
        prompt.setLocalTranslation(camera.getWidth() / 2f - prompt.lineWidth() / 2f,
                PROMPT_BOTTOM, 0f);
    }

    private void updateHud(float tpf, Camera camera) {
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        Optional<ItemStack> held = inventory.selectedStack();
        String text = held.map(stack -> "[" + (inventory.selected() + 1) + "] " + itemCatalog.get(stack.item()).name()
                + (stack.quantity() > 1 ? " x" + stack.quantity() : "")
                + useHint(itemCatalog.get(stack.item()))
                + "     [I] Inventory   [Q] Drop   [P] Place")
                .orElse("[" + (inventory.selected() + 1) + "] empty     [I] Inventory");
        selectedText.setText(text);
        selectedText.setLocalTranslation(HUD_MARGIN, HUD_MARGIN + selectedText.lineHeight(), 0f);
        if (toastLeft > 0f) {
            toastLeft -= tpf;
            toast.setLocalTranslation(camera.getWidth() / 2f - toast.lineWidth() / 2f,
                    TOAST_BOTTOM, 0f);
            if (toastLeft <= 0f) {
                toast.setText("");
            }
        }
    }

    private static String useHint(ItemDefinition definition) {
        return definition.useVerb().isEmpty() ? "" : "   [F] " + definition.useVerb();
    }

    private void showToast(String message) {
        toast.setText(message);
        toastLeft = TOAST_SECONDS;
    }

    private void playFootsteps(PlayerInput input) {
        float dx = player.x() - lastX;
        float dz = player.z() - lastZ;
        lastX = player.x();
        lastZ = player.z();
        boolean wading = world.water().depthAt(world.terrain(), player.x(), player.z()) > PlayerMovement.WADE_DEPTH;
        if (steps.advance((float) Math.sqrt(dx * dx + dz * dz)) && footsteps != null && !wading
                && !simulation.isDriving()) {
            footsteps.play(input.sprint());
        }
    }

    /** Development scripts: do one thing a few seconds after the world starts, so a screenshot can show it. */
    private void runScenario(float tpf) {
        if (scenarioDone || options.scenario().isEmpty()) {
            return;
        }
        scenarioClock += tpf;
        if (scenarioClock < SCENARIO_DELAY) {
            return;
        }
        scenarioDone = true;
        switch (options.scenario()) {
            case "inventory" -> navigator.openInventory();
            case "restart" -> navigator.restartGame();
            case "placement" -> {
                scenarioLook = SCENARIO_PLACE_LOOK;
                startPlacement(0);
            }
            case "drop" -> {
                simulation.dropItem(PlayerId.LOCAL, 0, 2);
                scenarioLook = SCENARIO_LOOK_DOWN;
            }
            default -> LOG.warn("Unknown dev.scenario '{}'", options.scenario());
        }
    }

    private static ShadowText hudText(BitmapFont font, float scale) {
        return new ShadowText(font, scale, HUD_COLOR);
    }

    private void centreHud(Camera camera) {
        crosshair.setLocalTranslation(camera.getWidth() / 2f - crosshair.lineWidth() / 2f,
                camera.getHeight() / 2f + crosshair.lineHeight() / 2f, 0f);
    }

    /**
     * Moves the camera to the player's eyes and points it where they look. The simulation moves the player in
     * fixed steps, so the eye is blended between the last two steps to stay smooth at any frame rate.
     */
    private void followPlayer(Camera camera) {
        float a = simulation.interpolation();
        float x = player.previousX() + (player.x() - player.previousX()) * a;
        float y = player.previousY() + (player.y() - player.previousY()) * a;
        float z = player.previousZ() + (player.z() - player.previousZ()) * a;
        float eyeHeight = simulation.isDriving() ? DRIVER_EYE_HEIGHT : EYE_HEIGHT;
        camera.setLocation(camera.getLocation().set(x, y + eyeHeight, z));
        float cosPitch = (float) Math.cos(player.pitch());
        camera.lookAtDirection(new Vector3f(
                (float) Math.sin(player.yaw()) * cosPitch,
                (float) Math.sin(player.pitch()),
                (float) Math.cos(player.yaw()) * cosPitch), Vector3f.UNIT_Y);
        scene.update(player.x(), player.z());
    }

    private static float interpolate(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }
}
