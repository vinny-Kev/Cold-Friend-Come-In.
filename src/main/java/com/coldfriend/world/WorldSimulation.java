package com.coldfriend.world;

import com.coldfriend.entity.EntityId;
import com.coldfriend.interaction.ActionResult;
import com.coldfriend.interaction.Interactable;
import com.coldfriend.interaction.InteractableKind;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemStack;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerMovement;
import com.coldfriend.player.PlayerState;
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
 * The authoritative game state of one world: players, their inventories, the cabin door, and the items lying or
 * placed in the world. It advances from player inputs and requests only, and knows nothing about rendering, the
 * camera or the input devices, so it could run on a server. Things that change over time, such as villagers and
 * the creature, will be added here.
 */
public final class WorldSimulation {

    /** Stable id of the cabin door. */
    public static final String CABIN_DOOR_ID = "cabin-door";
    /** Prefix of the id of a world item as an interactable; the rest is the item's entity id. */
    public static final String ITEM_PREFIX = "item:";
    private static final float DOOR_REACH = 2.2f;
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
    private final Map<PlayerId, PlayerState> players = new LinkedHashMap<>();
    private final Map<PlayerId, Inventory> inventories = new LinkedHashMap<>();
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
        this.world = world;
        this.catalog = catalog;
        this.collision = new WorldCollision(world);
        this.movement = new PlayerMovement(world, collision);
        float[] doorway = CabinLayout.doorway(world.cabin());
        this.cabinDoor = new Interactable(CABIN_DOOR_ID, InteractableKind.DOOR, doorway[0], doorway[1], DOOR_REACH);
        this.validator = new PlacementValidator(world, collision, worldItems, catalog);
        this.itemActions = new ItemActions(catalog, worldItems, validator, collision);
        StartingItems.populate(world, catalog, worldItems, validator);
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

    /** @return true if the cabin door is open */
    public boolean isCabinDoorOpen() {
        return cabinDoorOpen;
    }

    /**
     * What the player could use right now. A world item the player is looking at comes first; otherwise the
     * cabin door if the player is next to it.
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
            return Optional.of(new Interactable(ITEM_PREFIX + item.id(), InteractableKind.ITEM, item.x(), item.z(),
                    ItemActions.PICKUP_REACH));
        }
        float dx = cabinDoor.x() - player.x();
        float dz = cabinDoor.z() - player.z();
        return dx * dx + dz * dz <= cabinDoor.reach() * cabinDoor.reach() ? Optional.of(cabinDoor) : Optional.empty();
    }

    /**
     * Handles a player's request to use something. It works only if the player could use that thing right now.
     *
     * @param request who wants to use what
     * @return what happened, with a message to show
     */
    public ActionResult interact(InteractionRequest request) {
        Optional<Interactable> target = interactableFor(request.player());
        if (target.isEmpty() || !target.get().id().equals(request.interactableId())) {
            return ActionResult.failed("There is nothing to use there.");
        }
        if (target.get().kind() == InteractableKind.ITEM) {
            EntityId item = new EntityId(Long.parseLong(target.get().id().substring(ITEM_PREFIX.length())));
            return pickUpItem(request.player(), item);
        }
        cabinDoorOpen = !cabinDoorOpen;
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
        return itemActions.pickUp(players.get(id), inventories.get(id), item);
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
        return itemActions.drop(players.get(id), inventories.get(id), slot, quantity);
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
        return itemActions.place(players.get(id), inventories.get(id), slot, x, z, yaw);
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
     * Advances the world by one frame.
     *
     * @param dt seconds since the last frame
     * @param inputs what each player asked for; a player with no entry does nothing
     */
    public void update(float dt, Map<PlayerId, PlayerInput> inputs) {
        for (PlayerState player : players.values()) {
            movement.step(player, inputs.getOrDefault(player.id(), PlayerInput.NONE), dt);
        }
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
