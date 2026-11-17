package com.coldfriend.player;

import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;

/** Moves a player through the world: walking, running, wading, sliding along obstacles, following the ground. */
public final class PlayerMovement {

    /** Radius of a person's body, in metres. */
    public static final float BODY_RADIUS = 0.4f;
    private static final float WALK_SPEED = 3.4f;
    private static final float SPRINT_SPEED = 5.6f;
    private static final float WADE_SPEED_FACTOR = 0.55f;
    /** Water deeper than this, in metres, slows the player and counts as wading. */
    public static final float WADE_DEPTH = 0.25f;
    private static final float MAX_PITCH = 1.45f;
    /**
     * Longest time one step may cover, in seconds. A slow frame (a window drag, a stall) must not move the player
     * far enough in one step to pass through a wall; movement only checks where the player ends up.
     */
    public static final float MAX_STEP_SECONDS = 0.05f;

    private final WorldData world;
    private final WorldCollision collision;

    /**
     * @param world the generated world
     * @param collision obstacle and walkability rules for the world
     */
    public PlayerMovement(WorldData world, WorldCollision collision) {
        this.world = world;
        this.collision = collision;
    }

    /**
     * Turns the player's head by the mouse movement of one frame. Look is applied every frame, not per simulation
     * step, so the camera answers the mouse at once; it is a change of angle, not a rate, so it does not depend on
     * the frame rate.
     *
     * @param player the player's state, changed in place
     * @param input what the player asked for
     */
    public void look(PlayerState player, PlayerInput input) {
        // Turning right means a smaller heading, because heading 0 faces +z and positive turns toward +x.
        float yaw = player.yaw() - input.turn();
        float pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, player.pitch() + input.look()));
        player.setLook(yaw, pitch);
    }

    /**
     * Moves one player for one simulation step. Remembers where the step started so drawing can blend between
     * steps.
     *
     * @param player the player's state, changed in place
     * @param input what the player asked for; only the movement keys are used here
     * @param dt the step length in seconds; capped at {@link #MAX_STEP_SECONDS}
     */
    public void move(PlayerState player, PlayerInput input, float dt) {
        player.beginStep();
        float seconds = Math.min(dt, MAX_STEP_SECONDS);
        float yaw = player.yaw();
        float sin = (float) Math.sin(yaw);
        float cos = (float) Math.cos(yaw);
        float wantX = input.forward() * sin - input.strafe() * cos;
        float wantZ = input.forward() * cos + input.strafe() * sin;
        float length = (float) Math.sqrt(wantX * wantX + wantZ * wantZ);
        float x = player.x();
        float z = player.z();
        if (length > 0f) {
            float speed = input.sprint() ? SPRINT_SPEED : WALK_SPEED;
            if (world.water().depthAt(world.terrain(), x, z) > WADE_DEPTH) {
                speed *= WADE_SPEED_FACTOR;
            }
            float scale = Math.min(1f, 1f / length) * speed * seconds;
            float[] moved = collision.move(x, z, wantX * scale, wantZ * scale, BODY_RADIUS);
            x = moved[0];
            z = moved[1];
        }
        player.setPosition(x, world.terrain().heightAt(x, z), z);
    }
}
