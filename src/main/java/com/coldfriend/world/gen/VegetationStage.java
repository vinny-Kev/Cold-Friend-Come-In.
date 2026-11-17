package com.coldfriend.world.gen;

import com.coldfriend.assets.PropKind;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.Placement;
import com.coldfriend.world.model.Region;
import com.coldfriend.world.model.RegionMap;
import com.coldfriend.world.model.WaterMap;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Places trees, rocks, bushes, fallen logs and clutter. Each kind uses its own random stream. Candidates sit on
 * a jittered grid, so there is no visible grid, and are accepted according to forest density, groves from a
 * cluster noise, slope, and distance from water. Trees go first and later kinds keep clear of what is there.
 */
final class VegetationStage {

    private static final float OPEN_WATER_MARGIN = 0.5f;
    private static final float SLOPE_PROBE = 2f;
    private static final int CLUSTER_OCTAVES = 2;
    private static final float CLUSTER_BASE = 0.35f;
    private static final float CLUSTER_GAIN = 0.9f;
    private static final float CLUSTER_LOW = -0.35f;
    private static final float CLUSTER_HIGH = 0.35f;
    private static final float TWO_PI = (float) (2.0 * StrictMath.PI);
    private static final int VARIANT_RANGE = 1000;
    private static final float ROCK_BASE = 0.10f;
    private static final float ROCK_SLOPE_GAIN = 0.7f;
    private static final float ROCK_SLOPE_START = 0.15f;
    private static final float ROCK_BANK_BONUS = 0.25f;
    private static final float ROCK_BANK_DISTANCE = 10f;
    private static final float ROCK_MOUNTAIN_BONUS = 0.2f;
    private static final float ROCK_MAX_CHANCE = 0.9f;
    private static final float BUSH_BASE = 0.1f;
    private static final float BUSH_DENSITY_GAIN = 0.35f;
    private static final float BUSH_VALLEY_BONUS = 0.25f;
    private static final float BUSH_MAX_SLOPE = 0.6f;
    private static final float LOG_BASE = 0.1f;
    private static final float LOG_DENSITY_GAIN = 0.4f;
    private static final float LOG_MAX_SLOPE = 0.4f;
    private static final float CLUTTER_CHANCE = 0.35f;
    private static final float CLUTTER_MAX_SLOPE = 0.8f;

    private final HeightField terrain;
    private final WaterMap water;
    private final RegionMap regions;
    private final WorldGenParameters params;
    private final PlacementParameters place;
    private final RandomStreams streams;
    private final PointIndex index;
    private final List<Placement> out = new ArrayList<>();

    private VegetationStage(HeightField terrain, WaterMap water, RegionMap regions, WorldGenParameters params,
            RandomStreams streams) {
        this.terrain = terrain;
        this.water = water;
        this.regions = regions;
        this.params = params;
        this.place = params.placement();
        this.streams = streams;
        this.index = new PointIndex(terrain.sizeMeters(), 4f);
    }

    static List<Placement> generate(HeightField terrain, WaterMap water, RegionMap regions,
            WorldGenParameters params, RandomStreams streams) {
        VegetationStage stage = new VegetationStage(terrain, water, regions, params, streams);
        stage.trees();
        stage.rocks();
        stage.bushes();
        stage.logs();
        stage.clutter();
        return stage.out;
    }

    private void trees() {
        Noise2D cluster = new Noise2D(streams.seedFor("vegetation.clusters"));
        SplittableRandom random = streams.random("vegetation.trees");
        float cell = place.treeCell();
        int n = (int) (params.worldSizeMeters() / cell);
        for (int cj = 0; cj < n; cj++) {
            for (int ci = 0; ci < n; ci++) {
                // Always draw the same numbers per cell, so one cell's outcome never shifts another's.
                float x = (ci + (float) random.nextDouble()) * cell;
                float z = (cj + (float) random.nextDouble()) * cell;
                double chance = random.nextDouble();
                double kindRoll = random.nextDouble();
                float yaw = (float) random.nextDouble() * TWO_PI;
                float scaleRoll = (float) random.nextDouble();
                int variant = random.nextInt(VARIANT_RANGE);

                float grove = CLUSTER_BASE + CLUSTER_GAIN * Maths.smoothstep(CLUSTER_LOW, CLUSTER_HIGH,
                        cluster.fbm(x / place.clusterWavelength(), z / place.clusterWavelength(), CLUSTER_OCTAVES));
                if (chance >= regions.forestDensityAt(x, z) * grove) {
                    continue;
                }
                if (terrain.slopeAt(x, z, SLOPE_PROBE) > place.treeMaxSlope()
                        || water.distanceAt(x, z) < place.treeWaterMargin()) {
                    continue;
                }
                PropKind kind = treeKind(regions.regionAt(x, z), kindRoll);
                float scale = Maths.lerp(0.8f, 1.5f, scaleRoll);
                tryAdd(kind, x, z, yaw, scale, variant);
            }
        }
    }

    private void rocks() {
        SplittableRandom random = streams.random("vegetation.rocks");
        scan(random, place.rockCell(), (x, z, roll) -> {
            float slope = terrain.slopeAt(x, z, SLOPE_PROBE);
            float chance = ROCK_BASE + ROCK_SLOPE_GAIN * Math.max(0f, slope - ROCK_SLOPE_START);
            if (water.distanceAt(x, z) < ROCK_BANK_DISTANCE) {
                chance += ROCK_BANK_BONUS;
            }
            if (regions.regionAt(x, z) == Region.MOUNTAIN_FOREST) {
                chance += ROCK_MOUNTAIN_BONUS;
            }
            return roll < Math.min(ROCK_MAX_CHANCE, chance) && water.distanceAt(x, z) >= OPEN_WATER_MARGIN;
        }, PropKind.ROCK, 0.6f, 2.4f, true);
    }

    private void bushes() {
        SplittableRandom random = streams.random("vegetation.bushes");
        scan(random, place.bushCell(), (x, z, roll) -> {
            float chance = BUSH_BASE + BUSH_DENSITY_GAIN * regions.forestDensityAt(x, z);
            if (regions.regionAt(x, z) == Region.RIVER_VALLEY) {
                chance += BUSH_VALLEY_BONUS;
            }
            return roll < chance && terrain.slopeAt(x, z, SLOPE_PROBE) <= BUSH_MAX_SLOPE
                    && water.distanceAt(x, z) >= OPEN_WATER_MARGIN;
        }, PropKind.BUSH, 0.7f, 1.4f, false);
    }

    private void logs() {
        SplittableRandom random = streams.random("vegetation.logs");
        scan(random, place.logCell(), (x, z, roll) ->
                roll < LOG_BASE + LOG_DENSITY_GAIN * regions.forestDensityAt(x, z)
                        && terrain.slopeAt(x, z, SLOPE_PROBE) <= LOG_MAX_SLOPE
                        && water.distanceAt(x, z) >= place.treeWaterMargin(),
                PropKind.FALLEN_LOG, 0.8f, 1.3f, false);
    }

    private void clutter() {
        SplittableRandom random = streams.random("vegetation.clutter");
        scan(random, place.clutterCell(), (x, z, roll) ->
                roll < CLUTTER_CHANCE && terrain.slopeAt(x, z, SLOPE_PROBE) <= CLUTTER_MAX_SLOPE
                        && water.distanceAt(x, z) >= OPEN_WATER_MARGIN,
                PropKind.CLUTTER, 0.7f, 1.5f, false);
    }

    /** Decides whether a candidate at a position is wanted. */
    private interface Rule {
        boolean accept(float x, float z, double roll);
    }

    private void scan(SplittableRandom random, float cell, Rule rule, PropKind kind, float minScale,
            float maxScale, boolean skewSmall) {
        int n = (int) (params.worldSizeMeters() / cell);
        for (int cj = 0; cj < n; cj++) {
            for (int ci = 0; ci < n; ci++) {
                float x = (ci + (float) random.nextDouble()) * cell;
                float z = (cj + (float) random.nextDouble()) * cell;
                double roll = random.nextDouble();
                float yaw = (float) random.nextDouble() * TWO_PI;
                float scaleRoll = (float) random.nextDouble();
                int variant = random.nextInt(VARIANT_RANGE);
                if (!rule.accept(x, z, roll)) {
                    continue;
                }
                // Skewing makes most rocks small and a few large.
                float t = skewSmall ? scaleRoll * scaleRoll : scaleRoll;
                tryAdd(kind, x, z, yaw, Maths.lerp(minScale, maxScale, t), variant);
            }
        }
    }

    private void tryAdd(PropKind kind, float x, float z, float yaw, float scale, int variant) {
        float radius = kind.spacingRadius() * scale;
        if (index.overlaps(x, z, radius)) {
            return;
        }
        index.add(x, z, radius);
        out.add(new Placement(kind, x, terrain.heightAt(x, z), z, yaw, scale, variant));
    }

    private static PropKind treeKind(Region region, double roll) {
        // Cumulative chances of conifer, then bare tree; the rest are dead trees.
        double conifer;
        double bare;
        switch (region) {
            case CONIFER_FOREST -> {
                conifer = 0.85;
                bare = 0.92;
            }
            case OPEN_WOODLAND -> {
                conifer = 0.5;
                bare = 0.8;
            }
            case MOUNTAIN_FOREST -> {
                conifer = 0.75;
                bare = 0.9;
            }
            case RIVER_VALLEY -> {
                conifer = 0.4;
                bare = 0.8;
            }
            case CLEARING -> {
                conifer = 0.3;
                bare = 0.6;
            }
            default -> throw new IllegalStateException("Unknown region " + region);
        }
        if (roll < conifer) {
            return PropKind.CONIFER;
        }
        return roll < bare ? PropKind.BARE_TREE : PropKind.DEAD_TREE;
    }
}
