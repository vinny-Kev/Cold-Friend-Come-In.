package com.coldfriend.world.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.world.model.River;
import com.coldfriend.world.model.River.RiverPoint;
import com.coldfriend.world.model.WorldData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldGeneratorTest {

    private static final long[] SEEDS = {101L, 202L, 303L, 404L};
    private static final int SAMPLE_GRID = 48;
    private static final float MIN_MEAN_HEIGHT_DIFFERENCE = 6f;
    private static final float MAX_RIVER_OVERLAP = 0.2f;
    private static final float VALLEY_PROBE = 30f;
    private static final float MAX_CHANGED_TERRAIN = 0.05f;

    @Test
    void sameSeedGivesIdenticalWorld() {
        WorldData first = TestWorlds.standard(SEEDS[0]);
        WorldData second = WorldGenerator.generate(new WorldSeed(SEEDS[0]), WorldGenParameters.DEFAULT);

        assertEquals(TestWorlds.terrainHash(first), TestWorlds.terrainHash(second));
        assertEquals(first.rivers(), second.rivers());
        assertEquals(first.placements(), second.placements());
        assertEquals(first.cabin(), second.cabin());
        assertEquals(first.village(), second.village());
        assertEquals(first.paths(), second.paths());
        assertEquals(first.locations(), second.locations());
        assertEquals(first.parametersFingerprint(), second.parametersFingerprint());
        for (int k = 0; k < SAMPLE_GRID; k++) {
            float x = k * first.sizeMeters() / SAMPLE_GRID;
            assertEquals(first.regions().regionAt(x, x), second.regions().regionAt(x, x));
            assertEquals(first.water().surfaceAt(x, x), second.water().surfaceAt(x, x));
        }
    }

    @Test
    void worldIsReproducibleAcrossRuns() {
        // Pinned value: if this changes, generation no longer matches earlier builds, so bump GenerationVersion.
        assertEquals(GoldenValues.TERRAIN_HASH_SEED_101, TestWorlds.terrainHash(TestWorlds.standard(101L)));
    }

    @Test
    void differentSeedsGiveDifferentGeography() {
        for (int a = 0; a < SEEDS.length; a++) {
            for (int b = a + 1; b < SEEDS.length; b++) {
                WorldData first = TestWorlds.standard(SEEDS[a]);
                WorldData second = TestWorlds.standard(SEEDS[b]);
                assertTrue(meanHeightDifference(first, second) > MIN_MEAN_HEIGHT_DIFFERENCE,
                        "terrain too similar for seeds " + SEEDS[a] + " and " + SEEDS[b]);
                assertTrue(riverOverlap(first, second) < MAX_RIVER_OVERLAP,
                        "rivers follow the same course for seeds " + SEEDS[a] + " and " + SEEDS[b]);
            }
        }
    }

    @Test
    void riversRunDownhillAndSitInValleys() {
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            float depthSum = 0f;
            int probes = 0;
            for (River river : world.rivers()) {
                List<RiverPoint> points = river.points();
                for (int k = 1; k < points.size(); k++) {
                    assertTrue(points.get(k).level() <= points.get(k - 1).level() + 0.001f,
                            "river rises downstream, seed " + seed);
                }
                for (int k = 0; k < points.size(); k += 6) {
                    RiverPoint p = points.get(k);
                    float bed = world.terrain().heightAt(p.x(), p.z());
                    assertTrue(bed < p.level(), "river bed is above its water, seed " + seed);
                    float around = (world.terrain().heightAt(p.x() + VALLEY_PROBE, p.z())
                            + world.terrain().heightAt(p.x() - VALLEY_PROBE, p.z())
                            + world.terrain().heightAt(p.x(), p.z() + VALLEY_PROBE)
                            + world.terrain().heightAt(p.x(), p.z() - VALLEY_PROBE)) / 4f;
                    depthSum += around - bed;
                    probes++;
                }
            }
            assertTrue(depthSum / probes > 0.5f, "rivers do not sit lower than their surroundings, seed " + seed);
        }
    }

    @Test
    void riverReachesTheMapEdgeAndWaterExists() {
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            assertTrue(WorldValidator.validate(world).isEmpty(), "validation failed for seed " + seed);
            assertTrue(countWater(world) > 0, "no water for seed " + seed);
        }
    }

    @Test
    void generationVersionAndSeedAreRecorded() {
        WorldData world = TestWorlds.standard(SEEDS[1]);

        assertEquals(GenerationVersion.CURRENT, world.generationVersion());
        assertEquals(SEEDS[1], world.seed());
    }

    @Test
    void changingRegionTuningDoesNotMoveRiversOrTheLand() {
        WorldData base = TestWorlds.standard(SEEDS[2]);
        WorldGenParameters tuned = WorldGenParameters.DEFAULT.withRegionTuning(-0.3f, 0.05f);

        WorldData other = WorldGenerator.generate(new WorldSeed(SEEDS[2]), tuned);

        assertNotEquals(base.parametersFingerprint(), other.parametersFingerprint());
        assertEquals(base.rivers(), other.rivers());
        // Region tuning can move the cabin and village, which reshapes their sites; the rest of the land is the same.
        assertTrue(fractionOfTerrainChanged(base, other) < MAX_CHANGED_TERRAIN, "terrain moved with region tuning");
        assertTrue(regionsDiffer(base, other), "region tuning should change the regions");
    }

    @Test
    void validatorRejectsARiverThatRisesDownstream() {
        WorldData world = TestWorlds.standard(SEEDS[0]);
        List<RiverPoint> bad = new ArrayList<>(world.rivers().get(0).points());
        RiverPoint last = bad.get(bad.size() - 1);
        bad.set(bad.size() - 1, new RiverPoint(last.x(), last.z(), last.level() + 50f, last.halfWidth()));
        List<River> rivers = new ArrayList<>(world.rivers());
        rivers.set(0, new River(bad));
        WorldData broken = new WorldData(world.seed(), world.generationVersion(), world.attempt(),
                world.parametersFingerprint(), world.terrain(), rivers, world.water(), world.regions(),
                world.placements(), world.cabin(), world.village(), world.paths(), world.locations());

        assertFalse(WorldValidator.validate(broken).isEmpty());
    }

    @Test
    void writesMapsForVisualInspection() throws IOException {
        for (long seed : SEEDS) {
            WorldMapRenderer.write(TestWorlds.standard(seed), Path.of("build", "world-maps", "seed-" + seed + ".png"));
        }
    }

    private static float meanHeightDifference(WorldData a, WorldData b) {
        float sum = 0f;
        for (int j = 0; j < SAMPLE_GRID; j++) {
            for (int i = 0; i < SAMPLE_GRID; i++) {
                float x = (i + 0.5f) * a.sizeMeters() / SAMPLE_GRID;
                float z = (j + 0.5f) * a.sizeMeters() / SAMPLE_GRID;
                sum += Math.abs(a.terrain().heightAt(x, z) - b.terrain().heightAt(x, z));
            }
        }
        return sum / (SAMPLE_GRID * SAMPLE_GRID);
    }

    /** Share of the first world's water that is also water in the second. */
    private static float riverOverlap(WorldData a, WorldData b) {
        int water = 0;
        int shared = 0;
        int n = a.terrain().cells();
        float cell = a.terrain().cellSize();
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                if (a.water().isWaterAt(i * cell, j * cell)) {
                    water++;
                    if (b.water().isWaterAt(i * cell, j * cell)) {
                        shared++;
                    }
                }
            }
        }
        return water == 0 ? 0f : (float) shared / water;
    }

    private static int countWater(WorldData world) {
        int count = 0;
        int n = world.terrain().cells();
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                if (world.water().surface(i, j) > Float.NEGATIVE_INFINITY) {
                    count++;
                }
            }
        }
        return count;
    }

    private static float fractionOfTerrainChanged(WorldData a, WorldData b) {
        int n = a.terrain().vertices();
        int changed = 0;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (a.terrain().get(i, j) != b.terrain().get(i, j)) {
                    changed++;
                }
            }
        }
        return (float) changed / (n * n);
    }

    private static boolean regionsDiffer(WorldData a, WorldData b) {
        int side = a.regions().vertices();
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                if (a.regions().regionAtVertex(i, j) != b.regions().regionAtVertex(i, j)) {
                    return true;
                }
            }
        }
        return false;
    }
}
