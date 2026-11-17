package com.coldfriend.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratedWorldTest {

    private static final long[] SEEDS = {1L, 2L, 7L, 42L, 710_17L, 123_456_789L, -5L, 99L};
    private static final float EPS = 1e-3f;
    private static final float BANK_TOLERANCE = 0.5f;

    private static WaterBody lake(float x, float z, float radius, float surface, boolean frozen) {
        return new WaterBody(WaterType.LAKE, new float[] {x}, new float[] {z}, new float[] {radius},
                new float[] {surface}, new float[] {2f}, frozen, 0.3f);
    }

    private static WaterBody river(boolean frozen) {
        return new WaterBody(WaterType.RIVER, new float[] {0f, 10f, 20f}, new float[] {0f, 0f, 0f},
                new float[] {2f, 2f, 4f}, new float[] {5f, 4f, 3f}, new float[] {1f, 1f, 1f}, frozen, 0.2f);
    }

    @Test
    void sameSeedGivesIdenticalWorld() {
        GeneratedWorld a = GeneratedWorld.generate(42L);
        GeneratedWorld b = GeneratedWorld.generate(42L);

        assertEquals(a.waterBodies().size(), b.waterBodies().size());
        for (int i = 0; i < a.waterBodies().size(); i++) {
            WaterBody x = a.waterBodies().get(i);
            WaterBody y = b.waterBodies().get(i);
            assertEquals(x.type(), y.type());
            assertEquals(x.isFrozen(), y.isFrozen());
            assertEquals(x.iceThickness(), y.iceThickness());
            assertEquals(x.pointCount(), y.pointCount());
            for (int p = 0; p < x.pointCount(); p++) {
                assertEquals(x.x(p), y.x(p));
                assertEquals(x.z(p), y.z(p));
                assertEquals(x.surface(p), y.surface(p));
                assertEquals(x.halfWidth(p), y.halfWidth(p));
            }
        }
        assertEquals(a.terrainHeight(13.3f, -41.7f), b.terrainHeight(13.3f, -41.7f));
    }

    @Test
    void differentSeedsGiveDifferentWorlds() {
        GeneratedWorld a = GeneratedWorld.generate(1L);
        GeneratedWorld b = GeneratedWorld.generate(2L);

        assertNotEquals(a.terrainHeight(10f, 10f), b.terrainHeight(10f, 10f));
        assertNotEquals(a.waterBodies().get(0).x(0), b.waterBodies().get(0).x(0));
    }

    @Test
    void everySeedProducesFlowingAndStillWater() {
        for (long seed : SEEDS) {
            List<WaterBody> bodies = GeneratedWorld.generate(seed).waterBodies();
            assertTrue(bodies.stream().anyMatch(b -> b.type().isFlowing()), "flowing water, seed " + seed);
            assertTrue(bodies.stream().anyMatch(b -> !b.type().isFlowing()), "still water, seed " + seed);
        }
    }

    @Test
    void riversStayInsideTheWorldAndNeverFlowUphill() {
        for (long seed : SEEDS) {
            for (WaterBody body : GeneratedWorld.generate(seed).waterBodies()) {
                for (int i = 0; i < body.pointCount(); i++) {
                    assertTrue(Math.abs(body.x(i)) <= BaseTerrain.HALF_EXTENT, "x in world, seed " + seed);
                    assertTrue(Math.abs(body.z(i)) <= BaseTerrain.HALF_EXTENT, "z in world, seed " + seed);
                    if (i > 0 && body.type().isFlowing()) {
                        assertTrue(body.surface(i) <= body.surface(i - 1) + EPS, "surface rises, seed " + seed);
                    }
                }
            }
        }
    }

    @Test
    void waterSitsInAChannelCarvedBelowItsSurfaceWithBanksAtOrAboveIt() {
        for (long seed : SEEDS) {
            GeneratedWorld world = GeneratedWorld.generate(seed);
            WaterSample near = new WaterSample();
            for (WaterBody body : world.waterBodies()) {
                for (int i = 0; i < body.pointCount(); i++) {
                    float bed = world.terrainHeight(body.x(i), body.z(i));
                    assertTrue(bed < body.surface(i), "centreline bed under surface, seed " + seed);
                    // Just outside the water's edge, in every direction, ground is not below the water level.
                    for (int k = 0; k < 8; k++) {
                        double a = Math.PI * k / 4;
                        float r = body.halfWidth(i) + 0.3f;
                        float x = body.x(i) + (float) Math.cos(a) * r;
                        float z = body.z(i) + (float) Math.sin(a) * r;
                        world.sampleWater(x, z, near);
                        if (!near.isInside()) {
                            // A river on a side slope may have a downhill bank a little under its level.
                            assertTrue(world.terrainHeight(x, z) >= near.surfaceY - BANK_TOLERANCE,
                                    "bank near local water level, seed " + seed);
                        }
                    }
                }
            }
        }
    }

    @Test
    void terrainChangesSmoothlyAcrossTheBank() {
        GeneratedWorld world = GeneratedWorld.generate(42L);
        WaterBody body = world.waterBodies().stream().filter(b -> b.type() == WaterType.RIVER).findFirst()
                .orElseThrow();
        float step = 0.25f;
        float x = body.x(3);
        float z = body.z(3);
        float previous = world.terrainHeight(x, z);
        for (int i = 1; i < 80; i++) {
            float h = world.terrainHeight(x + i * step, z);
            assertTrue(Math.abs(h - previous) < 1.5f, "no cliff step at offset " + i * step);
            previous = h;
        }
    }

    @Test
    void lakeContainmentBoundaryIsExact() {
        GeneratedWorld world = GeneratedWorld.of(1L, List.of(lake(0f, 0f, 10f, 5f, false)));

        assertNotNull(world.waterBodyAt(9.99f, 0f));
        assertNotNull(world.waterBodyAt(10f, 0f));
        assertNull(world.waterBodyAt(10.01f, 0f));
        assertNull(world.waterBodyAt(50f, 50f));
    }

    @Test
    void riverWidthAndSurfaceInterpolateAlongTheCentreline() {
        GeneratedWorld world = GeneratedWorld.of(1L, List.of(river(false)));
        WaterSample s = new WaterSample();

        world.sampleWater(5f, 0f, s);
        assertEquals(4.5f, s.surfaceY, EPS);
        assertEquals(2f, s.halfWidth, EPS);
        assertEquals(1f, s.flowX, EPS);
        assertTrue(s.flowSpeed > 0f);

        world.sampleWater(15f, 1f, s);
        assertEquals(3.5f, s.surfaceY, EPS);
        assertEquals(3f, s.halfWidth, EPS);

        assertNotNull(world.waterBodyAt(15f, 2.9f));
        assertNull(world.waterBodyAt(15f, 3.1f));
        assertNull(world.waterBodyAt(-3f, 0f));
    }

    @Test
    void surfaceAndDepthQueries() {
        GeneratedWorld world = GeneratedWorld.of(1L, List.of(lake(0f, 0f, 10f, 5f, false)));

        assertEquals(5f, world.surfaceHeight(0f, 0f), EPS);
        assertTrue(Float.isNaN(world.surfaceHeight(40f, 0f)));
        assertEquals(2f, world.depthAt(0f, 0f), EPS);
        assertEquals(0f, world.depthAt(40f, 0f));
        assertEquals(0f, world.depthAt(10f, 0f), EPS);
    }

    @Test
    void frozenAndOpenWaterDifferInWalkingHeightAndDetection() {
        GeneratedWorld open = GeneratedWorld.of(1L, List.of(lake(0f, 0f, 10f, 5f, false)));
        GeneratedWorld ice = GeneratedWorld.of(1L, List.of(lake(0f, 0f, 10f, 5f, true)));

        assertEquals(open.terrainHeight(0f, 0f), open.groundHeight(0f, 0f));
        assertEquals(5f, ice.groundHeight(0f, 0f), EPS);
        assertEquals(open.terrainHeight(3f, 3f), ice.terrainHeight(3f, 3f), "freezing never moves terrain");

        WaterBody frozen = ice.waterBodyAt(0f, 0f);
        assertTrue(frozen.isFrozen());
        assertTrue(frozen.isIceSafe());
        assertEquals(0.3f, frozen.iceThickness(), EPS);
        assertFalse(open.waterBodyAt(0f, 0f).isFrozen());
        assertEquals(0f, open.waterBodyAt(0f, 0f).iceThickness());
        assertFalse(open.waterBodyAt(0f, 0f).isIceSafe());
    }

    @Test
    void generationProducesBothFrozenAndOpenWaterAcrossSeeds() {
        boolean frozen = false;
        boolean open = false;
        for (long seed : SEEDS) {
            for (WaterBody b : GeneratedWorld.generate(seed).waterBodies()) {
                frozen |= b.isFrozen();
                open |= !b.isFrozen();
            }
        }
        assertTrue(frozen && open);
    }

    @Test
    void detectorReportsEnteringLeavingAndIce() {
        GeneratedWorld world = GeneratedWorld.of(1L, List.of(lake(0f, 0f, 10f, 5f, false),
                lake(100f, 0f, 10f, 5f, true)));
        WaterDetector detector = new WaterDetector(world);

        assertEquals(WaterContact.DRY, detector.update(40f, 5f, 0f));
        assertFalse(detector.enteredWater());

        assertEquals(WaterContact.IN_WATER, detector.update(0f, 3f, 0f));
        assertTrue(detector.enteredWater());
        assertTrue(detector.depth() > 1f);
        assertNotNull(detector.body());

        detector.update(0.5f, 3f, 0f);
        assertFalse(detector.enteredWater());

        assertEquals(WaterContact.DRY, detector.update(40f, 5f, 0f));
        assertTrue(detector.leftWater());
        assertNull(detector.body());

        assertEquals(WaterContact.ON_ICE, detector.update(100f, 5f, 0f));
        assertFalse(detector.enteredWater());
        assertEquals(WaterContact.DRY, detector.update(100f, 12f, 0f), "above the surface is not in contact");
    }

    @Test
    void shallowShorelineCountsAsDry() {
        GeneratedWorld world = GeneratedWorld.of(1L, List.of(lake(0f, 0f, 10f, 5f, false)));
        WaterDetector detector = new WaterDetector(world);

        assertEquals(WaterContact.DRY, detector.update(9.99f, 5f, 0f));
    }

    @Test
    void bodyRejectsBadData() {
        assertThrows(IllegalArgumentException.class, () -> new WaterBody(WaterType.RIVER, new float[] {0f},
                new float[] {0f}, new float[] {1f}, new float[] {1f}, new float[] {1f}, false, 0f));
        assertThrows(IllegalArgumentException.class, () -> new WaterBody(WaterType.LAKE, new float[] {0f},
                new float[] {0f}, new float[] {0f}, new float[] {1f}, new float[] {1f}, false, 0f));
        assertThrows(IllegalArgumentException.class, () -> new WaterBody(WaterType.LAKE, new float[] {0f},
                new float[] {0f, 1f}, new float[] {1f}, new float[] {1f}, new float[] {1f}, false, 0f));
    }
}
