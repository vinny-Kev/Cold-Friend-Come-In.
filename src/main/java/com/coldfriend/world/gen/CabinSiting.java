package com.coldfriend.world.gen;

import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.RegionMap;
import com.coldfriend.world.model.WaterMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Finds places for the cabin. It scans the map for flat, dry ground away from the edge, then ranks sites by how
 * close they come to a distance from water and a forest density drawn from the seed. That is what makes one
 * seed's cabin sit near a river in thin woods and another's far from water in deep forest.
 */
final class CabinSiting {

    /** A candidate cabin position with its score; higher is better. */
    record Site(float x, float z, float score) {
    }

    private static final float MIN_RIVER_DISTANCE = 15f;
    private static final float MIN_WATER_EDGE = 8f;
    private static final float MAX_SLOPE = 0.2f;
    private static final float SAMPLE_PADDING = 1f;
    private static final int SAMPLE_COUNT = 8;
    private static final float RIVER_SCORE_SCALE = 120f;
    private static final float DENSITY_SCORE_WEIGHT = 1.2f;
    private static final float RELIEF_SCORE_WEIGHT = 0.3f;
    private static final float JITTER_WEIGHT = 0.15f;
    private static final float TWO_PI = (float) (2.0 * StrictMath.PI);
    private static final int KEEP = 6;

    private CabinSiting() {
    }

    static List<Site> rank(HeightField terrain, WaterMap water, RegionMap regions, RiverProximity proximity,
            WorldGenParameters p, SplittableRandom random, float targetRiverDistance, float targetDensity) {
        PlacementParameters place = p.placement();
        float radius = (float) Math.sqrt(place.cabinHalfWidth() * place.cabinHalfWidth()
                + place.cabinHalfDepth() * place.cabinHalfDepth()) + SAMPLE_PADDING;
        float margin = place.cabinMapMargin();
        float far = terrain.sizeMeters() - margin;
        List<Site> sites = new ArrayList<>();
        for (float z = margin; z <= far; z += place.siteStep()) {
            for (float x = margin; x <= far; x += place.siteStep()) {
                float jitter = (float) random.nextDouble();
                float relief = relief(terrain, x, z, radius);
                if (relief > place.cabinMaxRelief() || terrain.slopeAt(x, z, radius) > MAX_SLOPE
                        || !dry(terrain, water, x, z, radius)) {
                    continue;
                }
                float riverDistance = proximity.edgeDistance(x, z);
                if (riverDistance < MIN_RIVER_DISTANCE) {
                    continue;
                }
                float score = -Math.abs(riverDistance - targetRiverDistance) / RIVER_SCORE_SCALE
                        - Math.abs(regions.forestDensityAt(x, z) - targetDensity) * DENSITY_SCORE_WEIGHT
                        - relief * RELIEF_SCORE_WEIGHT
                        + jitter * JITTER_WEIGHT;
                sites.add(new Site(x, z, score));
            }
        }
        sites.sort(Comparator.comparingDouble(Site::score).reversed());
        return sites.subList(0, Math.min(KEEP, sites.size()));
    }

    private static float relief(HeightField terrain, float x, float z, float radius) {
        float low = terrain.heightAt(x, z);
        float high = low;
        for (int k = 0; k < SAMPLE_COUNT; k++) {
            float angle = TWO_PI * k / SAMPLE_COUNT;
            float h = terrain.heightAt(x + (float) StrictMath.cos(angle) * radius,
                    z + (float) StrictMath.sin(angle) * radius);
            low = Math.min(low, h);
            high = Math.max(high, h);
        }
        return high - low;
    }

    private static boolean dry(HeightField terrain, WaterMap water, float x, float z, float radius) {
        if (water.distanceAt(x, z) < MIN_WATER_EDGE + radius) {
            return false;
        }
        return water.depthAt(terrain, x, z) <= 0f;
    }
}
