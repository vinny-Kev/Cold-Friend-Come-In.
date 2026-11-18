package com.coldfriend.world;

import com.coldfriend.creature.CreatureSimulation;
import com.coldfriend.creature.CreatureStatus;
import com.coldfriend.creature.SenseEvent;
import com.coldfriend.creature.SenseKind;
import com.coldfriend.creature.SenseLog;
import com.coldfriend.entity.EntityId;
import com.coldfriend.interaction.ActionResult;
import com.coldfriend.interaction.Interactable;
import com.coldfriend.interaction.InteractableKind;
import com.coldfriend.interaction.InteractableRef;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemStack;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerMovement;
import com.coldfriend.player.PlayerState;
import com.coldfriend.player.StepCounter;
import com.coldfriend.survival.Hearth;
import com.coldfriend.survival.HearthParameters;
import com.coldfriend.survival.HearthStatus;
import com.coldfriend.survival.Heat;
import com.coldfriend.survival.Survival;
import com.coldfriend.survival.SurvivalParameters;
import com.coldfriend.survival.SurvivalStatus;
import com.coldfriend.time.ClockParameters;
import com.coldfriend.time.DayPhase;
import com.coldfriend.time.WorldClock;
import com.coldfriend.world.items.Deadfall;
import com.coldfriend.world.gen.RandomStreams;
import com.coldfriend.world.items.ItemActions;
import com.coldfriend.world.items.PlacementCheck;
import com.coldfriend.world.items.PlacementProblem;
import com.coldfriend.world.items.PlacementValidator;
import com.coldfriend.world.items.StartingItems;
import com.coldfriend.world.items.WorldItem;
import com.coldfriend.world.items.WorldItems;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The authoritative game state of one world: players, their inventories and needs, the game clock, the cabin door
 * and hearth, and the items lying or placed in the world. It advances from player inputs and requests only, and
 * knows nothing about rendering, the camera or the input devices, so it could run on a server.
 *
 * <p>Time advances in fixed steps (see {@link FixedStep}). Each step runs, in this order: player movement, the
 * clock, the hearth, then each player's needs. Each of those is its own class; this class only owns them and
 * calls them in order. Other code reads the state through snapshots ({@link SurvivalStatus},
 * {@link HearthStatus}) and changes it only through requests.
 */
public final class WorldSimulation {

    private static final float DOOR_REACH = 2.2f;
    /** Ids for the car's round obstacles; far above the item counter, like the creature's id. */
    private static final long CAR_ID = 2_000_000_000L;
    /** Distance walked between two footsteps the creature can hear, in metres. */
    private static final float FOOTSTEP_STRIDE = 2.5f;
    /** How close to the stove's centre a player must be to tend it, in metres. */
    private static final float HEARTH_REACH = 1.6f;
    private static final float AIM_EYE_HEIGHT = 1.65f;
    private static final float AIM_TOLERANCE = 0.45f;
    private static final float AIM_MIN_DISTANCE = 0.2f;

    private final WorldData world;
    private final ItemCatalog catalog;
    private final WorldCollision collision;
    private final PlayerMovement movement;
    private final Interactable cabinDoor;
    private final WorldItems worldItems = new WorldItems();
    private final PlacementValidator validator;
    private final ItemActions itemActions;
    private final Deadfall deadfall;
    private final Map<PlayerId, PlayerState> players = new LinkedHashMap<>();
    private final Map<PlayerId, Inventory> inventories = new LinkedHashMap<>();
    private final Map<PlayerId, Survival> survival = new LinkedHashMap<>();
    private final Map<PlayerId, StepCounter> strides = new LinkedHashMap<>();
    private final SenseLog senses = new SenseLog();
    private final Optional<CreatureSimulation> creature;
    private final Optional<ParkedCar> car;
    private final FixedStep fixedStep = new FixedStep();
    private final WorldClock clock;
    private final Hearth hearth = new Hearth(HearthParameters.DEFAULT);
    private final Interactable cabinHearth;
    private final SurvivalActions survivalActions;
    private boolean cabinDoorOpen;

    /**
     * A world with no item definitions. Enough for movement and doors.
     *
     * @param world the generated world, which never changes
     */
    public WorldSimulation(WorldData world) {
        this(world, ItemCatalog.empty());
    }

    /**
     * @param world the generated world, which never changes
     * @param catalog the item definitions
     */
    public WorldSimulation(WorldData world, ItemCatalog catalog) {
        this(world, catalog, ClockParameters.DEFAULT.newGameStart());
    }

    /**
     * @param world the generated world, which never changes
     * @param catalog the item definitions
     * @param startMinute time of day the game starts at on day 1, in minutes after midnight
     */
    public WorldSimulation(WorldData world, ItemCatalog catalog, int startMinute) {
        this.world = world;
        this.clock = new WorldClock(ClockParameters.DEFAULT, FixedStep.STEPS_PER_SECOND, startMinute);
        this.survivalActions = new SurvivalActions(world, catalog, SurvivalParameters.DEFAULT);
        this.catalog = catalog;
        this.collision = new WorldCollision(world);
        this.movement = new PlayerMovement(world, collision);
        float[] doorway = CabinLayout.doorway(world.cabin());
        this.cabinDoor = new Interactable(InteractableRef.CABIN_DOOR, doorway[0], doorway[1], DOOR_REACH);
        float[] stove = stoveCentre(world);
        this.cabinHearth = new Interactable(InteractableRef.CABIN_HEARTH, stove[0], stove[1], HEARTH_REACH);
        this.validator = new PlacementValidator(world, collision, worldItems, catalog);
        this.itemActions = new ItemActions(catalog, worldItems, validator, collision);
        StartingItems.populate(world, catalog, worldItems, validator);
        this.deadfall = new Deadfall(world, catalog, collision, worldItems, validator);
        this.car = ParkedCar.find(world, collision);
        car.ifPresent(parked -> {
            float[] centres = parked.obstacleCentres();
            for (int i = 0; i < centres.length; i += 2) {
                collision.addObstacle(new EntityId(CAR_ID + i / 2), centres[i], centres[i + 1], ParkedCar.RADIUS);
            }
        });
        this.creature = CreatureSimulation.spawn(world, collision, CreatureSimulation.ID,
                new RandomStreams(world.seed(), world.generationVersion(), 0));
        deadfall.scatter(clock.day(), Deadfall.INITIAL_STACKS);
    }

    /** @return the generated world this simulation runs in */
    public WorldData world() {
        return world;
    }

    /** @return the item definitions this simulation uses */
    public ItemCatalog items() {
        return catalog;
    }

    /**
     * Adds a player at a spawn point, with an empty inventory plus the starting kit.
     *
     * @param id the player's identity
     * @param spawn where the player appears and which way they face
     * @return the new player's state
     */
    public PlayerState addPlayer(PlayerId id, SpawnPoints.Spawn spawn) {
        PlayerState player = new PlayerState(id, spawn.x(), world.terrain().heightAt(spawn.x(), spawn.z()),
                spawn.z(), spawn.yaw());
        players.put(id, player);
        Inventory inventory = new Inventory(catalog, Inventory.PLAYER_SLOTS);
        for (ItemStack stack : catalog.startingKit()) {
            inventory.add(stack);
        }
        inventories.put(id, inventory);
        survival.put(id, new Survival(SurvivalParameters.DEFAULT));
        strides.put(id, new StepCounter(FOOTSTEP_STRIDE));
        return player;
    }

    /**
     * @param id a player's identity
     * @return that player's state, or null if there is no such player
     */
    public PlayerState player(PlayerId id) {
        return players.get(id);
    }

    /**
     * @param id a player's identity
     * @return that player's inventory, or null if there is no such player
     */
    public Inventory inventory(PlayerId id) {
        return inventories.get(id);
    }

    /**
     * Moves the contents of one inventory slot to another: into an empty slot, merged with the same item, or
     * swapped with a different item.
     *
     * @param id the player
     * @param from the slot to move from
     * @param to the slot to move to
     * @return what happened
     */
    public Inventory.MoveResult moveInventoryItem(PlayerId id, int from, int to) {
        return inventories.get(id).move(from, to);
    }

    /**
     * @param x world x
     * @param z world z
     * @return the height an item resting at this spot would sit at: a table top, the cabin floor or the ground
     */
    public float restHeight(float x, float z) {
        return validator.restHeight(x, z);
    }

    /** @return a snapshot of every item lying or placed in the world */
    public List<WorldItem> worldItems() {
        return worldItems.all();
    }

    /**
     * @param id a player's identity
     * @return a snapshot of that player's needs
     * @throws IllegalArgumentException if there is no such player
     */
    public SurvivalStatus survival(PlayerId id) {
        Survival needs = survival.get(id);
        if (needs == null) {
            throw new IllegalArgumentException("No player " + id);
        }
        return needs.status();
    }

    /** @return the car parked beside the cabin, if one fits in this world */
    public Optional<ParkedCar> car() {
        return car;
    }

    /** @return the creature, if one could be placed in this world */
    public Optional<CreatureSimulation> creature() {
        return creature;
    }

    /** @return a snapshot of the creature for the debug overlay, if there is one */
    public Optional<CreatureStatus> creatureStatus() {
        return creature.map(CreatureSimulation::status);
    }

    /** @return how many sense events the player has ever caused (for tests and development) */
    public long sensesRaised() {
        return senses.raised();
    }

    /** @return a snapshot of the cabin's hearth */
    public HearthStatus hearth() {
        return hearth.status();
    }

    /** @return the cabin stove fire's level, 0 when not lit; cheap, for drawing every frame */
    public double hearthLevel() {
        return hearth.level();
    }

    /** @return the in-game day, starting at 1 */
    public int day() {
        return clock.day();
    }

    /** @return the in-game time of day in minutes after midnight */
    public double minuteOfDay() {
        return clock.minuteOfDay();
    }

    /** @return the part of the day it is */
    public DayPhase phase() {
        return clock.phase();
    }

    /** @return the clock's tuning, for presentation that maps time of day to light */
    public ClockParameters clockParameters() {
        return clock.parameters();
    }

    /** @return true if the cabin door is open */
    public boolean isCabinDoorOpen() {
        return cabinDoorOpen;
    }

    /**
     * What the player could use right now. A world item the player is looking at comes first; otherwise the
     * nearer of the cabin door and the stove, if the player is within reach of it.
     *
     * @param id a player's identity
     * @return the thing that player could use, if any
     */
    public Optional<Interactable> interactableFor(PlayerId id) {
        PlayerState player = players.get(id);
        if (player == null) {
            return Optional.empty();
        }
        Optional<WorldItem> aimed = aimedItem(player);
        if (aimed.isPresent()) {
            WorldItem item = aimed.get();
            return Optional.of(new Interactable(InteractableRef.item(item.id()), item.x(), item.z(),
                    ItemActions.PICKUP_REACH));
        }
        float door = distanceSquared(player, cabinDoor);
        float stove = distanceSquared(player, cabinHearth);
        boolean doorInReach = door <= cabinDoor.reach() * cabinDoor.reach();
        boolean stoveInReach = stove <= cabinHearth.reach() * cabinHearth.reach();
        if (doorInReach && (!stoveInReach || door <= stove)) {
            return Optional.of(cabinDoor);
        }
        return stoveInReach ? Optional.of(cabinHearth) : Optional.empty();
    }

    /**
     * Uses the item in one of a player's inventory slots: eat, drink, or fill a container at open water.
     *
     * @param id the player
     * @param slot the inventory slot
     * @return what happened
     */
    public ActionResult useItem(PlayerId id, int slot) {
        PlayerState player = players.get(id);
        if (player == null) {
            return ActionResult.failed("There is nothing to use.");
        }
        return survivalActions.use(player, inventories.get(id), survival.get(id), slot);
    }

    private boolean isDead(PlayerId id) {
        Survival needs = survival.get(id);
        return needs != null && needs.dead();
    }

    private static float distanceSquared(PlayerState player, Interactable target) {
        float dx = target.x() - player.x();
        float dz = target.z() - player.z();
        return dx * dx + dz * dz;
    }

    /** @return world x and z of the middle of the cabin's stove */
    private static float[] stoveCentre(WorldData world) {
        for (CabinLayout.Furnishing furnishing : CabinLayout.furnishings(world.cabin().footprint())) {
            if ("stove".equals(furnishing.id())) {
                return CabinLayout.point(world.cabin(), furnishing.part().side(), furnishing.part().forward());
            }
        }
        throw new IllegalStateException("The cabin layout has no stove");
    }

    /**
     * Handles a player's request to use something. It works only if the player could use that thing right now.
     *
     * @param request who wants to use what
     * @return what happened, with a message to show
     */
    public ActionResult interact(InteractionRequest request) {
        Optional<Interactable> target = interactableFor(request.player());
        if (target.isEmpty() || !target.get().ref().equals(request.target())) {
            return ActionResult.failed("There is nothing to use there.");
        }
        if (survival.get(request.player()).dead()) {
            return ActionResult.failed(SurvivalActions.DEAD_MESSAGE);
        }
        if (target.get().kind() == InteractableKind.ITEM) {
            return pickUpItem(request.player(), target.get().ref().entity());
        }
        if (target.get().kind() == InteractableKind.HEARTH) {
            return survivalActions.tendHearth(inventories.get(request.player()), hearth);
        }
        cabinDoorOpen = !cabinDoorOpen;
        sense(SenseKind.DOOR, cabinDoor.x(), cabinDoor.z());
        collision.setDoorClosed(!cabinDoorOpen);
        return ActionResult.ok(cabinDoorOpen ? "You open the door." : "You close the door.");
    }

    /**
     * Picks up a world item, as much as fits. The player must be close enough and able to reach it.
     *
     * @param id the player
     * @param item which world item
     * @return what happened
     */
    public ActionResult pickUpItem(PlayerId id, EntityId item) {
        if (isDead(id)) {
            return ActionResult.failed(SurvivalActions.DEAD_MESSAGE);
        }
        Optional<WorldItem> found = worldItems.get(item);
        ActionResult result = itemActions.pickUp(players.get(id), inventories.get(id), item);
        if (result.success() && found.isPresent()) {
            sense(SenseKind.PICK_UP, found.get().x(), found.get().z());
        }
        return result;
    }

    /**
     * Drops some of what a player holds in one slot on the ground in front of them.
     *
     * @param id the player
     * @param slot the inventory slot
     * @param quantity how many to drop
     * @return what happened
     */
    public ActionResult dropItem(PlayerId id, int slot, int quantity) {
        if (isDead(id)) {
            return ActionResult.failed(SurvivalActions.DEAD_MESSAGE);
        }
        ActionResult result = itemActions.drop(players.get(id), inventories.get(id), slot, quantity);
        if (result.success()) {
            sense(SenseKind.DROP, players.get(id).x(), players.get(id).z());
        }
        return result;
    }

    /**
     * Places one item from a slot in the world. The spot is validated here, whatever a preview showed.
     *
     * @param id the player
     * @param slot the inventory slot
     * @param x wanted world x
     * @param z wanted world z
     * @param yaw wanted rotation in radians
     * @return what happened
     */
    public ActionResult placeItem(PlayerId id, int slot, float x, float z, float yaw) {
        if (isDead(id)) {
            return ActionResult.failed(SurvivalActions.DEAD_MESSAGE);
        }
        ActionResult result = itemActions.place(players.get(id), inventories.get(id), slot, x, z, yaw);
        if (result.success()) {
            sense(SenseKind.PLACE, x, z);
        }
        return result;
    }

    /**
     * Asks whether the item in a slot could be placed at a spot, without changing anything. A screen uses this
     * for its preview; the answer is advice only.
     *
     * @return the check, or a refusal if the slot is empty
     */
    public PlacementCheck checkPlacement(PlayerId id, int slot, float x, float z) {
        PlayerState player = players.get(id);
        Optional<ItemStack> held = inventories.get(id).slot(slot);
        if (held.isEmpty()) {
            return PlacementCheck.refused(PlacementProblem.NOT_ALLOWED);
        }
        return validator.checkPlace(catalog.get(held.get().item()), player.x(), player.z(), x, z);
    }

    /**
     * Advances the world by one rendered frame. Mouse look is applied at once; everything else runs in fixed
     * steps of {@link FixedStep#STEP_SECONDS}, as many as the frame's time makes due (see {@link FixedStep}).
     *
     * @param frameSeconds real seconds since the last frame
     * @param inputs what each player asked for; a player with no entry does nothing
     */
    public void update(float frameSeconds, Map<PlayerId, PlayerInput> inputs) {
        for (PlayerState player : players.values()) {
            if (!survival.get(player.id()).dead()) {
                movement.look(player, inputs.getOrDefault(player.id(), PlayerInput.NONE));
            }
        }
        int steps = fixedStep.advance(frameSeconds);
        for (int i = 0; i < steps; i++) {
            step(inputs);
        }
    }

    /**
     * Forgets frame time owed but not yet simulated. Call when play resumes after a pause, so the first frame
     * cannot run a burst of catch-up steps.
     */
    public void resetFrameTiming() {
        fixedStep.reset();
    }

    /** @return how far between the last step and the next one the present moment is, from 0 to under 1 */
    public float interpolation() {
        return fixedStep.alpha();
    }

    /**
     * One fixed step. The order is the update order of the whole simulation: players move, then the clock
     * advances, then the hearth burns, then each player's needs change.
     */
    private void step(Map<PlayerId, PlayerInput> inputs) {
        for (PlayerState player : players.values()) {
            PlayerInput input = survival.get(player.id()).dead() ? PlayerInput.NONE
                    : inputs.getOrDefault(player.id(), PlayerInput.NONE);
            float fromX = player.x();
            float fromZ = player.z();
            movement.move(player, input, FixedStep.STEP_SECONDS);
            float dx = player.x() - fromX;
            float dz = player.z() - fromZ;
            if (strides.get(player.id()).advance((float) Math.sqrt(dx * dx + dz * dz))) {
                sense(input.sprint() ? SenseKind.FOOTSTEP_SPRINT : SenseKind.FOOTSTEP_WALK, player.x(), player.z());
            }
        }
        if (clock.advanceStep()) {
            deadfall.scatter(clock.day(), Deadfall.DAILY_STACKS);
        }
        double minutes = clock.minutesPerStep();
        hearth.update(minutes);
        DayPhase phase = clock.phase();
        Heat heat = hearth.heat();
        for (PlayerState player : players.values()) {
            boolean inside = world.cabin().footprint().contains(player.x(), player.z(), 0f);
            boolean wading = world.water().depthAt(world.terrain(), player.x(), player.z())
                    > PlayerMovement.WADE_DEPTH;
            survival.get(player.id()).update(minutes, inside, wading, phase, heat);
        }
        if (creature.isPresent()) {
            creature.get().step(FixedStep.STEP_SECONDS, players.values(), senses.events(), clock.phase(),
                    clock.totalMinutes());
        }
        senses.clear();
    }

    private void sense(SenseKind kind, float x, float z) {
        senses.add(new SenseEvent(kind, x, z, clock.totalMinutes()));
    }

    /** The world item the player is looking at, within reach: the one nearest to the line of sight. */
    private Optional<WorldItem> aimedItem(PlayerState player) {
        float cosPitch = (float) Math.cos(player.pitch());
        float dirX = (float) Math.sin(player.yaw()) * cosPitch;
        float dirY = (float) Math.sin(player.pitch());
        float dirZ = (float) Math.cos(player.yaw()) * cosPitch;
        float eyeY = player.y() + AIM_EYE_HEIGHT;
        WorldItem best = null;
        float bestMiss = Float.MAX_VALUE;
        for (WorldItem item : worldItems.all()) {
            float dx = item.x() - player.x();
            float dy = item.y() + AIM_TOLERANCE / 2f - eyeY;
            float dz = item.z() - player.z();
            float along = dx * dirX + dy * dirY + dz * dirZ;
            float distanceSquared = dx * dx + dy * dy + dz * dz;
            if (along < AIM_MIN_DISTANCE || distanceSquared > ItemActions.PICKUP_REACH * ItemActions.PICKUP_REACH) {
                continue;
            }
            float miss = (float) Math.sqrt(Math.max(0f, distanceSquared - along * along));
            if (miss <= AIM_TOLERANCE && miss < bestMiss) {
                bestMiss = miss;
                best = item;
            }
        }
        return Optional.ofNullable(best);
    }
}
