package com.coldfriend.creature;

import com.coldfriend.world.model.Village;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldPath.PathPoint;
import com.coldfriend.world.query.TerrainRules;
import com.coldfriend.world.query.WorldCollision;
import com.coldfriend.player.PlayerMovement;
import java.util.List;

/**
 * Where the creature may go while it is not hunting: away from the cabin and the village, on ground it can walk on.
 * Targets are kept further out than the route, so a route may pass closer in but never through the middle.
 */
final class Territory {

    private final WorldData world;
    private final WorldCollision collision;
    private final float cabinX;
    private final float cabinZ;
    private final Village village;

    Territory(WorldData world, WorldCollision collision) {
        this.world = world;
        this.collision = collision;
        this.cabinX = world.cabin().footprint().x();
        this.cabinZ = world.cabin().footprint().z();
        this.village = world.village();
    }

    /** @return true if a creature can stand here: dry, walkable, clear of scenery and buildings */
    boolean standable(float x, float z) {
        WaterMap water = world.water();
        return TerrainRules.walkable(world.terrain(), water, x, z)
                && water.depthAt(world.terrain(), x, z) <= 0.05f
                && collision.canStandAt(x, z, PlayerMovement.BODY_RADIUS);
    }

    /** @return true if a spot is far enough from the cabin and the village to be a wander target */
    boolean targetAllowed(float x, float z) {
        return cabinDistance(x, z) >= CreatureParameters.CABIN_KEEP_OUT_TARGET
                && villageDistance(x, z) >= village.radius() + CreatureParameters.VILLAGE_KEEP_OUT_TARGET;
    }

    /** @return true if every point of a route keeps the creature away from the cabin and the village */
    boolean routeAllowed(List<PathPoint> route) {
        for (PathPoint point : route) {
            if (cabinDistance(point.x(), point.z()) < CreatureParameters.CABIN_KEEP_OUT_ROUTE
                    || villageDistance(point.x(), point.z()) < village.radius() + CreatureParameters.VILLAGE_KEEP_OUT_ROUTE) {
                return false;
            }
        }
        return true;
    }

    /** @return true if a spot is a fair place for the creature to start: far from the cabin, outside the village */
    boolean spawnAllowed(float x, float z) {
        return cabinDistance(x, z) >= CreatureParameters.SPAWN_MIN_DISTANCE
                && villageDistance(x, z) >= village.radius() + CreatureParameters.VILLAGE_KEEP_OUT_TARGET;
    }

    private float cabinDistance(float x, float z) {
        return distance(x, z, cabinX, cabinZ);
    }

    private float villageDistance(float x, float z) {
        return distance(x, z, village.x(), village.z());
    }

    private static float distance(float ax, float az, float bx, float bz) {
        float dx = ax - bx;
        float dz = az - bz;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}
