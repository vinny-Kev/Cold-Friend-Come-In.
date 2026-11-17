package com.coldfriend.player;

/**
 * Where a player is and which way they face. This is authoritative game state, not a camera: the camera
 * follows it. Mutable, owned by the world simulation.
 */
public final class PlayerState {

    private final PlayerId id;
    private float x;
    private float y;
    private float z;
    private float yaw;
    private float pitch;

    /**
     * @param id the player's identity
     * @param x starting world x in metres
     * @param y starting ground height in metres
     * @param z starting world z in metres
     * @param yaw starting heading in radians; 0 faces +z
     */
    public PlayerState(PlayerId id, float x, float y, float z, float yaw) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
    }

    /** @return the player's identity */
    public PlayerId id() {
        return id;
    }

    /** @return world x of the player's feet, in metres */
    public float x() {
        return x;
    }

    /** @return height of the player's feet, in metres */
    public float y() {
        return y;
    }

    /** @return world z of the player's feet, in metres */
    public float z() {
        return z;
    }

    /** @return heading in radians; 0 faces +z and positive turns toward +x */
    public float yaw() {
        return yaw;
    }

    /** @return look angle in radians; positive looks up */
    public float pitch() {
        return pitch;
    }

    void setPosition(float newX, float newY, float newZ) {
        this.x = newX;
        this.y = newY;
        this.z = newZ;
    }

    void setLook(float newYaw, float newPitch) {
        this.yaw = newYaw;
        this.pitch = newPitch;
    }
}
