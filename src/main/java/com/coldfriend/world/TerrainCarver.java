package com.coldfriend.world;

import java.util.List;

/**
 * Carves water channels into a {@link BaseTerrain}. Used by {@link GeneratedWorld} for the final terrain and
 * by {@link WaterGenerator} to trace each new river over the terrain already shaped by earlier water, which
 * is what lets rivers follow existing valleys and arrive at lakes at the right level.
 *
 * <p>Holds a live reference to the body list, so bodies added later are seen. Not thread-safe.
 */
final class TerrainCarver {

    private static final int PROXIMITY_POWER = 4;

    private final BaseTerrain base;
    private final List<WaterBody> bodies;
    private final WaterSample scratch = new WaterSample();

    TerrainCarver(BaseTerrain base, List<WaterBody> bodies) {
        this.base = base;
        this.bodies = bodies;
    }

    /**
     * Ground height with every body's channel carved in. Inside a body the ground is the bed; just outside
     * it blends smoothly back to the base terrain.
     *
     * @param x world x
     * @param z world z
     * @return height in metres
     */
    float height(float x, float z) {
        float ground = base.height(x, z);
        float weightedDelta = 0f;
        float weightSum = 0f;
        float strongest = 0f;
        float lowestBed = Float.POSITIVE_INFINITY;
        for (WaterBody body : bodies) {
            if (body.mayInfluence(x, z)) {
                scratch.reset();
                body.sample(x, z, scratch);
                float pull = pull(ground, scratch);
                if (pull > 0f) {
                    float weight = proximityWeight(pull, scratch);
                    weightedDelta += weight * (target(scratch) - ground);
                    weightSum += weight;
                    strongest = Math.max(strongest, pull);
                    if (scratch.distance <= scratch.halfWidth) {
                        lowestBed = Math.min(lowestBed, target(scratch));
                    }
                }
            }
        }
        // Each body pulls the ground toward its channel, fading to zero at its bank edge. The result moves
        // by the strongest pull toward a blend of the bodies' targets weighted by closeness to each body's water
        // edge, so the nearest body dominates and a distant one at a different elevation cannot drag a neighbour's shore. Every term is
        // continuous, so there are no seams; a single body gives exactly its own profile.
        float blended = weightSum == 0f ? ground : ground + strongest * weightedDelta / weightSum;
        // Inside any body's water the ground never rises above that body's own bed, so overlapping bodies
        // cannot leave a hump poking through the surface. Continuous: a bed equals the water level at its edge.
        return Math.min(blended, lowestBed);
    }

    /** Blend weight: 1 at the water, falling steeply with distance from its edge, and 0 where the pull is 0. */
    private static float proximityWeight(float pull, WaterSample s) {
        float falloff = 1f / (1f + Math.max(0f, s.distance - s.halfWidth));
        float weight = pull;
        for (int i = 0; i < PROXIMITY_POWER; i++) {
            weight *= falloff;
        }
        return weight;
    }

    /** Height the ground is pulled toward: the bed inside the water, the water level on the bank. */
    private static float target(WaterSample s) {
        if (s.distance <= s.halfWidth) {
            float u = s.distance / s.halfWidth;
            return s.surfaceY - s.depth * (1f - u * u);
        }
        return s.surfaceY;
    }

    /** 1 inside the water, falling smoothly to 0 at the edge of the bank. */
    private static float pull(float ground, WaterSample s) {
        float w = s.halfWidth;
        if (s.distance <= w) {
            return 1f;
        }
        float bank = WaterBody.bankWidth(w, ground - s.surfaceY);
        if (s.distance >= w + bank) {
            return 0f;
        }
        float t = (s.distance - w) / bank;
        return 1f - t * t * (3f - 2f * t);
    }
}
