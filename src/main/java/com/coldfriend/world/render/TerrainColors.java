package com.coldfriend.world.render;

import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldPath;
import com.coldfriend.world.model.WorldPath.PathPoint;
import com.coldfriend.world.model.VillageBuilding;

/**
 * Works out the ground colour at every terrain vertex: snow, bare rock on steep ground, dark wet ground along
 * riverbanks and beds, shading under dense forest, and trampled earth on paths and in the village square.
 */
final class TerrainColors {

    private static final float[] SNOW = {0.93f, 0.95f, 1.0f};
    private static final float[] ROCK = {0.46f, 0.47f, 0.51f};
    private static final float[] WET = {0.30f, 0.30f, 0.30f};
    private static final float[] BED = {0.18f, 0.20f, 0.23f};
    private static final float[] TRAMPLED = {0.62f, 0.57f, 0.52f};
    private static final float ROCK_START = 0.45f;
    private static final float ROCK_END = 0.9f;
    private static final float BANK_WIDTH = 3f;
    private static final float FOREST_DARKENING = 0.2f;
    private static final float PATH_FADE = 1.2f;
    private static final float SQUARE_RADIUS = 9f;
    private static final float SQUARE_FADE = 5f;
    private static final float BUILDING_FADE = 2.5f;
    private static final float SQUARE_STRENGTH = 0.8f;
    private static final float PATH_STRENGTH = 0.85f;
    private static final int CHANNELS = 3;

    private TerrainColors() {
    }

    /** @return red, green, blue for every vertex, row by row */
    static float[] compute(WorldData world) {
        HeightField terrain = world.terrain();
        int n = terrain.vertices();
        float cell = terrain.cellSize();
        float[] colors = new float[n * n * CHANNELS];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float x = i * cell;
                float z = j * cell;
                float[] c = SNOW.clone();
                float rock = smooth(ROCK_START, ROCK_END, terrain.slopeAt(x, z, cell * 2f));
                mix(c, ROCK, rock);
                float edge = world.water().distance(i, j);
                if (world.water().surface(i, j) > Float.NEGATIVE_INFINITY) {
                    mix(c, BED, 1f);
                } else if (edge < BANK_WIDTH) {
                    mix(c, WET, 1f - edge / BANK_WIDTH);
                }
                float shade = 1f - FOREST_DARKENING * world.regions().forestDensityAt(x, z);
                int base = (j * n + i) * CHANNELS;
                colors[base] = c[0] * shade;
                colors[base + 1] = c[1] * shade;
                colors[base + 2] = c[2] * shade;
            }
        }
        for (WorldPath path : world.paths()) {
            for (int k = 0; k + 1 < path.points().size(); k++) {
                stampSegment(colors, terrain, path.points().get(k), path.points().get(k + 1), path.width() / 2f);
            }
        }
        stampDisc(colors, terrain, world.village().x(), world.village().z(), SQUARE_RADIUS, SQUARE_FADE);
        for (VillageBuilding building : world.village().buildings()) {
            stampFootprint(colors, terrain, building.footprint());
        }
        return colors;
    }

    private static void stampSegment(float[] colors, HeightField terrain, PathPoint a, PathPoint b, float half) {
        float cell = terrain.cellSize();
        float reach = half + PATH_FADE;
        int minI = Math.max(0, (int) Math.floor((Math.min(a.x(), b.x()) - reach) / cell));
        int maxI = Math.min(terrain.cells(), (int) Math.ceil((Math.max(a.x(), b.x()) + reach) / cell));
        int minJ = Math.max(0, (int) Math.floor((Math.min(a.z(), b.z()) - reach) / cell));
        int maxJ = Math.min(terrain.cells(), (int) Math.ceil((Math.max(a.z(), b.z()) + reach) / cell));
        float sx = b.x() - a.x();
        float sz = b.z() - a.z();
        float lengthSquared = Math.max(1e-6f, sx * sx + sz * sz);
        for (int j = minJ; j <= maxJ; j++) {
            for (int i = minI; i <= maxI; i++) {
                float px = i * cell - a.x();
                float pz = j * cell - a.z();
                float t = Math.max(0f, Math.min(1f, (px * sx + pz * sz) / lengthSquared));
                float dx = px - sx * t;
                float dz = pz - sz * t;
                float d = (float) Math.sqrt(dx * dx + dz * dz);
                tint(colors, terrain, i, j, PATH_STRENGTH * (1f - smooth(half, half + PATH_FADE, d)));
            }
        }
    }

    private static void stampDisc(float[] colors, HeightField terrain, float cx, float cz, float radius, float fade) {
        float cell = terrain.cellSize();
        float reach = radius + fade;
        for (int j = Math.max(0, (int) ((cz - reach) / cell)); j <= Math.min(terrain.cells(), (int) ((cz + reach) / cell)); j++) {
            for (int i = Math.max(0, (int) ((cx - reach) / cell)); i <= Math.min(terrain.cells(), (int) ((cx + reach) / cell)); i++) {
                float d = (float) Math.sqrt((i * cell - cx) * (i * cell - cx) + (j * cell - cz) * (j * cell - cz));
                tint(colors, terrain, i, j, SQUARE_STRENGTH * (1f - smooth(radius, radius + fade, d)));
            }
        }
    }

    private static void stampFootprint(float[] colors, HeightField terrain, Footprint f) {
        float cell = terrain.cellSize();
        float reach = f.circumradius() + BUILDING_FADE;
        for (int j = Math.max(0, (int) ((f.z() - reach) / cell)); j <= Math.min(terrain.cells(), (int) ((f.z() + reach) / cell)); j++) {
            for (int i = Math.max(0, (int) ((f.x() - reach) / cell)); i <= Math.min(terrain.cells(), (int) ((f.x() + reach) / cell)); i++) {
                float[] local = f.toLocal(i * cell, j * cell);
                float outsideSide = Math.max(0f, Math.abs(local[0]) - f.halfWidth());
                float outsideForward = Math.max(0f, Math.abs(local[1]) - f.halfDepth());
                float d = (float) Math.sqrt(outsideSide * outsideSide + outsideForward * outsideForward);
                tint(colors, terrain, i, j, SQUARE_STRENGTH * (1f - smooth(0f, BUILDING_FADE, d)));
            }
        }
    }

    private static void tint(float[] colors, HeightField terrain, int i, int j, float strength) {
        if (strength <= 0f) {
            return;
        }
        int base = (j * terrain.vertices() + i) * CHANNELS;
        for (int c = 0; c < CHANNELS; c++) {
            colors[base + c] += (TRAMPLED[c] - colors[base + c]) * strength;
        }
    }

    private static void mix(float[] color, float[] other, float t) {
        for (int c = 0; c < CHANNELS; c++) {
            color[c] += (other[c] - color[c]) * t;
        }
    }

    private static float smooth(float low, float high, float x) {
        float t = Math.max(0f, Math.min(1f, (x - low) / (high - low)));
        return t * t * (3f - 2f * t);
    }
}
