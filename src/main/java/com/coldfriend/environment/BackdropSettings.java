package com.coldfriend.environment;

/**
 * Tuning values for the menu backdrop. Built from config/game.properties by the caller.
 *
 * @param treeCount number of conifers scattered around the cabin
 * @param snowParticleCount maximum number of falling snow particles alive at once
 * @param cameraDriftSpeed angular speed of the slow camera sway, radians per second
 * @param cameraDriftRadius sideways distance the camera sways, in world units
 */
public record BackdropSettings(
        int treeCount,
        int snowParticleCount,
        float cameraDriftSpeed,
        float cameraDriftRadius) {
}
