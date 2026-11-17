package com.coldfriend.world.query;

import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldData;
import java.util.List;

/**
 * Walkability on a coarse grid, for questions such as "can a person get from here to there". Applies
 * {@link TerrainRules} and treats building footprints as solid. Trees are ignored: they are spaced widely enough
 * to walk between. NPC navigation will build on this.
 */
public final class GridNavigation {

    private static final float BUILDING_MARGIN = 0.5f;
    private static final int[] DI = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DJ = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int SEARCH_RADIUS = 2;

    private final float cell;
    private final int side;
    private final boolean[] walkable;

    /**
     * @param terrain the terrain
     * @param water the water map
     * @param footprints solid building footprints
     * @param cell grid spacing in metres
     */
    public GridNavigation(HeightField terrain, WaterMap water, List<Footprint> footprints, float cell) {
        this.cell = cell;
        this.side = (int) (terrain.sizeMeters() / cell) + 1;
        this.walkable = new boolean[side * side];
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                float x = i * cell;
                float z = j * cell;
                walkable[j * side + i] = TerrainRules.walkable(terrain, water, x, z) && !inside(footprints, x, z);
            }
        }
    }

    /**
     * @param world the generated world
     * @param cell grid spacing in metres
     * @return navigation over the world's terrain, water and buildings
     */
    public static GridNavigation of(WorldData world, float cell) {
        return new GridNavigation(world.terrain(), world.water(), world.footprints(), cell);
    }

    /** All the places reachable from one starting point. */
    public final class Reachable {

        private final boolean[] reached;

        private Reachable(boolean[] reached) {
            this.reached = reached;
        }

        /** @return true if a person at the start could walk to the point */
        public boolean contains(float x, float z) {
            int ci = Math.round(x / cell);
            int cj = Math.round(z / cell);
            for (int dj = -1; dj <= 1; dj++) {
                for (int di = -1; di <= 1; di++) {
                    int i = ci + di;
                    int j = cj + dj;
                    if (i >= 0 && j >= 0 && i < side && j < side && reached[j * side + i]) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * @param x start world x
     * @param z start world z
     * @return everything reachable on foot from the start; empty if no walkable ground is near it
     */
    public Reachable reachableFrom(float x, float z) {
        boolean[] reached = new boolean[side * side];
        int start = nearestWalkable(x, z);
        if (start < 0) {
            return new Reachable(reached);
        }
        int[] queue = new int[side * side];
        int head = 0;
        int tail = 0;
        queue[tail++] = start;
        reached[start] = true;
        while (head < tail) {
            int current = queue[head++];
            int ci = current % side;
            int cj = current / side;
            for (int d = 0; d < DI.length; d++) {
                int ni = ci + DI[d];
                int nj = cj + DJ[d];
                if (ni < 0 || nj < 0 || ni >= side || nj >= side) {
                    continue;
                }
                int next = nj * side + ni;
                if (walkable[next] && !reached[next]) {
                    reached[next] = true;
                    queue[tail++] = next;
                }
            }
        }
        return new Reachable(reached);
    }

    private int nearestWalkable(float x, float z) {
        int ci = Math.round(x / cell);
        int cj = Math.round(z / cell);
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            for (int dj = -r; dj <= r; dj++) {
                for (int di = -r; di <= r; di++) {
                    int i = ci + di;
                    int j = cj + dj;
                    if (i >= 0 && j >= 0 && i < side && j < side && walkable[j * side + i]) {
                        return j * side + i;
                    }
                }
            }
        }
        return -1;
    }

    private static boolean inside(List<Footprint> footprints, float x, float z) {
        for (Footprint footprint : footprints) {
            if (footprint.contains(x, z, BUILDING_MARGIN)) {
                return true;
            }
        }
        return false;
    }
}
