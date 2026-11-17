package com.coldfriend.world.gen;

/**
 * Tunable values for placing scenery, the cabin, the village and paths. Part of {@link WorldGenParameters}, so
 * changing any of them changes the worlds that seeds produce.
 *
 * @param treeCell grid spacing for tree candidates, in metres
 * @param treeMaxSlope steepest ground a tree grows on
 * @param treeWaterMargin how far from the water's edge trees stay, in metres
 * @param clusterWavelength size of tree groves and gaps, in metres
 * @param rockCell grid spacing for rock candidates
 * @param bushCell grid spacing for bush candidates
 * @param logCell grid spacing for fallen-log candidates
 * @param clutterCell grid spacing for clutter candidates
 * @param siteStep grid spacing when searching for a cabin site, in metres
 * @param cabinHalfWidth half the cabin's width
 * @param cabinHalfDepth half the cabin's depth
 * @param cabinRiverMin nearest target distance from the cabin to a river
 * @param cabinRiverMax farthest target distance from the cabin to a river
 * @param cabinMaxRelief greatest height difference allowed across the cabin site
 * @param cabinMapMargin how far the cabin stays from the map edge
 * @param villageStep grid spacing when searching for a village site
 * @param villageRadius how far the village extends from its square
 * @param villageDistanceMin nearest target distance from cabin to village
 * @param villageDistanceMax farthest target distance from cabin to village
 * @param villageMaxSlope steepest average ground allowed at the village site
 * @param villageMaxRelief greatest height difference allowed across the village site
 * @param villageMapMargin how far the village stays from the map edge
 * @param homesMin fewest homes in a village
 * @param homesMax most homes in a village
 * @param flattenMargin width of the blend between a levelled building site and the ground
 * @param pathGrid grid spacing used to route paths, in metres
 * @param fordDepth water depth left where a path crosses a river
 */
public record PlacementParameters(
        float treeCell,
        float treeMaxSlope,
        float treeWaterMargin,
        float clusterWavelength,
        float rockCell,
        float bushCell,
        float logCell,
        float clutterCell,
        float siteStep,
        float cabinHalfWidth,
        float cabinHalfDepth,
        float cabinRiverMin,
        float cabinRiverMax,
        float cabinMaxRelief,
        float cabinMapMargin,
        float villageStep,
        float villageRadius,
        float villageDistanceMin,
        float villageDistanceMax,
        float villageMaxSlope,
        float villageMaxRelief,
        float villageMapMargin,
        int homesMin,
        int homesMax,
        float flattenMargin,
        float pathGrid,
        float fordDepth) {

    /** The standard placement values. */
    public static final PlacementParameters DEFAULT = new PlacementParameters(
            6f, 0.75f, 2.5f, 45f,
            12f, 8f, 24f, 10f,
            16f, 3.6f, 3.0f, 60f, 450f, 1.5f, 150f,
            24f, 45f, 350f, 650f, 0.2f, 6f, 120f,
            5, 8,
            5f, 4f, 0.4f);
}
