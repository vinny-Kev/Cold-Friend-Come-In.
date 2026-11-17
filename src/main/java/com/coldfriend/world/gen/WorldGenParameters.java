package com.coldfriend.world.gen;

/**
 * All tunable world-generation values in one place. Changing any of them changes the worlds that seeds produce,
 * so the world records {@link #fingerprint()} alongside the seed and generation version.
 *
 * @param worldSizeMeters side length of the square world
 * @param cellSize terrain grid spacing
 * @param baseElevation average ground height before relief is added
 * @param tiltMetersMin smallest overall slope across the map, as a height difference
 * @param tiltMetersMax largest overall slope across the map
 * @param hillAmplitude height of rolling hills
 * @param hillWavelength distance between hill crests
 * @param mountainAmplitude height of mountain ridges
 * @param mountainWavelength distance between ridges
 * @param macroWavelength size of the regions where mountains do or do not rise
 * @param mountainBiasMin lowest per-seed shift toward mountains
 * @param mountainBiasMax highest per-seed shift toward mountains
 * @param basinDepth depth of hollows and depressions
 * @param basinWavelength distance between hollows
 * @param domainWarp how far positions are bent, which stops features lining up on straight lines
 * @param warpWavelength size of the bends
 * @param detailAmplitude height of small ground unevenness
 * @param detailWavelength size of small ground unevenness
 * @param hydrologyStride terrain cells per river-routing cell
 * @param riverTargetCellsMin fewest routing cells that should be river, for the driest seeds
 * @param riverTargetCellsMax most routing cells that should be river, for the wettest seeds
 * @param riverHalfWidthMin half width of the smallest stream
 * @param riverHalfWidthScale half width gained per square root of drained area
 * @param riverHalfWidthMax half width of the largest river
 * @param riverBankSlopeMin gentlest bank slope, rise over run
 * @param riverBankSlopeMax steepest bank slope
 * @param riverValleyExtra how far beyond the water the valley is cut
 * @param regionStride terrain cells per region-map cell
 * @param valleyDistance distance from a river that counts as river valley
 * @param mountainElevation share of the height range above which ground is mountain forest
 * @param mountainSlope steepness above which ground is mountain forest
 * @param clearingThreshold noise level above which open clearings form
 * @param biasRange largest per-seed shift in forest and clearing amounts
 * @param placement values for scenery, cabin, village and paths
 */
public record WorldGenParameters(
        float worldSizeMeters,
        float cellSize,
        float baseElevation,
        float tiltMetersMin,
        float tiltMetersMax,
        float hillAmplitude,
        float hillWavelength,
        float mountainAmplitude,
        float mountainWavelength,
        float macroWavelength,
        float mountainBiasMin,
        float mountainBiasMax,
        float basinDepth,
        float basinWavelength,
        float domainWarp,
        float warpWavelength,
        float detailAmplitude,
        float detailWavelength,
        int hydrologyStride,
        int riverTargetCellsMin,
        int riverTargetCellsMax,
        float riverHalfWidthMin,
        float riverHalfWidthScale,
        float riverHalfWidthMax,
        float riverBankSlopeMin,
        float riverBankSlopeMax,
        float riverValleyExtra,
        int regionStride,
        float valleyDistance,
        float mountainElevation,
        float mountainSlope,
        float clearingThreshold,
        float biasRange,
        PlacementParameters placement) {

    /** The standard 1536 m world. */
    public static final WorldGenParameters DEFAULT = new WorldGenParameters(
            1536f, 2f, 40f,
            30f, 65f,
            11f, 170f,
            60f, 300f, 800f,
            -0.25f, 0.2f,
            7f, 220f,
            55f, 260f,
            0.5f, 9f,
            4, 450, 900,
            1.4f, 0.07f, 14f,
            0.25f, 0.9f, 20f,
            4, 14f, 0.6f, 0.5f,
            0.3f, 0.2f,
            PlacementParameters.DEFAULT);

    /** Checks that the grid sizes divide evenly. */
    public WorldGenParameters {
        int cells = Math.round(worldSizeMeters / cellSize);
        if (cells <= 0 || cells % hydrologyStride != 0 || cells % regionStride != 0) {
            throw new IllegalArgumentException("World size must be a whole number of cells divisible by the strides");
        }
    }

    /** @return terrain cells along each side */
    public int cells() {
        return Math.round(worldSizeMeters / cellSize);
    }

    /**
     * @param meters new side length; must give a cell count divisible by both strides
     * @return a copy with that world size
     */
    public WorldGenParameters withWorldSize(float meters) {
        return new WorldGenParameters(meters, cellSize, baseElevation, tiltMetersMin, tiltMetersMax,
                hillAmplitude, hillWavelength, mountainAmplitude, mountainWavelength, macroWavelength,
                mountainBiasMin, mountainBiasMax, basinDepth, basinWavelength, domainWarp, warpWavelength,
                detailAmplitude, detailWavelength, hydrologyStride, riverTargetCellsMin, riverTargetCellsMax,
                riverHalfWidthMin, riverHalfWidthScale, riverHalfWidthMax, riverBankSlopeMin, riverBankSlopeMax,
                riverValleyExtra, regionStride, valleyDistance, mountainElevation, mountainSlope,
                clearingThreshold, biasRange, placement);
    }

    /**
     * @param newClearingThreshold noise level above which clearings form
     * @param newBiasRange largest per-seed shift in forest and clearing amounts
     * @return a copy with only the region tuning changed; terrain and rivers are unaffected
     */
    public WorldGenParameters withRegionTuning(float newClearingThreshold, float newBiasRange) {
        return new WorldGenParameters(worldSizeMeters, cellSize, baseElevation, tiltMetersMin, tiltMetersMax,
                hillAmplitude, hillWavelength, mountainAmplitude, mountainWavelength, macroWavelength,
                mountainBiasMin, mountainBiasMax, basinDepth, basinWavelength, domainWarp, warpWavelength,
                detailAmplitude, detailWavelength, hydrologyStride, riverTargetCellsMin, riverTargetCellsMax,
                riverHalfWidthMin, riverHalfWidthScale, riverHalfWidthMax, riverBankSlopeMin, riverBankSlopeMax,
                riverValleyExtra, regionStride, valleyDistance, mountainElevation, mountainSlope,
                newClearingThreshold, newBiasRange, placement);
    }

    /**
     * @param newPlacement new scenery, cabin, village and path values
     * @return a copy with only the placement values changed; terrain, rivers and regions are unaffected
     */
    public WorldGenParameters withPlacement(PlacementParameters newPlacement) {
        return new WorldGenParameters(worldSizeMeters, cellSize, baseElevation, tiltMetersMin, tiltMetersMax,
                hillAmplitude, hillWavelength, mountainAmplitude, mountainWavelength, macroWavelength,
                mountainBiasMin, mountainBiasMax, basinDepth, basinWavelength, domainWarp, warpWavelength,
                detailAmplitude, detailWavelength, hydrologyStride, riverTargetCellsMin, riverTargetCellsMax,
                riverHalfWidthMin, riverHalfWidthScale, riverHalfWidthMax, riverBankSlopeMin, riverBankSlopeMax,
                riverValleyExtra, regionStride, valleyDistance, mountainElevation, mountainSlope,
                clearingThreshold, biasRange, newPlacement);
    }

    /** @return a hash of every value, used to detect that a world was made with different parameters */
    public long fingerprint() {
        return Mixer.hash(toString());
    }
}
