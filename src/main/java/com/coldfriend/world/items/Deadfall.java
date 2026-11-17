package com.coldfriend.world.items;

import com.coldfriend.entity.EntityId;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemStack;
import com.coldfriend.player.PlayerMovement;
import com.coldfriend.world.SurvivalActions;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.TerrainRules;
import com.coldfriend.world.query.WorldCollision;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Where the player finds firewood in the forest, so the stove can be kept going after the starting supply runs out.
 * Stacks of dead wood are scattered around the cabin when the world starts and a few more appear each new day,
 * up to a cap on the stacks made here (wood the player drops does not count toward it). The spots come from the world seed and the day number alone, so the same seed always gives the same
 * wood, and nothing here reads the clock or the frame rate. Wood is ordinary world items: picked up through the
 * normal pick-up path, so nothing is duplicated or lost.
 */
public final class Deadfall {

    /** Stacks lying around when a world starts. */
    public static final int INITIAL_STACKS = 10;
    /** New stacks that appear at the start of each in-game day. */
    public static final int DAILY_STACKS = 3;
    /** Most stacks from this class lying at once; new ones wait until some are picked up. */
    public static final int MAX_STACKS = 12;
    /** Closest to the cabin a stack appears, in metres. */
    public static final float MIN_DISTANCE = 20f;
    /** Furthest from the cabin a stack appears, in metres. */
    public static final float MAX_DISTANCE = 90f;
    /** Fewest and most pieces in one stack. */
    public static final int MIN_PIECES = 2;
    /** @see #MIN_PIECES */
    public static final int MAX_PIECES = 3;
    private static final float SPACING = 4f;
    private static final float WET = 0.05f;
    private static final int ATTEMPTS_PER_STACK = 40;
    private static final long DAY_MIX = 0x9E3779B97F4A7C15L;

    private final WorldData world;
    private final ItemCatalog catalog;
    private final WorldCollision collision;
    private final WorldItems items;
    private final PlacementValidator validator;
    private final List<EntityId> mine = new ArrayList<>();

    /**
     * @param world the generated world
     * @param catalog the item definitions; nothing is placed if firewood is not in it
     * @param collision tells where a person can stand
     * @param items where the wood is added
     * @param validator gives the height things rest at
     */
    public Deadfall(WorldData world, ItemCatalog catalog, WorldCollision collision, WorldItems items,
            PlacementValidator validator) {
        this.world = world;
        this.catalog = catalog;
        this.collision = collision;
        this.items = items;
        this.validator = validator;
    }

    /**
     * Adds up to {@code count} stacks of firewood, fewer if the cap is reached or no good spot is found.
     *
     * @param day the in-game day; with the world seed it decides every spot
     * @param count how many stacks to try to add
     * @return how many were added
     */
    public int scatter(int day, int count) {
        if (!catalog.contains(SurvivalActions.FIREWOOD)) {
            return 0;
        }
        Random random = new Random(world.seed() ^ (day * DAY_MIX));
        Footprint cabin = world.cabin().footprint();
        int added = 0;
        for (int i = 0; i < count && stillLying() < MAX_STACKS; i++) {
            if (addOne(random, cabin)) {
                added++;
            }
        }
        return added;
    }

    private boolean addOne(Random random, Footprint cabin) {
        for (int attempt = 0; attempt < ATTEMPTS_PER_STACK; attempt++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            // Spread by area, so stacks are not bunched near the cabin.
            double near = (double) MIN_DISTANCE * MIN_DISTANCE;
            double far = (double) MAX_DISTANCE * MAX_DISTANCE;
            double distance = StrictMath.sqrt(near + random.nextDouble() * (far - near));
            float x = cabin.x() + (float) (StrictMath.sin(angle) * distance);
            float z = cabin.z() + (float) (StrictMath.cos(angle) * distance);
            int pieces = MIN_PIECES + random.nextInt(MAX_PIECES - MIN_PIECES + 1);
            if (!suitable(x, z)) {
                continue;
            }
            mine.add(items.spawn(new ItemStack(SurvivalActions.FIREWOOD, pieces), x, validator.restHeight(x, z), z,
                    (float) (random.nextDouble() * 2.0 * Math.PI), false).id());
            return true;
        }
        return false;
    }

    /** @return true if the spot is dry, walkable, clear of obstacles and away from other items */
    private boolean suitable(float x, float z) {
        if (!TerrainRules.walkable(world.terrain(), world.water(), x, z)
                || world.water().depthAt(world.terrain(), x, z) > WET
                || !collision.canStandAt(x, z, PlayerMovement.BODY_RADIUS)) {
            return false;
        }
        for (WorldItem other : items.all()) {
            float dx = other.x() - x;
            float dz = other.z() - z;
            if (dx * dx + dz * dz < SPACING * SPACING) {
                return false;
            }
        }
        return true;
    }

    /** @return how many of the stacks this class made are still on the ground; wood the player dropped does not count */
    private int stillLying() {
        mine.removeIf(id -> items.get(id).isEmpty());
        return mine.size();
    }
}
