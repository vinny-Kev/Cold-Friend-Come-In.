package com.coldfriend.creature;

import com.coldfriend.player.PlayerMovement;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldPath.PathPoint;
import com.coldfriend.world.query.PathFinder;
import com.coldfriend.world.query.WorldCollision;
import java.util.List;

/**
 * Walks the creature along a route. A route is searched only when the brain asks for a new one, never every step,
 * and it is reused until it is finished or blocked. A route that stops making progress is reported as blocked, so
 * the brain can ask again.
 */
final class CreatureNavigator {

    private final WorldData world;
    private final WorldCollision collision;
    private PathFinder finder;
    private List<PathPoint> route = List.of();
    private int next;
    private double stalled;
    private boolean blocked;

    CreatureNavigator(WorldData world, WorldCollision collision) {
        this.world = world;
        this.collision = collision;
    }

    /**
     * Searches a route. The first call builds the search grid for the world, which takes a moment.
     *
     * @return the route, or an empty list if no route exists within the search budget
     */
    List<PathPoint> search(float fromX, float fromZ, float toX, float toZ) {
        if (finder == null) {
            finder = new PathFinder(world.terrain(), world.water(), CreatureParameters.PATH_CELL, world.footprints());
        }
        return finder.find(fromX, fromZ, toX, toZ, CreatureParameters.PATH_NODE_BUDGET);
    }

    /** Starts following a route, replacing any current one. */
    void follow(List<PathPoint> newRoute) {
        route = List.copyOf(newRoute);
        next = 0;
        stalled = 0;
        blocked = false;
    }

    /** @return the route being followed; empty if none */
    List<PathPoint> route() {
        return route;
    }

    /** @return true when the route has been finished (or there was none) */
    boolean arrived() {
        return next >= route.size();
    }

    /** @return how many route points are still to be walked; 0 when the route is finished or absent */
    int remaining() {
        return arrived() ? 0 : route.size() - next;
    }

    /** @return true if the route stopped making progress and must be searched again */
    boolean blocked() {
        return blocked;
    }

    /**
     * Moves the creature toward the next point on its route for one step.
     *
     * @param creature the creature to move
     * @param speed speed in metres per second
     * @param dt step length in seconds
     */
    void step(CreatureState creature, float speed, float dt) {
        if (arrived() || blocked) {
            return;
        }
        PathPoint target = route.get(next);
        float dx = target.x() - creature.x();
        float dz = target.z() - creature.z();
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        if (distance <= CreatureParameters.WAYPOINT_REACH) {
            next++;
            stalled = 0;
            return;
        }
        float travel = Math.min(distance, speed * dt);
        float fromX = creature.x();
        float fromZ = creature.z();
        float[] moved = collision.move(fromX, fromZ, dx / distance * travel, dz / distance * travel,
                PlayerMovement.BODY_RADIUS);
        creature.moveTo(moved[0], moved[1]);
        float progress = (float) Math.sqrt((moved[0] - fromX) * (moved[0] - fromX) + (moved[1] - fromZ) * (moved[1] - fromZ));
        if (progress < CreatureParameters.STALL_FRACTION * travel) {
            stalled += dt;
            if (stalled >= CreatureParameters.STALL_SECONDS) {
                blocked = true;
            }
        } else {
            stalled = 0;
        }
    }
}
