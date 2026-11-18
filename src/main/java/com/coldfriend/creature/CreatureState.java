package com.coldfriend.creature;

import com.coldfriend.entity.EntityId;

/**
 * Where the creature is and which way it faces. Its identity ({@link #id()}) never changes; its appearance is kept
 * apart from it (see the design). Mutable, owned by {@link CreatureSimulation}.
 */
public final class CreatureState {

    private final EntityId id;
    private float x;
    private float z;
    private float heading;
    private CreatureMode mode = CreatureMode.ROAM;

    /**
     * @param id the creature's identity
     * @param x starting world x, in metres
     * @param z starting world z, in metres
     * @param heading starting facing, in radians; 0 is +z
     */
    CreatureState(EntityId id, float x, float z, float heading) {
        this.id = id;
        this.x = x;
        this.z = z;
        this.heading = heading;
    }

    /** @return the creature's identity */
    public EntityId id() {
        return id;
    }

    /** @return world x, in metres */
    public float x() {
        return x;
    }

    /** @return world z, in metres */
    public float z() {
        return z;
    }

    /** @return facing, in radians; 0 is +z, positive turns toward +x */
    public float heading() {
        return heading;
    }

    /** @return what the creature is doing */
    public CreatureMode mode() {
        return mode;
    }

    void setMode(CreatureMode next) {
        mode = next;
    }

    void moveTo(float newX, float newZ) {
        float dx = newX - x;
        float dz = newZ - z;
        if (dx * dx + dz * dz > 1e-8f) {
            heading = (float) Math.atan2(dx, dz);
        }
        x = newX;
        z = newZ;
    }
}
