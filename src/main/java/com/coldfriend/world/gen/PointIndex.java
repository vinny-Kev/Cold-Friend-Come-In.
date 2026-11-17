package com.coldfriend.world.gen;

import java.util.Arrays;

/** A grid index of round objects on the ground, for quickly asking whether a new one would overlap. */
final class PointIndex {

    private static final int INITIAL_CAPACITY = 1024;
    private static final int GROWTH = 2;

    private final float cell;
    private final int side;
    private final int[] head;
    private int[] next = new int[INITIAL_CAPACITY];
    private float[] xs = new float[INITIAL_CAPACITY];
    private float[] zs = new float[INITIAL_CAPACITY];
    private float[] radii = new float[INITIAL_CAPACITY];
    private int count;
    private float largestRadius;

    PointIndex(float worldSize, float cell) {
        this.cell = cell;
        this.side = (int) Math.ceil(worldSize / cell) + 1;
        this.head = new int[side * side];
        Arrays.fill(head, -1);
    }

    /** @return true if a circle at this spot would overlap one already added */
    boolean overlaps(float x, float z, float radius) {
        int reach = (int) Math.ceil((radius + largestRadius) / cell);
        int ci = cellOf(x);
        int cj = cellOf(z);
        for (int j = Math.max(0, cj - reach); j <= Math.min(side - 1, cj + reach); j++) {
            for (int i = Math.max(0, ci - reach); i <= Math.min(side - 1, ci + reach); i++) {
                for (int k = head[j * side + i]; k >= 0; k = next[k]) {
                    float dx = xs[k] - x;
                    float dz = zs[k] - z;
                    float limit = radii[k] + radius;
                    if (dx * dx + dz * dz < limit * limit) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void add(float x, float z, float radius) {
        if (count == xs.length) {
            int size = count * GROWTH;
            next = Arrays.copyOf(next, size);
            xs = Arrays.copyOf(xs, size);
            zs = Arrays.copyOf(zs, size);
            radii = Arrays.copyOf(radii, size);
        }
        int slot = cellOf(z) * side + cellOf(x);
        xs[count] = x;
        zs[count] = z;
        radii[count] = radius;
        next[count] = head[slot];
        head[slot] = count;
        largestRadius = Math.max(largestRadius, radius);
        count++;
    }

    private int cellOf(float coordinate) {
        return Math.max(0, Math.min(side - 1, (int) (coordinate / cell)));
    }
}
