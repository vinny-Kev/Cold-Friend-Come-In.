package com.coldfriend.world.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The generated world: everything the seed decides, with no engine types and no mutable game state.
 * The same seed, generation version and parameters always give an identical {@code WorldData}.
 *
 * @param seed the world seed
 * @param generationVersion the version of the generation algorithms that produced this world
 * @param attempt which generation attempt passed validation; 0 unless an earlier attempt was rejected
 * @param parametersFingerprint hash of the generation parameters, for spotting mismatched settings
 * @param terrain ground height, with river valleys carved and building sites levelled
 * @param rivers river reaches
 * @param water water surface and river distance
 * @param regions environmental regions and forest density
 * @param placements scenery: trees, rocks, bushes, logs and clutter
 * @param cabin the player's cabin
 * @param village the village
 * @param paths worn paths, including the trail from the cabin to the village
 * @param locations named places outside the village, such as where to collect water
 */
public record WorldData(
        long seed,
        int generationVersion,
        int attempt,
        long parametersFingerprint,
        HeightField terrain,
        List<River> rivers,
        WaterMap water,
        RegionMap regions,
        List<Placement> placements,
        Cabin cabin,
        Village village,
        List<WorldPath> paths,
        List<WorldLocation> locations) {

    /** Defensive copies of the lists. */
    public WorldData {
        rivers = List.copyOf(rivers);
        placements = List.copyOf(placements);
        paths = List.copyOf(paths);
        locations = List.copyOf(locations);
    }

    /** @return side length of the square world in metres */
    public float sizeMeters() {
        return terrain.sizeMeters();
    }

    /** @return every building footprint in the world: the cabin and the village buildings */
    public List<Footprint> footprints() {
        List<Footprint> all = new ArrayList<>();
        all.add(cabin.footprint());
        for (VillageBuilding building : village.buildings()) {
            all.add(building.footprint());
        }
        return all;
    }
}
