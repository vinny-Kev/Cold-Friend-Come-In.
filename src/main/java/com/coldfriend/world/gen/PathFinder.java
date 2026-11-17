package com.coldfriend.world.gen;

import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldPath.PathPoint;
import com.coldfriend.world.query.TerrainRules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Finds walking routes across the terrain with A* search on a grid. Steep ground and building footprints are
 * impassable. Deep water is allowed at a high price, because a path that must cross a river gets a ford there.
 */
final class PathFinder {

    private static final float SLOPE_WEIGHT = 6f;
    private static final float WADE_WEIGHT = 2f;
    private static final float DEEP_WATER_COST = 25f;
    private static final float BLOCK_MARGIN = 0.8f;
    private static final int[] DI = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DJ = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int INDEX_MASK_SHIFT = 32;
    private static final long INDEX_MASK = 0xFFFFFFFFL;

    private final float grid;
    private final int side;
    private final float[] cost;

    PathFinder(HeightField terrain, WaterMap water, float grid, List<Footprint> blocked) {
        this.grid = grid;
        this.side = (int) (terrain.sizeMeters() / grid) + 1;
        this.cost = new float[side * side];
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                cost[j * side + i] = nodeCost(terrain, water, blocked, i * grid, j * grid);
            }
        }
    }

    private float nodeCost(HeightField terrain, WaterMap water, List<Footprint> blocked, float x, float z) {
        for (Footprint footprint : blocked) {
            if (footprint.contains(x, z, BLOCK_MARGIN)) {
                return Float.POSITIVE_INFINITY;
            }
        }
        float slope = terrain.slopeAt(x, z, grid / 2f);
        if (slope > TerrainRules.MAX_WALK_SLOPE) {
            return Float.POSITIVE_INFINITY;
        }
        float depth = water.depthAt(terrain, x, z);
        float wet = depth <= 0f ? 0f : depth <= TerrainRules.MAX_WADE_DEPTH ? WADE_WEIGHT * depth : DEEP_WATER_COST;
        return 1f + SLOPE_WEIGHT * slope * slope + wet;
    }

    /**
     * @return the route from start to goal, or an empty list if there is none; the first and last points are
     *         exactly the start and goal
     */
    List<PathPoint> find(float startX, float startZ, float goalX, float goalZ) {
        int start = nodeAt(startX, startZ);
        int goal = nodeAt(goalX, goalZ);
        float[] best = new float[side * side];
        int[] parent = new int[side * side];
        boolean[] closed = new boolean[side * side];
        Arrays.fill(best, Float.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);
        best[start] = 0f;
        PriorityQueue<Long> queue = new PriorityQueue<>();
        queue.add(Maths.sortKey(heuristic(start, goal), start));
        while (!queue.isEmpty()) {
            int current = (int) (queue.poll() & INDEX_MASK);
            if (closed[current]) {
                continue;
            }
            closed[current] = true;
            if (current == goal) {
                return route(parent, goal, startX, startZ, goalX, goalZ);
            }
            relax(current, goal, best, parent, closed, queue, start);
        }
        return List.of();
    }

    private void relax(int current, int goal, float[] best, int[] parent, boolean[] closed,
            PriorityQueue<Long> queue, int start) {
        int ci = current % side;
        int cj = current / side;
        for (int d = 0; d < DI.length; d++) {
            int ni = ci + DI[d];
            int nj = cj + DJ[d];
            if (ni < 0 || nj < 0 || ni >= side || nj >= side) {
                continue;
            }
            int next = nj * side + ni;
            if (closed[next]) {
                continue;
            }
            float enter = (next == goal || next == start) ? 1f : cost[next];
            float leave = (current == start) ? 1f : cost[current];
            if (Float.isInfinite(enter) || Float.isInfinite(leave)) {
                continue;
            }
            float step = (float) Math.sqrt((double) DI[d] * DI[d] + (double) DJ[d] * DJ[d]) * grid;
            float through = best[current] + step * (enter + leave) / 2f;
            if (through < best[next]) {
                best[next] = through;
                parent[next] = current;
                queue.add(Maths.sortKey(through + heuristic(next, goal), next));
            }
        }
    }

    private float heuristic(int a, int b) {
        float dx = (a % side - b % side) * grid;
        float dz = (a / side - b / side) * grid;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    private List<PathPoint> route(int[] parent, int goal, float startX, float startZ, float goalX, float goalZ) {
        List<PathPoint> points = new ArrayList<>();
        for (int node = goal; node >= 0; node = parent[node]) {
            points.add(new PathPoint((node % side) * grid, (node / side) * grid));
        }
        Collections.reverse(points);
        points.set(0, new PathPoint(startX, startZ));
        points.set(points.size() - 1, new PathPoint(goalX, goalZ));
        return points;
    }

    private int nodeAt(float x, float z) {
        int i = Math.max(0, Math.min(side - 1, Math.round(x / grid)));
        int j = Math.max(0, Math.min(side - 1, Math.round(z / grid)));
        return j * side + i;
    }
}
