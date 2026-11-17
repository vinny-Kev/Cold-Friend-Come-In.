package com.coldfriend.world.gen;

import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldPath;
import com.coldfriend.world.model.WorldPath.PathPoint;
import java.util.List;

/** Edits terrain around structures and paths: levelled building sites and shallow fords where paths cross water. */
final class TerrainShaping {

    private static final float FORD_RADIUS = 5f;
    private static final float FORD_CORE = 2f;
    private static final float FORD_STEP = 1f;
    private static final float FLOOR_LIFT = 0.15f;

    private TerrainShaping() {
    }

    /** @return the average ground height over a footprint, plus a small lift so floors sit above the snow */
    static float floorHeight(HeightField terrain, Footprint f) {
        float sum = 0f;
        int count = 0;
        for (int sx = -1; sx <= 1; sx++) {
            for (int sz = -1; sz <= 1; sz++) {
                float[] p = f.toWorld(sx * f.halfWidth(), sz * f.halfDepth());
                sum += terrain.heightAt(p[0], p[1]);
                count++;
            }
        }
        return sum / count + FLOOR_LIFT;
    }

    /** @return the greatest height difference across a footprint's nine sample points */
    static float relief(HeightField terrain, Footprint f) {
        float low = Float.MAX_VALUE;
        float high = -Float.MAX_VALUE;
        for (int sx = -1; sx <= 1; sx++) {
            for (int sz = -1; sz <= 1; sz++) {
                float[] p = f.toWorld(sx * f.halfWidth(), sz * f.halfDepth());
                float h = terrain.heightAt(p[0], p[1]);
                low = Math.min(low, h);
                high = Math.max(high, h);
            }
        }
        return high - low;
    }

    /** Levels the ground under a footprint to a floor height and blends it into the surroundings. */
    static void levelRect(HeightField terrain, Footprint f, float floorY, float margin) {
        float reach = f.circumradius() + margin;
        forEachVertex(terrain, f.x(), f.z(), reach, (i, j, x, z) -> {
            float[] local = f.toLocal(x, z);
            float outsideSide = Math.max(0f, Math.abs(local[0]) - f.halfWidth());
            float outsideForward = Math.max(0f, Math.abs(local[1]) - f.halfDepth());
            float d = (float) Math.sqrt(outsideSide * outsideSide + outsideForward * outsideForward);
            blend(terrain, i, j, floorY, 1f - Maths.smoothstep(0f, margin, d));
        });
    }

    /** Levels a disc of ground to a height and blends it into the surroundings. */
    static void levelCircle(HeightField terrain, float cx, float cz, float radius, float height, float margin) {
        forEachVertex(terrain, cx, cz, radius + margin, (i, j, x, z) -> {
            float d = (float) Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
            blend(terrain, i, j, height, 1f - Maths.smoothstep(radius, radius + margin, d));
        });
    }

    /**
     * Raises the river bed where a path crosses water so the crossing is shallow enough to wade. The water
     * surface is unchanged; only the bed rises.
     */
    static void carveFords(HeightField terrain, WaterMap water, WorldPath path, float depth) {
        List<PathPoint> points = path.points();
        for (int k = 0; k + 1 < points.size(); k++) {
            PathPoint a = points.get(k);
            PathPoint b = points.get(k + 1);
            float length = (float) Math.sqrt((b.x() - a.x()) * (b.x() - a.x()) + (b.z() - a.z()) * (b.z() - a.z()));
            int steps = Math.max(1, (int) Math.ceil(length / FORD_STEP));
            for (int s = 0; s <= steps; s++) {
                float t = (float) s / steps;
                float px = Maths.lerp(a.x(), b.x(), t);
                float pz = Maths.lerp(a.z(), b.z(), t);
                if (water.depthAt(terrain, px, pz) > 0f) {
                    raiseBed(terrain, water, px, pz, depth);
                }
            }
        }
    }

    private static void raiseBed(HeightField terrain, WaterMap water, float cx, float cz, float depth) {
        forEachVertex(terrain, cx, cz, FORD_RADIUS, (i, j, x, z) -> {
            float surface = water.surface(i, j);
            if (surface == Float.NEGATIVE_INFINITY) {
                return;
            }
            float d = (float) Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
            float weight = 1f - Maths.smoothstep(FORD_CORE, FORD_RADIUS, d);
            float bed = terrain.get(i, j);
            float target = surface - depth;
            if (bed < target) {
                terrain.set(i, j, bed + (target - bed) * weight);
            }
        });
    }

    private static void blend(HeightField terrain, int i, int j, float target, float weight) {
        float h = terrain.get(i, j);
        terrain.set(i, j, h + (target - h) * weight);
    }

    private interface VertexAction {
        void apply(int i, int j, float x, float z);
    }

    private static void forEachVertex(HeightField terrain, float cx, float cz, float reach, VertexAction action) {
        float cell = terrain.cellSize();
        int minI = Math.max(0, (int) Math.floor((cx - reach) / cell));
        int maxI = Math.min(terrain.cells(), (int) Math.ceil((cx + reach) / cell));
        int minJ = Math.max(0, (int) Math.floor((cz - reach) / cell));
        int maxJ = Math.min(terrain.cells(), (int) Math.ceil((cz + reach) / cell));
        for (int j = minJ; j <= maxJ; j++) {
            for (int i = minI; i <= maxI; i++) {
                action.apply(i, j, i * cell, j * cell);
            }
        }
    }
}
