package com.coldfriend.world.gen;

import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.LocationKind;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldLocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds places to collect water: gentle, dry riverbank ground just beside the water that a person can walk to.
 * Picks the closest to the cabin, a second one well away from it, and the closest to the village.
 */
final class ResourceStage {

    private record Candidate(float x, float z) {
    }

    private static final float SCAN_STEP = 8f;
    private static final float MIN_EDGE_DISTANCE = 1f;
    private static final float MAX_EDGE_DISTANCE = 3f;
    private static final float MAX_BANK_SLOPE = 0.3f;
    private static final float SLOPE_PROBE = 4f;
    private static final float MAP_MARGIN = 10f;
    private static final float SECOND_POINT_SPACING = 80f;
    private static final int MAX_ROUTE_TRIES = 12;

    private ResourceStage() {
    }

    static List<WorldLocation> waterAccess(HeightField terrain, WaterMap water, PathFinder finder, float[] cabinDoor,
            VillageSiting.Site village) {
        List<Candidate> banks = new ArrayList<>();
        float far = terrain.sizeMeters() - MAP_MARGIN;
        for (float z = MAP_MARGIN; z <= far; z += SCAN_STEP) {
            for (float x = MAP_MARGIN; x <= far; x += SCAN_STEP) {
                float edge = water.distanceAt(x, z);
                if (edge >= MIN_EDGE_DISTANCE && edge <= MAX_EDGE_DISTANCE && water.depthAt(terrain, x, z) <= 0f
                        && terrain.slopeAt(x, z, SLOPE_PROBE) <= MAX_BANK_SLOPE) {
                    banks.add(new Candidate(x, z));
                }
            }
        }
        List<WorldLocation> found = new ArrayList<>();
        Candidate nearCabin = firstReachable(banks, finder, cabinDoor[0], cabinDoor[1], null);
        if (nearCabin != null) {
            found.add(new WorldLocation("water-cabin", LocationKind.WATER_ACCESS, nearCabin.x(), nearCabin.z()));
            Candidate second = firstReachable(banks, finder, cabinDoor[0], cabinDoor[1], nearCabin);
            if (second != null) {
                found.add(new WorldLocation("water-cabin-2", LocationKind.WATER_ACCESS, second.x(), second.z()));
            }
        }
        Candidate nearVillage = firstReachable(banks, finder, village.x(), village.z(), null);
        if (nearVillage != null) {
            found.add(new WorldLocation("water-village", LocationKind.WATER_ACCESS, nearVillage.x(),
                    nearVillage.z()));
        }
        return found;
    }

    /** The closest bank to a point that a person can reach from it, optionally far from an earlier choice. */
    private static Candidate firstReachable(List<Candidate> banks, PathFinder finder, float fromX, float fromZ,
            Candidate avoid) {
        List<Candidate> sorted = new ArrayList<>(banks);
        sorted.sort(Comparator.comparingDouble(c -> distance(c.x(), c.z(), fromX, fromZ)));
        int tries = 0;
        for (Candidate c : sorted) {
            if (avoid != null && distance(c.x(), c.z(), avoid.x(), avoid.z()) < SECOND_POINT_SPACING) {
                continue;
            }
            if (!finder.find(fromX, fromZ, c.x(), c.z()).isEmpty()) {
                return c;
            }
            if (++tries >= MAX_ROUTE_TRIES) {
                break;
            }
        }
        return null;
    }

    private static double distance(float ax, float az, float bx, float bz) {
        return StrictMath.hypot(ax - bx, az - bz);
    }
}
