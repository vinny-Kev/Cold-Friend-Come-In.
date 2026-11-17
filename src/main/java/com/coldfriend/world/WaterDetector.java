package com.coldfriend.world;

/**
 * Tracks one entity's water contact from frame to frame and reports entering and leaving. Allocation-free:
 * call {@link #update} once per tick with the entity's feet position.
 */
public final class WaterDetector {

    /** Open water shallower than this counts as dry (a wet shoreline, not wading). */
    public static final float MIN_WET_DEPTH = 0.1f;
    /** Feet may be this far above the surface and still count as touching it. */
    private static final float SURFACE_TOLERANCE = 0.05f;

    private final GeneratedWorld world;
    private final WaterSample sample = new WaterSample();
    private WaterContact contact = WaterContact.DRY;
    private WaterContact previous = WaterContact.DRY;
    private WaterBody body;
    private float depth;

    /** @param world the world to query */
    public WaterDetector(GeneratedWorld world) {
        this.world = world;
    }

    /**
     * Re-evaluates contact for a feet position.
     *
     * @param x feet x
     * @param feetY feet height
     * @param z feet z
     * @return the new contact state
     */
    public WaterContact update(float x, float feetY, float z) {
        previous = contact;
        world.sampleWater(x, z, sample);
        contact = WaterContact.DRY;
        body = null;
        depth = 0f;
        if (sample.isInside() && feetY <= sample.surfaceY + SURFACE_TOLERANCE) {
            body = sample.body;
            depth = Math.max(0f, sample.surfaceY - world.terrainHeight(x, z));
            if (body.isFrozen()) {
                contact = WaterContact.ON_ICE;
            } else if (depth >= MIN_WET_DEPTH) {
                contact = WaterContact.IN_WATER;
            } else {
                body = null;
            }
        }
        return contact;
    }

    /** @return contact after the latest update */
    public WaterContact contact() {
        return contact;
    }

    /** @return the body currently touched, or null when dry */
    public WaterBody body() {
        return body;
    }

    /** @return water depth under the feet after the latest update, 0 when dry */
    public float depth() {
        return depth;
    }

    /** @return true if the latest update moved from not-in-open-water into it */
    public boolean enteredWater() {
        return contact == WaterContact.IN_WATER && previous != WaterContact.IN_WATER;
    }

    /** @return true if the latest update moved out of open water */
    public boolean leftWater() {
        return previous == WaterContact.IN_WATER && contact != WaterContact.IN_WATER;
    }
}
