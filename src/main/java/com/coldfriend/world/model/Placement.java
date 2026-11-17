package com.coldfriend.world.model;

import com.coldfriend.assets.PropKind;

/**
 * One piece of scenery in the world.
 *
 * @param kind what it is
 * @param x world x in metres
 * @param y ground height in metres
 * @param z world z in metres
 * @param yaw rotation about the vertical axis, in radians
 * @param scale size multiplier
 * @param variant a number the asset catalogue uses to pick one of several models for this kind
 */
public record Placement(PropKind kind, float x, float y, float z, float yaw, float scale, int variant) {

    /** @return the radius of the solid part, scaled */
    public float colliderRadius() {
        return kind.colliderRadius() * scale;
    }

    /** @return a copy standing at a different ground height */
    public Placement withY(float newY) {
        return new Placement(kind, x, newY, z, yaw, scale, variant);
    }
}
