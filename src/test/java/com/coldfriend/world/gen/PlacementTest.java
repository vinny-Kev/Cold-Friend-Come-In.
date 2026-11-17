package com.coldfriend.world.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.assets.PropKind;
import com.coldfriend.world.model.BuildingKind;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.LocationKind;
import com.coldfriend.world.model.Placement;
import com.coldfriend.world.model.VillageBuilding;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldLocation;
import com.coldfriend.world.model.WorldPath.PathPoint;
import com.coldfriend.world.query.GridNavigation;
import com.coldfriend.world.query.TerrainRules;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlacementTest {

    private static final long[] SEEDS = {101L, 202L, 303L, 404L};
    private static final float MIN_CABIN_RIVER_DISTANCE = 15f;
    private static final float MIN_VILLAGE_DISTANCE = 250f;
    private static final float CABIN_FLAT_TOLERANCE = 0.5f;
    private static final float NAVIGATION_CELL = 4f;
    private static final int MIN_PLACEMENTS = 5000;
    private static final float MIN_WATER_SPREAD = 60f;

    @Test
    void cabinIsOnFlatDryGroundAwayFromTheEdgeAndWater() {
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            Footprint f = world.cabin().footprint();
            assertTrue(f.x() > 100f && f.z() > 100f && f.x() < world.sizeMeters() - 100f
                    && f.z() < world.sizeMeters() - 100f, "cabin too near the edge, seed " + seed);
            assertTrue(world.water().distanceAt(f.x(), f.z()) >= MIN_CABIN_RIVER_DISTANCE,
                    "cabin too near water, seed " + seed);
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    float[] corner = f.toWorld(sx * f.halfWidth(), sz * f.halfDepth());
                    assertEquals(world.cabin().floorY(), world.terrain().heightAt(corner[0], corner[1]),
                            CABIN_FLAT_TOLERANCE, "cabin ground not level, seed " + seed);
                }
            }
            assertTrue(TerrainRules.walkable(world.terrain(), world.water(), world.cabin().doorX(),
                    world.cabin().doorZ()), "cabin door is not on walkable ground, seed " + seed);
        }
    }

    @Test
    void villageHasAStoreHomesAndNamedPlacesAndLiesAwayFromTheCabin() {
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            long stores = world.village().buildings().stream()
                    .filter(b -> b.kind() == BuildingKind.GENERAL_STORE).count();
            long homes = world.village().buildings().stream().filter(b -> b.kind() == BuildingKind.HOME).count();
            assertEquals(1, stores, "seed " + seed);
            assertTrue(homes >= 3, "too few homes, seed " + seed);
            assertTrue(hasLocation(world, LocationKind.VILLAGE_SQUARE), "no square, seed " + seed);
            assertTrue(hasLocation(world, LocationKind.STORE_COUNTER), "no store counter, seed " + seed);
            double away = Math.hypot(world.village().x() - world.cabin().footprint().x(),
                    world.village().z() - world.cabin().footprint().z());
            assertTrue(away >= MIN_VILLAGE_DISTANCE, "village too close to the cabin, seed " + seed);
            for (VillageBuilding building : world.village().buildings()) {
                assertTrue(world.water().distanceAt(building.footprint().x(), building.footprint().z()) > 0f,
                        building.id() + " is in water, seed " + seed);
            }
        }
    }

    @Test
    void cabinVillageDoorstepsAndWaterAreAllReachableOnFoot() {
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            GridNavigation.Reachable fromCabin = GridNavigation.of(world, NAVIGATION_CELL)
                    .reachableFrom(world.cabin().doorX(), world.cabin().doorZ());
            assertTrue(fromCabin.contains(world.village().x(), world.village().z()), "square, seed " + seed);
            for (VillageBuilding building : world.village().buildings()) {
                assertTrue(fromCabin.contains(building.doorX(), building.doorZ()), building.id() + ", seed " + seed);
            }
            assertTrue(world.locations().stream().anyMatch(l -> l.kind() == LocationKind.WATER_ACCESS),
                    "no water access, seed " + seed);
            for (WorldLocation location : world.locations()) {
                assertTrue(fromCabin.contains(location.x(), location.z()), location.id() + ", seed " + seed);
            }
        }
    }

    @Test
    void trailRunsFromTheCabinDoorToTheVillageSquare() {
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            var trail = world.paths().get(0).points();
            PathPoint first = trail.get(0);
            PathPoint last = trail.get(trail.size() - 1);
            assertEquals(world.cabin().doorX(), first.x(), 0.01f);
            assertEquals(world.cabin().doorZ(), first.z(), 0.01f);
            assertEquals(world.village().x(), last.x(), 0.01f);
            assertEquals(world.village().z(), last.z(), 0.01f);
        }
    }

    @Test
    void sceneryStaysOutOfWaterBuildingsAndPathsAndStandsOnTheGround() {
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            assertTrue(world.placements().size() > MIN_PLACEMENTS, "too little scenery, seed " + seed);
            for (Placement p : world.placements()) {
                assertTrue(p.x() >= 0f && p.z() >= 0f && p.x() <= world.sizeMeters() && p.z() <= world.sizeMeters());
                assertEquals(world.terrain().heightAt(p.x(), p.z()), p.y(), 0.001f);
                assertEquals(0f, world.water().depthAt(world.terrain(), p.x(), p.z()), 0.0001f,
                        p.kind() + " stands in water, seed " + seed);
                for (Footprint f : world.footprints()) {
                    assertFalse(f.contains(p.x(), p.z(), 0f), p.kind() + " inside a building, seed " + seed);
                }
                if (p.kind() == PropKind.CONIFER || p.kind() == PropKind.BARE_TREE
                        || p.kind() == PropKind.DEAD_TREE) {
                    assertTrue(world.water().distanceAt(p.x(), p.z()) >= 0.5f, "tree in the water margin");
                }
            }
        }
    }

    @Test
    void sceneryMixesKindsAndBiomesDifferInTreeMix() {
        WorldData world = TestWorlds.standard(SEEDS[0]);
        Set<PropKind> kinds = EnumSet.noneOf(PropKind.class);
        for (Placement p : world.placements()) {
            kinds.add(p.kind());
        }

        assertEquals(EnumSet.allOf(PropKind.class), kinds);
    }

    @Test
    void waterAccessDistanceVariesBetweenSeeds() {
        double nearest = Double.MAX_VALUE;
        double farthest = 0;
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            double best = Double.MAX_VALUE;
            for (WorldLocation location : world.locations()) {
                if (location.kind() == LocationKind.WATER_ACCESS) {
                    best = Math.min(best, Math.hypot(location.x() - world.cabin().doorX(),
                            location.z() - world.cabin().doorZ()));
                }
            }
            nearest = Math.min(nearest, best);
            farthest = Math.max(farthest, best);
        }

        assertTrue(farthest - nearest > MIN_WATER_SPREAD,
                "water is about the same distance from the cabin in every seed");
    }

    @Test
    void movingTreesDoesNotMoveRiversCabinOrVillage() {
        WorldData base = TestWorlds.standard(SEEDS[1]);
        PlacementParameters denser = PlacementParameters.DEFAULT;
        PlacementParameters changed = new PlacementParameters(
                denser.treeCell() * 0.75f, denser.treeMaxSlope(), denser.treeWaterMargin(),
                denser.clusterWavelength() * 2f, denser.rockCell(), denser.bushCell(), denser.logCell(),
                denser.clutterCell(), denser.siteStep(), denser.cabinHalfWidth(), denser.cabinHalfDepth(),
                denser.cabinRiverMin(), denser.cabinRiverMax(), denser.cabinMaxRelief(), denser.cabinMapMargin(),
                denser.villageStep(), denser.villageRadius(), denser.villageDistanceMin(),
                denser.villageDistanceMax(), denser.villageMaxSlope(), denser.villageMaxRelief(),
                denser.villageMapMargin(), denser.homesMin(), denser.homesMax(), denser.flattenMargin(),
                denser.pathGrid(), denser.fordDepth());

        WorldData other = WorldGenerator.generate(new WorldSeed(SEEDS[1]),
                WorldGenParameters.DEFAULT.withPlacement(changed));

        assertEquals(base.rivers(), other.rivers());
        assertEquals(base.cabin(), other.cabin());
        assertEquals(base.village(), other.village());
        assertEquals(TestWorlds.terrainHash(base), TestWorlds.terrainHash(other));
        assertTrue(base.placements().size() != other.placements().size(), "tree change should change scenery");
    }

    @Test
    void everyBuildingFacesTheSquareWithItsDoorstepOnTheSquareSide() {
        for (long seed : SEEDS) {
            WorldData world = TestWorlds.standard(seed);
            for (VillageBuilding building : world.village().buildings()) {
                Footprint f = building.footprint();
                double centreToSquare = Math.hypot(f.x() - world.village().x(), f.z() - world.village().z());
                double doorToSquare = Math.hypot(building.doorX() - world.village().x(),
                        building.doorZ() - world.village().z());
                assertTrue(doorToSquare < centreToSquare, building.id() + " door faces away, seed " + seed);
            }
        }
    }

    private static boolean hasLocation(WorldData world, LocationKind kind) {
        return world.village().locations().stream().anyMatch(l -> l.kind() == kind);
    }
}
