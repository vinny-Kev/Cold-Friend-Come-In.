package com.coldfriend.creature;

/**
 * A read-only snapshot of the creature, for the debug overlay and tests.
 *
 * @param x world x, in metres
 * @param z world z, in metres
 * @param heading facing, in radians
 * @param mode what it is doing
 * @param observations how many things it remembers
 * @param routePointsLeft points still to walk on its current route; 0 when it has none
 * @param targetX the spot it is walking to, x (meaningless when routePointsLeft is 0)
 * @param targetZ the spot it is walking to, z
 */
public record CreatureStatus(float x, float z, float heading, CreatureMode mode, int observations,
        int routePointsLeft, float targetX, float targetZ) {
}
