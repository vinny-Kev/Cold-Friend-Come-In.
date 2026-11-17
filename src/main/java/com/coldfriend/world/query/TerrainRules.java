package com.coldfriend.world.query;

import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.WaterMap;

/** The rules for where a person can walk. Generation and gameplay both use them, so they always agree. */
public final class TerrainRules {

    /** Steepest ground a person can walk on, as rise over run. */
    public static final float MAX_WALK_SLOPE = 0.7f;
    /** Deepest water a person can wade through, in metres. */
    public static final float MAX_WADE_DEPTH = 0.8f;
    private static final float SLOPE_PROBE = 2f;

    private TerrainRules() {
    }

    /**
     * @param terrain the terrain
     * @param water the water map
     * @param x world x in metres
     * @param z world z in metres
     * @return true if a person can stand at the point: inside the world, not too steep, not too deep in water
     */
    public static boolean walkable(HeightField terrain, WaterMap water, float x, float z) {
        float size = terrain.sizeMeters();
        if (x < 0f || z < 0f || x > size || z > size) {
            return false;
        }
        return terrain.slopeAt(x, z, SLOPE_PROBE) <= MAX_WALK_SLOPE
                && water.depthAt(terrain, x, z) <= MAX_WADE_DEPTH;
    }
}
