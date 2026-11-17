package com.coldfriend.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Decides where bushes stand in a {@link GeneratedWorld}. Deterministic for a world's seed and
 * {@link #PLACER_VERSION}, and on its own random stream, so adding or changing bushes never moves terrain or
 * water. Bushes keep clear of water and its shoreline and keep a minimum spacing.
 */
public final class BushPlacer {

    /** Bump when a change alters bush placement for existing seeds. */
    public static final int PLACER_VERSION = 1;
    /** Number of distinct bush shapes. */
    public static final int VARIANTS = 3;
    /** Bushes stay at least this far from any water's edge, in metres. */
    public static final float SHORE_CLEARANCE = 2.5f;
    /** Minimum distance between two bushes, in metres. */
    public static final float MIN_SPACING = 3f;

    private static final long STREAM_SALT = 0xB05E5L;
    private static final int ATTEMPTS = 700;
    private static final float EDGE_MARGIN = 6f;
    private static final float MIN_SCALE = 0.8f;
    private static final float MAX_SCALE = 1.5f;
    private static final float MIN_TINT = 0.85f;
    private static final float MAX_TINT = 1.15f;

    private BushPlacer() {
    }

    /**
     * Places bushes on a world.
     *
     * @param world the generated world
     * @return immutable list of bushes
     */
    public static List<BushSpawn> place(GeneratedWorld world) {
        Random random = new Random(world.seed() * 0x9E3779B97F4A7C15L + PLACER_VERSION + STREAM_SALT);
        WaterSample sample = new WaterSample();
        float range = BaseTerrain.HALF_EXTENT - EDGE_MARGIN;
        List<BushSpawn> bushes = new ArrayList<>();
        for (int i = 0; i < ATTEMPTS; i++) {
            // Draw every value for every attempt so rejecting one attempt never shifts the next.
            float x = (random.nextFloat() * 2f - 1f) * range;
            float z = (random.nextFloat() * 2f - 1f) * range;
            float yaw = random.nextFloat() * (float) (2 * Math.PI);
            float scale = MIN_SCALE + random.nextFloat() * (MAX_SCALE - MIN_SCALE);
            int variant = random.nextInt(VARIANTS);
            float tint = MIN_TINT + random.nextFloat() * (MAX_TINT - MIN_TINT);
            world.sampleWater(x, z, sample);
            if (sample.body != null && sample.margin < SHORE_CLEARANCE) {
                continue;
            }
            if (tooClose(bushes, x, z)) {
                continue;
            }
            bushes.add(new BushSpawn(x, world.terrainHeight(x, z), z, yaw, scale, variant, tint));
        }
        return List.copyOf(bushes);
    }

    private static boolean tooClose(List<BushSpawn> bushes, float x, float z) {
        for (BushSpawn b : bushes) {
            if (Math.hypot(x - b.x(), z - b.z()) < MIN_SPACING) {
                return true;
            }
        }
        return false;
    }
}
