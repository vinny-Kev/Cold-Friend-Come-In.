package com.coldfriend.world.gen;

import com.coldfriend.world.model.BuildingKind;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.LocationKind;
import com.coldfriend.world.model.VillageBuilding;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldLocation;
import com.coldfriend.world.model.WorldPath.PathPoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

/** Finds a place for the village, then lays out its store and homes around a central square. */
final class VillageSiting {

    /** A candidate village centre with its score; higher is better. */
    record Site(float x, float z, float score) {
    }

    /** The laid-out village. */
    record Layout(List<VillageBuilding> buildings, List<WorldLocation> locations, float squareHeight) {
    }

    private static final float MIN_CABIN_DISTANCE = 250f;
    private static final float DISTANCE_SLACK = 150f;
    private static final float MIN_RIVER_DISTANCE = 25f;
    private static final float MAX_RIVER_DISTANCE = 200f;
    private static final float TARGET_RIVER_DISTANCE = 70f;
    private static final float RING_FRACTION = 0.8f;
    private static final int RING_SAMPLES = 8;
    private static final float RING_WATER_MARGIN = 6f;
    private static final float DISTANCE_SCORE_SCALE = 100f;
    private static final float RIVER_SCORE_SCALE = 80f;
    private static final float SLOPE_SCORE_WEIGHT = 4f;
    private static final float JITTER_WEIGHT = 0.15f;
    private static final float TWO_PI = (float) (2.0 * StrictMath.PI);
    private static final int KEEP = 10;

    private static final float STORE_HALF_WIDTH = 4.5f;
    private static final float STORE_HALF_DEPTH = 3.5f;
    private static final float STORE_RADIUS = 17f;
    private static final float HOME_HALF_WIDTH = 3.0f;
    private static final float HOME_HALF_DEPTH = 2.6f;
    private static final float HOME_RADIUS = 24f;
    private static final float HOME_RADIUS_JITTER = 4f;
    private static final float HOME_ANGLE_JITTER = 0.2f;
    private static final int HOME_ATTEMPTS = 4;
    private static final float HOME_RADIUS_STEP = 2f;
    private static final int MIN_HOMES_PLACED = 3;
    private static final float BUILDING_MAX_RELIEF = 2.5f;
    private static final float BUILDING_WATER_MARGIN = 10f;
    private static final float TRAIL_CLEARANCE = 7f;
    private static final float BUILDING_GAP = 3f;
    private static final float DOORSTEP_DISTANCE = 1.3f;
    private static final float COUNTER_INSET = 1.2f;

    private VillageSiting() {
    }

    static List<Site> rank(HeightField terrain, WaterMap water, RiverProximity proximity, WorldGenParameters p,
            SplittableRandom random, float cabinX, float cabinZ, float targetDistance) {
        PlacementParameters place = p.placement();
        float margin = place.villageMapMargin();
        float far = terrain.sizeMeters() - margin;
        float ringRadius = place.villageRadius() * RING_FRACTION;
        List<Site> sites = new ArrayList<>();
        for (float z = margin; z <= far; z += place.villageStep()) {
            for (float x = margin; x <= far; x += place.villageStep()) {
                float jitter = (float) random.nextDouble();
                float away = (float) StrictMath.hypot(x - cabinX, z - cabinZ);
                if (away < MIN_CABIN_DISTANCE || away > place.villageDistanceMax() + DISTANCE_SLACK) {
                    continue;
                }
                float slope = terrain.slopeAt(x, z, ringRadius);
                if (slope > place.villageMaxSlope() || !suitable(terrain, water, x, z, ringRadius, place)) {
                    continue;
                }
                float riverDistance = proximity.edgeDistance(x, z);
                if (riverDistance < MIN_RIVER_DISTANCE || riverDistance > MAX_RIVER_DISTANCE) {
                    continue;
                }
                float score = -Math.abs(away - targetDistance) / DISTANCE_SCORE_SCALE
                        - Math.abs(riverDistance - TARGET_RIVER_DISTANCE) / RIVER_SCORE_SCALE
                        - slope * SLOPE_SCORE_WEIGHT
                        + jitter * JITTER_WEIGHT;
                sites.add(new Site(x, z, score));
            }
        }
        sites.sort(Comparator.comparingDouble(Site::score).reversed());
        return sites.subList(0, Math.min(KEEP, sites.size()));
    }

    private static boolean suitable(HeightField terrain, WaterMap water, float x, float z, float ring,
            PlacementParameters place) {
        float low = terrain.heightAt(x, z);
        float high = low;
        for (int k = 0; k < RING_SAMPLES; k++) {
            float angle = TWO_PI * k / RING_SAMPLES;
            float px = x + (float) StrictMath.cos(angle) * ring;
            float pz = z + (float) StrictMath.sin(angle) * ring;
            if (water.distanceAt(px, pz) < RING_WATER_MARGIN) {
                return false;
            }
            float h = terrain.heightAt(px, pz);
            low = Math.min(low, h);
            high = Math.max(high, h);
        }
        return high - low <= place.villageMaxRelief() && water.distanceAt(x, z) >= RING_WATER_MARGIN;
    }

    /**
     * Lays out the store and homes around the square.
     *
     * @param trail the cabin trail, kept clear of buildings
     * @return the layout, or null if too few homes fit at this site
     */
    static Layout layout(HeightField terrain, WaterMap water, WorldGenParameters p, SplittableRandom random,
            Site site, List<PathPoint> trail) {
        PlacementParameters place = p.placement();
        float squareHeight = terrain.heightAt(site.x(), site.z());
        List<VillageBuilding> buildings = new ArrayList<>();
        List<WorldLocation> locations = new ArrayList<>();
        locations.add(new WorldLocation("village-square", LocationKind.VILLAGE_SQUARE, site.x(), site.z()));

        float startAngle = (float) random.nextDouble() * TWO_PI;
        Footprint storeFootprint = facingSquare(site, startAngle, STORE_RADIUS, STORE_HALF_WIDTH, STORE_HALF_DEPTH);
        if (!fits(terrain, water, storeFootprint, buildings, trail)) {
            return null;
        }
        add(terrain, buildings, locations, "village-store", BuildingKind.GENERAL_STORE, storeFootprint);
        float[] counter = storeFootprint.pointInFront(-(STORE_HALF_DEPTH - COUNTER_INSET));
        locations.add(new WorldLocation("village-store-counter", LocationKind.STORE_COUNTER, counter[0], counter[1]));

        int homes = place.homesMin() + random.nextInt(place.homesMax() - place.homesMin() + 1);
        int placed = 0;
        for (int k = 0; k < homes; k++) {
            float angle = startAngle + TWO_PI * (k + 1) / (homes + 1)
                    + (float) (random.nextDouble() * 2.0 - 1.0) * HOME_ANGLE_JITTER;
            float radius = HOME_RADIUS + (float) (random.nextDouble() * 2.0 - 1.0) * HOME_RADIUS_JITTER;
            for (int attempt = 0; attempt < HOME_ATTEMPTS; attempt++) {
                Footprint home = facingSquare(site, angle, radius + attempt * HOME_RADIUS_STEP,
                        HOME_HALF_WIDTH, HOME_HALF_DEPTH);
                if (fits(terrain, water, home, buildings, trail)) {
                    placed++;
                    add(terrain, buildings, locations, "village-home-" + placed, BuildingKind.HOME, home);
                    float[] inside = home.pointInFront(0f);
                    locations.add(new WorldLocation("village-home-" + placed + "-residence",
                            LocationKind.RESIDENCE, inside[0], inside[1]));
                    break;
                }
            }
        }
        return placed >= MIN_HOMES_PLACED ? new Layout(buildings, locations, squareHeight) : null;
    }

    private static Footprint facingSquare(Site site, float angle, float radius, float halfWidth, float halfDepth) {
        float x = site.x() + (float) StrictMath.sin(angle) * radius;
        float z = site.z() + (float) StrictMath.cos(angle) * radius;
        float yaw = (float) StrictMath.atan2(site.x() - x, site.z() - z);
        return new Footprint(x, z, yaw, halfWidth, halfDepth);
    }

    private static boolean fits(HeightField terrain, WaterMap water, Footprint f, List<VillageBuilding> placed,
            List<PathPoint> trail) {
        if (TerrainShaping.relief(terrain, f) > BUILDING_MAX_RELIEF
                || water.distanceAt(f.x(), f.z()) < BUILDING_WATER_MARGIN + f.circumradius()) {
            return false;
        }
        if (Exclusions.distanceToPath(trail, f.x(), f.z()) < TRAIL_CLEARANCE + f.circumradius()) {
            return false;
        }
        for (VillageBuilding other : placed) {
            Footprint o = other.footprint();
            float distance = (float) StrictMath.hypot(o.x() - f.x(), o.z() - f.z());
            if (distance < o.circumradius() + f.circumradius() + BUILDING_GAP) {
                return false;
            }
        }
        return true;
    }

    private static void add(HeightField terrain, List<VillageBuilding> buildings, List<WorldLocation> locations,
            String id, BuildingKind kind, Footprint f) {
        float[] door = f.pointInFront(f.halfDepth() + DOORSTEP_DISTANCE);
        buildings.add(new VillageBuilding(id, kind, f, TerrainShaping.floorHeight(terrain, f), door[0], door[1]));
        locations.add(new WorldLocation(id + "-door", LocationKind.DOORSTEP, door[0], door[1]));
    }
}
