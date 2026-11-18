package com.coldfriend.world;

import com.coldfriend.entity.EntityId;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.TerrainRules;
import com.coldfriend.world.query.WorldCollision;

/**
 * The car, once it can be driven. Its body is the same three round obstacles as {@link ParkedCar}, moved along as it
 * goes, so people and the creature bump into it. A move that would put the car into something solid is refused and
 * the car stops.
 */
public final class Vehicle {

    private static final float MAX_FORWARD = 8f;
    private static final float MAX_REVERSE = 3f;
    /** Metres per second of speed gained or lost per second of throttle. */
    private static final float ACCEL = 4f;
    /** Radians per second of turn at full forward speed, at full steer. */
    private static final float TURN_RATE = 1.2f;
    private static final float DRY_DEPTH = 0.05f;

    private final WorldData world;
    private final WorldCollision collision;
    private final long idBase;
    private float x;
    private float z;
    private float yaw;
    private float previousX;
    private float previousZ;
    private float previousYaw;
    private float speed;

    /**
     * @param world the generated world
     * @param collision where the car's body is registered
     * @param start the parked car it starts as
     * @param idBase the first of the three obstacle ids the car uses
     */
    Vehicle(WorldData world, WorldCollision collision, ParkedCar start, long idBase) {
        this.world = world;
        this.collision = collision;
        this.idBase = idBase;
        this.x = start.x();
        this.z = start.z();
        this.yaw = start.yaw();
        this.previousX = x;
        this.previousZ = z;
        this.previousYaw = yaw;
        addBody(x, z, yaw);
    }

    /** @return world x of the car's centre, in metres */
    public float x() {
        return x;
    }

    /** @return world z of the car's centre, in metres */
    public float z() {
        return z;
    }

    /** @return the direction the car points, in radians; 0 is +z */
    public float yaw() {
        return yaw;
    }

    /** @return the speed in metres per second; negative when reversing */
    public float speed() {
        return speed;
    }

    /** @return car x at the start of the last fixed step, for smooth drawing */
    public float previousX() {
        return previousX;
    }

    /** @return car z at the start of the last fixed step, for smooth drawing */
    public float previousZ() {
        return previousZ;
    }

    /** @return car heading at the start of the last fixed step, for smooth drawing */
    public float previousYaw() {
        return previousYaw;
    }

    /**
     * Drives for one step.
     *
     * @param throttle -1 (reverse) to 1 (forward)
     * @param steer -1 (left) to 1 (right), as for a player's turn
     * @param dt step length in seconds
     */
    public void drive(float throttle, float steer, float dt) {
        previousX = x;
        previousZ = z;
        previousYaw = yaw;
        float goal = throttle >= 0f ? throttle * MAX_FORWARD : throttle * MAX_REVERSE;
        speed = approach(speed, goal, ACCEL * dt);
        if (speed == 0f) {
            return;
        }
        // Steering right turns the car right, which lowers its heading (see PlayerMovement). Reversing turns it back.
        float newYaw = yaw - steer * TURN_RATE * (speed / MAX_FORWARD) * dt;
        float newX = x + (float) Math.sin(newYaw) * speed * dt;
        float newZ = z + (float) Math.cos(newYaw) * speed * dt;
        if (!moveTo(newX, newZ, newYaw)) {
            speed = 0f;
        }
    }

    /** Moves the body if it fits there; otherwise leaves it where it is. @return true if it moved */
    private boolean moveTo(float newX, float newZ, float newYaw) {
        removeBody();
        boolean fits = fits(newX, newZ, newYaw);
        if (fits) {
            x = newX;
            z = newZ;
            yaw = newYaw;
        }
        addBody(x, z, yaw);
        return fits;
    }

    private boolean fits(float cx, float cz, float cyaw) {
        WaterMap water = world.water();
        float[] centres = ParkedCar.centres(cx, cz, cyaw);
        for (int i = 0; i < centres.length; i += 2) {
            float px = centres[i];
            float pz = centres[i + 1];
            if (!TerrainRules.walkable(world.terrain(), water, px, pz) || water.depthAt(world.terrain(), px, pz) > DRY_DEPTH
                    || !collision.canStandAt(px, pz, ParkedCar.RADIUS)) {
                return false;
            }
        }
        return true;
    }

    private void addBody(float bx, float bz, float byaw) {
        float[] centres = ParkedCar.centres(bx, bz, byaw);
        for (int i = 0; i < centres.length; i += 2) {
            collision.addObstacle(new EntityId(idBase + i / 2), centres[i], centres[i + 1], ParkedCar.RADIUS);
        }
    }

    private void removeBody() {
        for (int i = 0; i < 3; i++) {
            collision.removeObstacle(new EntityId(idBase + i));
        }
    }

    private static float approach(float current, float goal, float maxStep) {
        if (goal > current) {
            return Math.min(goal, current + maxStep);
        }
        return Math.max(goal, current - maxStep);
    }
}
