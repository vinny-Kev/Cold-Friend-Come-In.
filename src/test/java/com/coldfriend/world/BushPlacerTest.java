package com.coldfriend.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BushPlacerTest {

    private static final long[] SEEDS = {1L, 2L, 7L, 42L, 710_17L, 123_456_789L, -5L, 99L};

    @Test
    void sameSeedGivesTheSameBushes() {
        List<BushSpawn> a = BushPlacer.place(GeneratedWorld.generate(42L));
        List<BushSpawn> b = BushPlacer.place(GeneratedWorld.generate(42L));

        assertEquals(a, b);
    }

    @Test
    void differentSeedsGiveDifferentBushes() {
        List<BushSpawn> a = BushPlacer.place(GeneratedWorld.generate(1L));
        List<BushSpawn> b = BushPlacer.place(GeneratedWorld.generate(2L));

        assertNotEquals(a, b);
    }

    @Test
    void everySeedGetsAUsefulNumberOfBushes() {
        for (long seed : SEEDS) {
            int count = BushPlacer.place(GeneratedWorld.generate(seed)).size();
            assertTrue(count > 100, "bushes for seed " + seed + ": " + count);
        }
    }

    @Test
    void bushesStayInTheWorldOnTheGroundAndOutOfTheWater() {
        WaterSample sample = new WaterSample();
        for (long seed : SEEDS) {
            GeneratedWorld world = GeneratedWorld.generate(seed);
            for (BushSpawn bush : BushPlacer.place(world)) {
                assertTrue(Math.abs(bush.x()) < BaseTerrain.HALF_EXTENT, "x in world, seed " + seed);
                assertTrue(Math.abs(bush.z()) < BaseTerrain.HALF_EXTENT, "z in world, seed " + seed);
                assertEquals(world.terrainHeight(bush.x(), bush.z()), bush.y(), 1e-4f);
                world.sampleWater(bush.x(), bush.z(), sample);
                assertTrue(sample.body == null || sample.margin >= BushPlacer.SHORE_CLEARANCE,
                        "bush clear of water, seed " + seed);
                assertTrue(bush.variant() >= 0 && bush.variant() < BushPlacer.VARIANTS);
            }
        }
    }

    @Test
    void bushesKeepTheirMinimumSpacing() {
        List<BushSpawn> bushes = BushPlacer.place(GeneratedWorld.generate(7L));
        for (int i = 0; i < bushes.size(); i++) {
            for (int j = i + 1; j < bushes.size(); j++) {
                double d = Math.hypot(bushes.get(i).x() - bushes.get(j).x(), bushes.get(i).z() - bushes.get(j).z());
                assertTrue(d >= BushPlacer.MIN_SPACING, "spacing " + d);
            }
        }
    }

    @Test
    void placingBushesNeverChangesTheWorld() {
        GeneratedWorld world = GeneratedWorld.generate(42L);
        float before = world.terrainHeight(13.3f, -41.7f);
        int bodies = world.waterBodies().size();

        BushPlacer.place(world);

        assertEquals(before, world.terrainHeight(13.3f, -41.7f));
        assertEquals(bodies, world.waterBodies().size());
    }
}
