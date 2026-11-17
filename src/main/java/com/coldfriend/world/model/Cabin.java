package com.coldfriend.world.model;

/**
 * The player's cabin.
 *
 * @param footprint where the cabin stands; its front faces the door
 * @param floorY floor height in metres
 * @param doorX x of the spot just outside the door
 * @param doorZ z of the spot just outside the door
 */
public record Cabin(Footprint footprint, float floorY, float doorX, float doorZ) {
}
