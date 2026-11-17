package com.coldfriend.world.model;

import java.util.List;

/**
 * The village: a gathering square, buildings around it, and named places inside it. It holds no villagers; they
 * will live in it later.
 *
 * @param x square centre x in metres
 * @param z square centre z in metres
 * @param radius how far the village extends from its centre, in metres
 * @param buildings the buildings
 * @param locations named places such as the square, doorsteps and the store counter
 */
public record Village(float x, float z, float radius, List<VillageBuilding> buildings,
        List<WorldLocation> locations) {

    /** Defensive copies so the village cannot change after generation. */
    public Village {
        buildings = List.copyOf(buildings);
        locations = List.copyOf(locations);
    }
}
