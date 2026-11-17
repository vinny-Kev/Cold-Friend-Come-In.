package com.coldfriend.world.gen;

import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.Region;
import com.coldfriend.world.model.RegionMap;
import com.coldfriend.world.model.WaterMap;

/**
 * Assigns each part of the world to an environmental region from its height, steepness, distance to water and
 * two large-scale noises. Also builds a forest density that is blurred across region borders so vegetation
 * thins and thickens gradually.
 */
final class RegionStage {

    private static final float FOREST_WAVELENGTH = 320f;
    private static final float CLEARING_WAVELENGTH = 90f;
    private static final int FOREST_OCTAVES = 3;
    private static final int CLEARING_OCTAVES = 2;
    private static final float CLEARING_MAX_SLOPE = 0.25f;
    private static final int BLUR_PASSES = 2;
    private static final float DENSITY_CONIFER = 0.9f;
    private static final float DENSITY_OPEN = 0.35f;
    private static final float DENSITY_MOUNTAIN = 0.7f;
    private static final float DENSITY_VALLEY = 0.5f;
    private static final float DENSITY_CLEARING = 0.04f;

    private RegionStage() {
    }

    static RegionMap generate(HeightField terrain, WaterMap water, Geography geography, WorldGenParameters p,
            RandomStreams streams) {
        int stride = p.regionStride();
        int side = terrain.cells() / stride + 1;
        float spacing = stride * terrain.cellSize();
        Noise2D forestNoise = new Noise2D(streams.seedFor(RandomStreams.REGIONS + ".forest"));
        Noise2D clearingNoise = new Noise2D(streams.seedFor(RandomStreams.REGIONS + ".clearing"));
        float low = terrain.min();
        float range = Math.max(1f, terrain.max() - low);

        Region[] regions = new Region[side * side];
        float[] density = new float[side * side];
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                float x = i * spacing;
                float z = j * spacing;
                float height = terrain.get(i * stride, j * stride);
                float slope = terrain.slopeAt(x, z, spacing);
                float riverDistance = water.distance(i * stride, j * stride);
                float forest = forestNoise.fbm(x / FOREST_WAVELENGTH, z / FOREST_WAVELENGTH, FOREST_OCTAVES)
                        + geography.forestBias();
                float clearing = clearingNoise.fbm(x / CLEARING_WAVELENGTH, z / CLEARING_WAVELENGTH, CLEARING_OCTAVES)
                        + geography.clearingBias();
                Region region = classify(p, (height - low) / range, slope, riverDistance, forest, clearing);
                regions[j * side + i] = region;
                density[j * side + i] = baseDensity(region);
            }
        }
        for (int pass = 0; pass < BLUR_PASSES; pass++) {
            density = blur(density, side);
        }
        return new RegionMap(side, spacing, regions, density);
    }

    private static Region classify(WorldGenParameters p, float elevation, float slope, float riverDistance,
            float forest, float clearing) {
        if (riverDistance < p.valleyDistance()) {
            return Region.RIVER_VALLEY;
        }
        if (elevation > p.mountainElevation() || slope > p.mountainSlope()) {
            return Region.MOUNTAIN_FOREST;
        }
        if (clearing > p.clearingThreshold() && slope < CLEARING_MAX_SLOPE) {
            return Region.CLEARING;
        }
        return forest > 0f ? Region.CONIFER_FOREST : Region.OPEN_WOODLAND;
    }

    private static float baseDensity(Region region) {
        return switch (region) {
            case CONIFER_FOREST -> DENSITY_CONIFER;
            case OPEN_WOODLAND -> DENSITY_OPEN;
            case MOUNTAIN_FOREST -> DENSITY_MOUNTAIN;
            case RIVER_VALLEY -> DENSITY_VALLEY;
            case CLEARING -> DENSITY_CLEARING;
        };
    }

    private static float[] blur(float[] in, int side) {
        float[] out = new float[in.length];
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                float sum = 0f;
                int count = 0;
                for (int dj = -1; dj <= 1; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        int ni = i + di;
                        int nj = j + dj;
                        if (ni >= 0 && nj >= 0 && ni < side && nj < side) {
                            sum += in[nj * side + ni];
                            count++;
                        }
                    }
                }
                out[j * side + i] = sum / count;
            }
        }
        return out;
    }
}
