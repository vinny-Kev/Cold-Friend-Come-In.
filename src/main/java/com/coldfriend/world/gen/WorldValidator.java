package com.coldfriend.world.gen;

import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.LocationKind;
import com.coldfriend.world.model.River;
import com.coldfriend.world.model.River.RiverPoint;
import com.coldfriend.world.model.VillageBuilding;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldLocation;
import com.coldfriend.world.query.GridNavigation;
import java.util.ArrayList;
import java.util.List;

/** Checks a generated world against the rules every world must satisfy. */
public final class WorldValidator {

    /** Greatest allowed rise of the water surface along a river, to allow for rounding. */
    private static final float LEVEL_TOLERANCE = 0.001f;
    private static final float MAX_SEGMENT_FACTOR = 3f;
    private static final float MIN_RIVER_LENGTH_FACTOR = 0.5f;
    private static final float NAVIGATION_CELL = 4f;
    private static final float BUILDING_GAP = 1f;

    private WorldValidator() {
    }

    /**
     * @param world the generated world
     * @return the problems found; empty if the world is valid
     */
    public static List<String> validate(WorldData world) {
        List<String> problems = new ArrayList<>();
        checkTerrain(world, problems);
        checkRivers(world, problems);
        if (problems.isEmpty()) {
            checkStructures(world, problems);
        }
        if (problems.isEmpty()) {
            checkReachability(world, problems);
        }
        return problems;
    }

    private static void checkTerrain(WorldData world, List<String> problems) {
        int vertices = world.terrain().vertices();
        for (int j = 0; j < vertices; j++) {
            for (int i = 0; i < vertices; i++) {
                if (!Float.isFinite(world.terrain().get(i, j))) {
                    problems.add("Terrain height is not a finite number at " + i + "," + j);
                    return;
                }
            }
        }
    }

    private static void checkRivers(WorldData world, List<String> problems) {
        if (world.rivers().isEmpty()) {
            problems.add("The world has no rivers");
            return;
        }
        float size = world.sizeMeters();
        float totalLength = 0f;
        boolean reachesEdge = false;
        for (River river : world.rivers()) {
            float length = 0f;
            List<RiverPoint> points = river.points();
            for (int k = 1; k < points.size(); k++) {
                RiverPoint a = points.get(k - 1);
                RiverPoint b = points.get(k);
                if (b.level() > a.level() + LEVEL_TOLERANCE) {
                    problems.add("A river rises downstream near " + (int) b.x() + "," + (int) b.z());
                    return;
                }
                float step = (float) Math.hypot(b.x() - a.x(), b.z() - a.z());
                if (step > world.regions().cellSize() * MAX_SEGMENT_FACTOR) {
                    problems.add("A river has a gap near " + (int) a.x() + "," + (int) a.z());
                    return;
                }
                length += step;
            }
            totalLength += length;
            RiverPoint mouth = river.mouth();
            float edge = Math.min(Math.min(mouth.x(), size - mouth.x()), Math.min(mouth.z(), size - mouth.z()));
            reachesEdge |= edge <= 0f;
        }
        if (!reachesEdge) {
            problems.add("No river reaches the edge of the map");
        }
        if (totalLength < size * MIN_RIVER_LENGTH_FACTOR) {
            problems.add("Rivers are too short");
        }
    }

    private static void checkStructures(WorldData world, List<String> problems) {
        List<Footprint> footprints = world.footprints();
        for (Footprint f : footprints) {
            if (world.water().distanceAt(f.x(), f.z()) <= 0f) {
                problems.add("A building stands in water at " + (int) f.x() + "," + (int) f.z());
                return;
            }
        }
        for (int a = 0; a < footprints.size(); a++) {
            for (int b = a + 1; b < footprints.size(); b++) {
                Footprint fa = footprints.get(a);
                Footprint fb = footprints.get(b);
                float distance = (float) Math.hypot(fa.x() - fb.x(), fa.z() - fb.z());
                if (distance < fa.circumradius() + fb.circumradius() + BUILDING_GAP) {
                    problems.add("Two buildings overlap near " + (int) fa.x() + "," + (int) fa.z());
                    return;
                }
            }
        }
        boolean hasWater = world.locations().stream().anyMatch(l -> l.kind() == LocationKind.WATER_ACCESS);
        if (!hasWater) {
            problems.add("No reachable place to collect water was found");
        }
    }

    private static void checkReachability(WorldData world, List<String> problems) {
        GridNavigation navigation = GridNavigation.of(world, NAVIGATION_CELL);
        GridNavigation.Reachable fromCabin = navigation.reachableFrom(world.cabin().doorX(), world.cabin().doorZ());
        if (!fromCabin.contains(world.village().x(), world.village().z())) {
            problems.add("The village square cannot be reached from the cabin");
        }
        for (VillageBuilding building : world.village().buildings()) {
            if (!fromCabin.contains(building.doorX(), building.doorZ())) {
                problems.add("The door of " + building.id() + " cannot be reached from the cabin");
            }
        }
        for (WorldLocation location : world.locations()) {
            if (!fromCabin.contains(location.x(), location.z())) {
                problems.add(location.id() + " cannot be reached from the cabin");
            }
        }
    }
}
