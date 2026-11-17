package com.coldfriend.world.model;

/**
 * Where there is water, and how far every point is from a river. Uses the same grid as the terrain
 * {@link HeightField}.
 */
public final class WaterMap {

    private final int cells;
    private final float cellSize;
    private final float[] surface;
    private final float[] riverDistance;

    /**
     * @param cells cells along each side, matching the height field
     * @param cellSize cell width in metres
     * @param surface water surface height per vertex, or negative infinity where there is no water
     * @param riverDistance distance to the nearest river centre line per vertex, capped
     */
    public WaterMap(int cells, float cellSize, float[] surface, float[] riverDistance) {
        int count = (cells + 1) * (cells + 1);
        if (surface.length != count || riverDistance.length != count) {
            throw new IllegalArgumentException("Water arrays must match the height field");
        }
        this.cells = cells;
        this.cellSize = cellSize;
        this.surface = surface.clone();
        this.riverDistance = riverDistance.clone();
    }

    /** @return the water surface at a vertex, or negative infinity if dry */
    public float surface(int i, int j) {
        return surface[j * (cells + 1) + i];
    }

    /** @return the distance to the nearest river at a vertex, in metres */
    public float distance(int i, int j) {
        return riverDistance[j * (cells + 1) + i];
    }

    /**
     * @param x world x in metres
     * @param z world z in metres
     * @return the water surface height at the nearest vertex, or negative infinity if dry
     */
    public float surfaceAt(float x, float z) {
        return surface(nearest(x), nearest(z));
    }

    /**
     * @param x world x in metres
     * @param z world z in metres
     * @return true if the nearest vertex is under water
     */
    public boolean isWaterAt(float x, float z) {
        return surfaceAt(x, z) > Float.NEGATIVE_INFINITY;
    }

    /**
     * @param x world x in metres
     * @param z world z in metres
     * @return distance to the nearest river centre line in metres, capped at the generator's search range
     */
    public float distanceAt(float x, float z) {
        return distance(nearest(x), nearest(z));
    }

    /**
     * Water depth over the terrain at a position.
     *
     * @param terrain the terrain height field
     * @param x world x in metres
     * @param z world z in metres
     * @return depth in metres; 0 where dry
     */
    public float depthAt(HeightField terrain, float x, float z) {
        float level = surfaceAt(x, z);
        if (level == Float.NEGATIVE_INFINITY) {
            return 0f;
        }
        return Math.max(0f, level - terrain.heightAt(x, z));
    }

    private int nearest(float coordinate) {
        return Math.max(0, Math.min(cells, Math.round(coordinate / cellSize)));
    }
}
