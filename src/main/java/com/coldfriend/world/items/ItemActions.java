package com.coldfriend.world.items;

import com.coldfriend.entity.EntityId;
import com.coldfriend.interaction.ActionResult;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemDefinition;
import com.coldfriend.item.ItemStack;
import com.coldfriend.player.PlayerState;
import com.coldfriend.world.query.WorldCollision;
import java.util.Optional;

/**
 * The authoritative transfers of items between an inventory and the world: dropping, picking up, placing. Each
 * action checks everything first and changes state only when it will succeed, so a failed action never loses or
 * duplicates an item. It works on the simulation's own state and knows nothing about screens.
 */
public final class ItemActions {

    /** Farthest an item can be picked up from, in metres. */
    public static final float PICKUP_REACH = 3.2f;
    private static final float PICKUP_EYE_HEIGHT = 1.3f;
    private static final float[] DROP_DISTANCES = {0.9f, 1.3f, 0.55f};
    private static final float[] DROP_ANGLES = {0f, 0.6f, -0.6f, 1.2f, -1.2f, 2.4f, -2.4f};

    private final ItemCatalog catalog;
    private final WorldItems items;
    private final PlacementValidator validator;
    private final WorldCollision collision;

    /**
     * @param catalog the item definitions
     * @param items the items in the world
     * @param validator decides where items may go
     * @param collision the world's obstacles, which placed objects join
     */
    public ItemActions(ItemCatalog catalog, WorldItems items, PlacementValidator validator,
            WorldCollision collision) {
        this.catalog = catalog;
        this.items = items;
        this.validator = validator;
        this.collision = collision;
    }

    /**
     * Drops some of what is in a slot on the ground in front of the player.
     *
     * @param player the player
     * @param inventory the player's inventory
     * @param slot the slot to drop from
     * @param quantity how many to drop; at least 1 and at most what the slot holds
     * @return what happened
     */
    public ActionResult drop(PlayerState player, Inventory inventory, int slot, int quantity) {
        Optional<ItemStack> held = inventory.slot(slot);
        if (held.isEmpty()) {
            return ActionResult.failed("Nothing to drop.");
        }
        if (quantity < 1 || quantity > held.get().quantity()) {
            return ActionResult.failed("You do not have that many.");
        }
        ItemDefinition definition = catalog.get(held.get().item());
        if (!definition.canDrop()) {
            return ActionResult.failed("You cannot drop the " + definition.name() + ".");
        }
        PlacementProblem lastProblem = PlacementProblem.BLOCKED;
        for (float distance : DROP_DISTANCES) {
            for (float angle : DROP_ANGLES) {
                float x = player.x() + (float) Math.sin(player.yaw() + angle) * distance;
                float z = player.z() + (float) Math.cos(player.yaw() + angle) * distance;
                PlacementCheck check = validator.checkDrop(definition, player.x(), player.z(), x, z);
                if (check.valid()) {
                    ItemStack taken = inventory.take(slot, quantity).orElseThrow();
                    items.spawn(taken, x, check.y(), z, player.yaw(), false);
                    return ActionResult.ok("Dropped " + describe(definition, quantity) + ".");
                }
                lastProblem = check.problem();
            }
        }
        return ActionResult.failed("No room to drop that here. " + lastProblem.message());
    }

    /**
     * Picks up a world item, as much as fits. The item leaves the world only when everything has moved to the
     * inventory; if only some fits, the rest stays where it was.
     *
     * @param player the player
     * @param inventory the player's inventory
     * @param id which world item
     * @return what happened
     */
    public ActionResult pickUp(PlayerState player, Inventory inventory, EntityId id) {
        Optional<WorldItem> found = items.get(id);
        if (found.isEmpty()) {
            return ActionResult.failed("That is gone.");
        }
        WorldItem item = found.get();
        ItemDefinition definition = catalog.get(item.stack().item());
        if (!definition.canPickUp()) {
            return ActionResult.failed("You cannot pick up the " + definition.name() + ".");
        }
        if (!inReach(player, item)) {
            return ActionResult.failed("Too far away.");
        }
        if (!validator.canReach(player.x(), player.z(), item.x(), item.z())) {
            return ActionResult.failed("You cannot reach that.");
        }
        int left = inventory.add(item.stack());
        int taken = item.stack().quantity() - left;
        if (taken == 0) {
            return ActionResult.failed("Your inventory is full.");
        }
        if (left == 0) {
            items.remove(id);
            collision.removeObstacle(id);
            return ActionResult.ok("Picked up " + describe(definition, taken) + ".");
        }
        items.replace(item.withStack(item.stack().withQuantity(left)));
        return ActionResult.ok("Picked up " + describe(definition, taken) + ". Your inventory is full; " + left
                + " left.");
    }

    /**
     * Places one item from a slot at a spot. The spot is checked again here, so what a screen previewed does not
     * matter. If the check fails, nothing is consumed.
     *
     * @param player the player
     * @param inventory the player's inventory
     * @param slot the slot to place from
     * @param x wanted world x
     * @param z wanted world z
     * @param yaw wanted rotation in radians
     * @return what happened
     */
    public ActionResult place(PlayerState player, Inventory inventory, int slot, float x, float z, float yaw) {
        Optional<ItemStack> held = inventory.slot(slot);
        if (held.isEmpty()) {
            return ActionResult.failed("Nothing to place.");
        }
        ItemDefinition definition = catalog.get(held.get().item());
        PlacementCheck check = validator.checkPlace(definition, player.x(), player.z(), x, z);
        if (!check.valid()) {
            return ActionResult.failed(check.problem().message());
        }
        ItemStack taken = inventory.take(slot, 1).orElseThrow();
        WorldItem placed = items.spawn(taken, x, check.y(), z, yaw, true);
        if (definition.placeRadius() > 0f && !check.onSurface()) {
            collision.addObstacle(placed.id(), x, z, definition.placeRadius());
        }
        return ActionResult.ok("Placed the " + definition.name() + ".");
    }

    private static boolean inReach(PlayerState player, WorldItem item) {
        float dx = item.x() - player.x();
        float dy = item.y() + 0.1f - (player.y() + PICKUP_EYE_HEIGHT);
        float dz = item.z() - player.z();
        return dx * dx + dy * dy + dz * dz <= PICKUP_REACH * PICKUP_REACH;
    }

    private static String describe(ItemDefinition definition, int quantity) {
        return quantity == 1 ? "the " + definition.name() : quantity + " " + definition.name();
    }
}
