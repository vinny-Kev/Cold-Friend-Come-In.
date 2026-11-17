package com.coldfriend.creature;

import com.coldfriend.world.model.WorldPath.PathPoint;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Chooses what the creature does. In Phase B it only wanders: it picks a spot to walk to, follows a route there, and
 * picks another. It decides from its own state and the route it has; it never reads the player (see the information
 * barrier test). Runs once per second of game time, not every step.
 */
final class CreatureBrain {

    private final Territory territory;
    private final CreatureNavigator navigator;
    private final SplittableRandom random;
    private boolean hasTarget;
    private float targetX;
    private float targetZ;
    private double targetMinute;

    CreatureBrain(Territory territory, CreatureNavigator navigator, SplittableRandom random) {
        this.territory = territory;
        this.navigator = navigator;
        this.random = random;
    }

    /**
     * One decision.
     *
     * @param creature the creature being steered
     * @param minute game time, in minutes
     */
    void think(CreatureState creature, double minute) {
        if (hasTarget && minute - targetMinute >= CreatureParameters.WANDER_TIMEOUT_MINUTES) {
            hasTarget = false;
        }
        if (hasTarget && navigator.blocked()) {
            // The route is stuck: search the same target again, once. If that fails, choose a new target.
            if (!routeTo(creature, targetX, targetZ)) {
                hasTarget = false;
            }
        }
        if (!hasTarget || navigator.arrived()) {
            chooseTarget(creature, minute);
        }
    }

    /** Picks a random reachable spot in the wander range that the creature may walk to. */
    private void chooseTarget(CreatureState creature, double minute) {
        for (int attempt = 0; attempt < CreatureParameters.WANDER_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double span = CreatureParameters.WANDER_MAX_DISTANCE - CreatureParameters.WANDER_MIN_DISTANCE;
            double distance = CreatureParameters.WANDER_MIN_DISTANCE + random.nextDouble() * span;
            float x = creature.x() + (float) (Math.sin(angle) * distance);
            float z = creature.z() + (float) (Math.cos(angle) * distance);
            if (territory.standable(x, z) && territory.targetAllowed(x, z) && routeTo(creature, x, z)) {
                targetX = x;
                targetZ = z;
                targetMinute = minute;
                hasTarget = true;
                return;
            }
        }
    }

    /** Searches a route to a spot and follows it if it is safe. @return true if the creature now has a route */
    private boolean routeTo(CreatureState creature, float x, float z) {
        List<PathPoint> route = navigator.search(creature.x(), creature.z(), x, z);
        if (route.isEmpty() || !territory.routeAllowed(route)) {
            return false;
        }
        navigator.follow(route);
        return true;
    }

    /** @return true if the creature has a target it is walking to */
    boolean hasTarget() {
        return hasTarget;
    }
}
