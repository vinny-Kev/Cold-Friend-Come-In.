package com.coldfriend.world.gen;

import com.coldfriend.world.model.HeightField;

/**
 * Builds the terrain heightfield: an overall tilt so water has somewhere to go, mountain ridges where a
 * large-scale noise allows them, rolling hills, hollows, and small ground unevenness. Positions are bent by
 * a warp noise so features do not line up on straight lines.
 */
final class TerrainStage {

    private static final int WARP_OCTAVES = 3;
    private static final int MACRO_OCTAVES = 3;
    private static final int RIDGE_OCTAVES = 5;
    private static final int HILL_OCTAVES = 5;
    private static final int BASIN_OCTAVES = 2;
    private static final int DETAIL_OCTAVES = 2;
    private static final float WARP_SHIFT_X = 71.3f;
    private static final float WARP_SHIFT_Z = 33.7f;
    private static final float MASK_LOW = -0.1f;
    private static final float MASK_HIGH = 0.55f;
    private static final float BASIN_LOW = 0.25f;
    private static final float BASIN_HIGH = 0.6f;

    private TerrainStage() {
    }

    static HeightField generate(WorldGenParameters p, Geography geography, RandomStreams streams) {
        Noise2D warp = new Noise2D(streams.seedFor(RandomStreams.TERRAIN + ".warp"));
        Noise2D macro = new Noise2D(streams.seedFor(RandomStreams.TERRAIN + ".macro"));
        Noise2D ridges = new Noise2D(streams.seedFor(RandomStreams.TERRAIN + ".ridges"));
        Noise2D hills = new Noise2D(streams.seedFor(RandomStreams.TERRAIN + ".hills"));
        Noise2D basins = new Noise2D(streams.seedFor(RandomStreams.TERRAIN + ".basins"));
        Noise2D detail = new Noise2D(streams.seedFor(RandomStreams.TERRAIN + ".detail"));

        HeightField field = new HeightField(p.cells(), p.cellSize());
        float size = field.sizeMeters();
        for (int j = 0; j <= field.cells(); j++) {
            for (int i = 0; i <= field.cells(); i++) {
                float x = i * p.cellSize();
                float z = j * p.cellSize();
                float wx = x + warp.fbm(x / p.warpWavelength(), z / p.warpWavelength(), WARP_OCTAVES) * p.domainWarp();
                float wz = z + warp.fbm(x / p.warpWavelength() + WARP_SHIFT_X, z / p.warpWavelength() - WARP_SHIFT_Z,
                        WARP_OCTAVES) * p.domainWarp();

                float tilt = -((x / size - 0.5f) * geography.downhillX() + (z / size - 0.5f) * geography.downhillZ())
                        * geography.tiltMeters();
                float mask = Maths.smoothstep(MASK_LOW, MASK_HIGH,
                        macro.fbm(wx / p.macroWavelength(), wz / p.macroWavelength(), MACRO_OCTAVES)
                                + geography.mountainBias());
                float mountains = ridges.ridged(wx / p.mountainWavelength(), wz / p.mountainWavelength(), RIDGE_OCTAVES)
                        * p.mountainAmplitude() * mask;
                float rolling = hills.fbm(wx / p.hillWavelength(), wz / p.hillWavelength(), HILL_OCTAVES)
                        * p.hillAmplitude();
                float hollow = -Maths.smoothstep(BASIN_LOW, BASIN_HIGH,
                        basins.fbm(wx / p.basinWavelength(), wz / p.basinWavelength(), BASIN_OCTAVES))
                        * p.basinDepth();
                float uneven = detail.fbm(x / p.detailWavelength(), z / p.detailWavelength(), DETAIL_OCTAVES)
                        * p.detailAmplitude();
                field.set(i, j, p.baseElevation() + tilt + mountains + rolling + hollow + uneven);
            }
        }
        return field;
    }
}
