package com.coldfriend.world.model;

/**
 * A square grid of terrain heights in metres. Vertex {@code (i, j)} sits at x = i * cellSize, z = j * cellSize.
 * The generator edits it while building the world; after generation treat it as read-only.
 */
public final class HeightField {

    private final int cells;
    private final float cellSize;
    private final float[] heights;

    /**
     * @param cells number of cells along each side; the grid has {@code cells + 1} vertices per side
     * @param cellSize cell width in metres
     */
    public HeightField(int cells, float cellSize) {
        if (cells <= 0 || cellSize <= 0f) {
            throw new IllegalArgumentException("Height field needs positive cells and cell size");
        }
        this.cells = cells;
        this.cellSize = cellSize;
        this.heights = new float[(cells + 1) * (cells + 1)];
    }

    /** @return cells along each side */
    public int cells() {
        return cells;
    }

    /** @return vertices along each side */
    public int vertices() {
        return cells + 1;
    }

    /** @return cell width in metres */
    public float cellSize() {
        return cellSize;
    }

    /** @return side length of the world in metres */
    public float sizeMeters() {
        return cells * cellSize;
    }

    /** @return the height at a vertex */
    public float get(int i, int j) {
        return heights[j * (cells + 1) + i];
    }

    /** Sets the height at a vertex. */
    public void set(int i, int j, float value) {
        heights[j * (cells + 1) + i] = value;
    }

    /**
     * Height at a world position, interpolated between vertices. Positions outside the world are clamped to
     * its edge.
     *
     * @param x world x in metres
     * @param z world z in metres
     * @return height in metres
     */
    public float heightAt(float x, float z) {
        float gx = Math.max(0f, Math.min(cells, x / cellSize));
        float gz = Math.max(0f, Math.min(cells, z / cellSize));
        int i = Math.min((int) gx, cells - 1);
        int j = Math.min((int) gz, cells - 1);
        float fx = gx - i;
        float fz = gz - j;
        float h00 = get(i, j);
        float h10 = get(i + 1, j);
        float h01 = get(i, j + 1);
        float h11 = get(i + 1, j + 1);
        float near = h00 + (h10 - h00) * fx;
        float far = h01 + (h11 - h01) * fx;
        return near + (far - near) * fz;
    }

    /**
     * Steepness at a world position.
     *
     * @param x world x in metres
     * @param z world z in metres
     * @return rise over run; 1 is a 45 degree slope
     */
    public float slopeAt(float x, float z) {
        return slopeAt(x, z, cellSize);
    }

    /**
     * Steepness measured over a wider distance, which ignores small bumps.
     *
     * @param x world x in metres
     * @param z world z in metres
     * @param delta half the distance the slope is measured over, in metres
     * @return rise over run
     */
    public float slopeAt(float x, float z, float delta) {
        float dx = (heightAt(x + delta, z) - heightAt(x - delta, z)) / (2f * delta);
        float dz = (heightAt(x, z + delta) - heightAt(x, z - delta)) / (2f * delta);
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /** @return the lowest vertex height */
    public float min() {
        float result = Float.MAX_VALUE;
        for (float h : heights) {
            result = Math.min(result, h);
        }
        return result;
    }

    /** @return the highest vertex height */
    public float max() {
        float result = -Float.MAX_VALUE;
        for (float h : heights) {
            result = Math.max(result, h);
        }
        return result;
    }
}
