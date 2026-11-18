package com.coldfriend.world;

import com.coldfriend.interaction.ActionResult;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemId;
import com.coldfriend.item.ItemStack;
import com.coldfriend.player.PlayerState;
import com.coldfriend.survival.Hearth;
import com.coldfriend.survival.Survival;
import com.coldfriend.survival.SurvivalParameters;
import com.coldfriend.world.model.WorldData;
import java.util.Optional;

/**
 * Actions that move items between a player's inventory and their needs or the hearth: eating, drinking, filling a
 * container, feeding and lighting the stove. Each action checks everything first and changes nothing if it fails,
 * so an item is never lost or duplicated. Items are named by {@link ItemId}; an item missing from the catalogue
 * simply cannot be used.
 */
public final class SurvivalActions {

    public static final ItemId FIREWOOD = new ItemId("firewood");
    static final ItemId BRANCH = new ItemId("branch");
    static final ItemId MATCHES = new ItemId("matches");
    static final ItemId RATION = new ItemId("ration");
    static final ItemId BANDAGE = new ItemId("bandage");
    static final ItemId WATER_FULL = new ItemId("water_container");
    static final ItemId WATER_EMPTY = new ItemId("water_container_empty");
    /** Shown when a dead player tries to act. */
    public static final String DEAD_MESSAGE = "You are dead.";
    /** How far in front of the player open water may be to fill a container, in metres. */
    private static final float FILL_REACH = 1.2f;
    /** Water at least this deep, in metres, can be scooped up. */
    private static final float FILL_DEPTH = 0.05f;

    private final WorldData world;
    private final ItemCatalog catalog;
    private final SurvivalParameters parameters;

    SurvivalActions(WorldData world, ItemCatalog catalog, SurvivalParameters parameters) {
        this.world = world;
        this.catalog = catalog;
        this.parameters = parameters;
    }

    /**
     * Using the stove: lights it if it is cold and has fuel (needs matches, uses one branch as kindling),
     * otherwise adds one piece of firewood.
     */
    ActionResult tendHearth(Inventory inventory, Hearth hearth) {
        if (hearth.canLight()) {
            if (!inventory.has(MATCHES, 1) || !inventory.has(BRANCH, 1)) {
                return ActionResult.failed("To light the stove you need matches and a branch for kindling.");
            }
            inventory.remove(BRANCH, 1);
            hearth.light();
            return ActionResult.ok("You light the stove.");
        }
        if (!inventory.has(FIREWOOD, 1)) {
            return ActionResult.failed("You have no firewood.");
        }
        if (!hearth.canAdd(1)) {
            return ActionResult.failed("The stove is full.");
        }
        inventory.remove(FIREWOOD, 1);
        hearth.addFirewood(1);
        return ActionResult.ok(hearth.status().state().lit() ? "You add firewood to the fire."
                : "You put firewood in the cold stove.");
    }

    /** Uses the item in one inventory slot: eat a ration, drink, or fill an empty container at open water. */
    ActionResult use(PlayerState player, Inventory inventory, Survival survival, int slot) {
        if (slot < 0 || slot >= inventory.size()) {
            return ActionResult.failed("There is nothing to use.");
        }
        Optional<ItemStack> held = inventory.slot(slot);
        if (held.isEmpty()) {
            return ActionResult.failed("There is nothing to use.");
        }
        if (survival.dead()) {
            return ActionResult.failed(DEAD_MESSAGE);
        }
        ItemId item = held.get().item();
        if (item.equals(RATION)) {
            if (survival.status().nourishment() >= 100.0) {
                return ActionResult.failed("You are not hungry.");
            }
            inventory.take(slot, 1);
            survival.eat(parameters.rationNourishment());
            return ActionResult.ok("You eat a ration.");
        }
        if (item.equals(BANDAGE)) {
            if (survival.status().health() >= 100.0) {
                return ActionResult.failed("You are not hurt.");
            }
            inventory.take(slot, 1);
            survival.heal(parameters.bandageHealth());
            return ActionResult.ok("You dress your wounds.");
        }
        if (item.equals(WATER_FULL)) {
            if (survival.status().hydration() >= 100.0) {
                return ActionResult.failed("You are not thirsty.");
            }
            if (!swap(inventory, slot, WATER_FULL, WATER_EMPTY)) {
                return ActionResult.failed("You cannot drink that now.");
            }
            survival.drink(parameters.drinkHydration());
            return ActionResult.ok("You drink. The container is empty.");
        }
        if (item.equals(WATER_EMPTY)) {
            if (!nearWater(player)) {
                return ActionResult.failed("You need open water to fill the container.");
            }
            if (!swap(inventory, slot, WATER_EMPTY, WATER_FULL)) {
                return ActionResult.failed("You cannot fill that now.");
            }
            return ActionResult.ok("You fill the container with river water.");
        }
        if (item.equals(FIREWOOD)) {
            return ActionResult.failed("Put firewood in the stove with E.");
        }
        return ActionResult.failed("You cannot use that here.");
    }

    /** Replaces one item in a slot with another, all or nothing. */
    private boolean swap(Inventory inventory, int slot, ItemId from, ItemId to) {
        if (!catalog.contains(to)) {
            return false;
        }
        Optional<ItemStack> taken = inventory.take(slot, 1);
        if (taken.isEmpty() || !taken.get().item().equals(from)) {
            taken.ifPresent(inventory::add);
            return false;
        }
        if (inventory.add(new ItemStack(to, 1)) > 0) {
            inventory.add(taken.get());
            return false;
        }
        return true;
    }

    /** @return true if the player stands in water or open water is just in front of them */
    private boolean nearWater(PlayerState player) {
        float aheadX = player.x() + (float) Math.sin(player.yaw()) * FILL_REACH;
        float aheadZ = player.z() + (float) Math.cos(player.yaw()) * FILL_REACH;
        return world.water().depthAt(world.terrain(), player.x(), player.z()) > FILL_DEPTH
                || world.water().depthAt(world.terrain(), aheadX, aheadZ) > FILL_DEPTH;
    }
}
