package com.coldfriend.world;

import java.util.List;

/**
 * A generated world's ground and water as plain data with queries. Terrain is the seeded base noise with
 * water channels carved into it; every query is analytic (no meshes, no physics engine), so it matches what
 * the renderer draws only because both read the same {@link WaterBody} data.
 *
 * <p>Not thread-safe: queries share an internal scratch {@link WaterSample}. Use from the game thread, or
 * call {@link #sampleWater} with your own sample.
 */
public final class GeneratedWorld {

    private final long seed;
    private final List<WaterBody> bodies;
    private final WaterSample scratch = new WaterSample();
    private final TerrainCarver carver;

    private GeneratedWorld(long seed, BaseTerrain base, List<WaterBody> bodies) {
        this.seed = seed;
        this.bodies = List.copyOf(bodies);
        this.carver = new TerrainCarver(base, this.bodies);
    }

    /**
     * Generates a world. The same seed always gives the same terrain and water.
     *
     * @param seed world seed
     * @return the world
     */
    public static GeneratedWorld generate(long seed) {
        BaseTerrain base = new BaseTerrain(seed);
        return new GeneratedWorld(seed, base, WaterGenerator.generate(seed, base));
    }

    /**
     * Builds a world from explicit bodies, for tests and tools.
     *
     * @param seed seed for the base terrain
     * @param bodies water bodies to carve in
     * @return the world
     */
    static GeneratedWorld of(long seed, List<WaterBody> bodies) {
        return new GeneratedWorld(seed, new BaseTerrain(seed), bodies);
    }

    /** @return the world seed */
    public long seed() {
        return seed;
    }

    /** @return all water bodies, immutable */
    public List<WaterBody> waterBodies() {
        return bodies;
    }

    /**
     * Ground height with water channels carved in. Inside a body the ground is the bed; just outside it
     * blends smoothly back to the base terrain.
     *
     * @param x world x
     * @param z world z
     * @return height in metres
     */
    public float terrainHeight(float x, float z) {
        return carver.height(x, z);
    }

    /**
     * Finds the water body nearest a point. Read {@link WaterSample#isInside()} for containment.
     *
     * @param x world x
     * @param z world z
     * @param out reusable result, reset by this call
     */
    public void sampleWater(float x, float z, WaterSample out) {
        out.reset();
        for (WaterBody body : bodies) {
            if (body.mayInfluence(x, z)) {
                body.sample(x, z, out);
            }
        }
    }

    /**
     * @param x world x
     * @param z world z
     * @return the body whose water contains the point, or null
     */
    public WaterBody waterBodyAt(float x, float z) {
        sampleWater(x, z, scratch);
        return scratch.isInside() ? scratch.body : null;
    }

    /**
     * @param x world x
     * @param z world z
     * @return water surface height, or NaN if the point is not over water
     */
    public float surfaceHeight(float x, float z) {
        sampleWater(x, z, scratch);
        return scratch.isInside() ? scratch.surfaceY : Float.NaN;
    }

    /**
     * @param x world x
     * @param z world z
     * @return water depth above the carved bed, 0 if the point is not over water
     */
    public float depthAt(float x, float z) {
        sampleWater(x, z, scratch);
        return scratch.isInside() ? Math.max(0f, scratch.surfaceY - terrainHeight(x, z)) : 0f;
    }

    /**
     * Height a walking entity stands on: the ice top over frozen water, otherwise the terrain.
     *
     * @param x world x
     * @param z world z
     * @return standing height in metres
     */
    public float groundHeight(float x, float z) {
        sampleWater(x, z, scratch);
        if (scratch.isInside() && scratch.body.isFrozen()) {
            return scratch.surfaceY;
        }
        return terrainHeight(x, z);
    }
}
