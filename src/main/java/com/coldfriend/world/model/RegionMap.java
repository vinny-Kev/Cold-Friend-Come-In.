package com.coldfriend.world.model;

/**
 * Which {@link Region} each part of the world belongs to, plus a smooth forest density that blends between
 * regions so vegetation changes gradually at their borders. Stored on a grid coarser than the terrain.
 */
public final class RegionMap {

    private final int vertices;
    private final float cellSize;
    private final Region[] regions;
    private final float[] density;

    /**
     * @param vertices grid vertices along each side
     * @param cellSize grid spacing in metres
     * @param regions region per vertex, row by row
     * @param density forest density per vertex from 0 (none) to 1 (densest)
     */
    public RegionMap(int vertices, float cellSize, Region[] regions, float[] density) {
        if (regions.length != vertices * vertices || density.length != regions.length) {
            throw new IllegalArgumentException("Region arrays must match the grid size");
        }
        this.vertices = vertices;
        this.cellSize = cellSize;
        this.regions = regions.clone();
        this.density = density.clone();
    }

    /** @return grid spacing in metres */
    public float cellSize() {
        return cellSize;
    }

    /** @return grid vertices along each side */
    public int vertices() {
        return vertices;
    }

    /** @return the region at a grid vertex */
    public Region regionAtVertex(int i, int j) {
        return regions[j * vertices + i];
    }

    /**
     * @param x world x in metres
     * @param z world z in metres
     * @return the region at the nearest grid vertex
     */
    public Region regionAt(float x, float z) {
        return regions[nearest(z) * vertices + nearest(x)];
    }

    /**
     * @param x world x in metres
     * @param z world z in metres
     * @return forest density from 0 to 1, interpolated between grid vertices
     */
    public float forestDensityAt(float x, float z) {
        int max = vertices - 1;
        float gx = Math.max(0f, Math.min(max, x / cellSize));
        float gz = Math.max(0f, Math.min(max, z / cellSize));
        int i = Math.min((int) gx, max - 1);
        int j = Math.min((int) gz, max - 1);
        float fx = gx - i;
        float fz = gz - j;
        float d00 = density[j * vertices + i];
        float d10 = density[j * vertices + i + 1];
        float d01 = density[(j + 1) * vertices + i];
        float d11 = density[(j + 1) * vertices + i + 1];
        float near = d00 + (d10 - d00) * fx;
        float far = d01 + (d11 - d01) * fx;
        return near + (far - near) * fz;
    }

    private int nearest(float coordinate) {
        return Math.max(0, Math.min(vertices - 1, Math.round(coordinate / cellSize)));
    }
}
