package com.coldfriend.world.model;

/**
 * One building in the village.
 *
 * @param id stable identifier
 * @param kind what the building is
 * @param footprint where it stands; its front faces the door
 * @param floorY floor height in metres
 * @param doorX x of the spot just outside the door
 * @param doorZ z of the spot just outside the door
 */
public record VillageBuilding(String id, BuildingKind kind, Footprint footprint, float floorY, float doorX,
        float doorZ) {
}
