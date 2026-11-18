package com.coldfriend.world.items;

import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemId;
import com.coldfriend.item.ItemStack;
import com.coldfriend.world.SurvivalActions;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.CabinLayout.Surface;
import com.coldfriend.world.model.WorldData;
import java.util.List;

/**
 * The few items that lie in the cabin when a world starts, so there is something to pick up and put back. They
 * are simulation state, not generated world data, and are skipped for any item the catalogue does not define.
 */
public final class StartingItems {

    private static final ItemId WATER = new ItemId("water_container");
    private static final ItemId BRANCH = new ItemId("branch");
    private static final float STOVE_SIDE = 2.2f;
    private static final float STOVE_FORWARD = -1.6f;
    private static final float DOOR_SIDE = -1.4f;
    private static final float DOOR_FORWARD = 1.9f;
    private static final float BRANCH_YAW = 0.7f;

    private StartingItems() {
    }

    /**
     * Puts firewood by the stove, a water container on the table, and two branches by the door.
     *
     * @param world the generated world
     * @param catalog the item definitions
     * @param items where the items are added
     * @param validator used for the resting height
     */
    public static void populate(WorldData world, ItemCatalog catalog, WorldItems items, PlacementValidator validator) {
        if (catalog.contains(SurvivalActions.FIREWOOD)) {
            float[] spot = CabinLayout.point(world.cabin(), STOVE_SIDE, STOVE_FORWARD);
            items.spawn(new ItemStack(SurvivalActions.FIREWOOD, 3), spot[0], validator.restHeight(spot[0], spot[1]), spot[1], 0f,
                    false);
        }
        List<Surface> tables = CabinLayout.supportSurfaces(world.cabin());
        if (catalog.contains(WATER) && !tables.isEmpty()) {
            Surface table = tables.get(0);
            items.spawn(new ItemStack(WATER, 1), table.area().x(), table.top(), table.area().z(), 0f, false);
        }
        if (catalog.contains(BRANCH)) {
            float[] spot = CabinLayout.point(world.cabin(), DOOR_SIDE, DOOR_FORWARD);
            items.spawn(new ItemStack(BRANCH, 2), spot[0], validator.restHeight(spot[0], spot[1]), spot[1],
                    BRANCH_YAW, false);
        }
    }
}
