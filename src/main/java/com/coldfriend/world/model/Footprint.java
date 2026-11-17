package com.coldfriend.world.model;

/**
 * A rotated rectangle on the ground. Its front faces the direction {@code (sin yaw, cos yaw)} and its right
 * side faces {@code (cos yaw, -sin yaw)}.
 *
 * @param x centre x in metres
 * @param z centre z in metres
 * @param yaw rotation in radians
 * @param halfWidth half the side-to-side size in metres
 * @param halfDepth half the front-to-back size in metres
 */
public record Footprint(float x, float z, float yaw, float halfWidth, float halfDepth) {

    /**
     * @param px world x
     * @param pz world z
     * @param margin extra distance around the rectangle, in metres
     * @return true if the point is inside the rectangle grown by the margin
     */
    public boolean contains(float px, float pz, float margin) {
        float[] local = toLocal(px, pz);
        return Math.abs(local[0]) <= halfWidth + margin && Math.abs(local[1]) <= halfDepth + margin;
    }

    /**
     * @param px world x
     * @param pz world z
     * @return the point's sideways and forward distance from the centre, in the rectangle's own directions
     */
    public float[] toLocal(float px, float pz) {
        float dx = px - x;
        float dz = pz - z;
        float sin = (float) StrictMath.sin(yaw);
        float cos = (float) StrictMath.cos(yaw);
        return new float[] {dx * cos - dz * sin, dx * sin + dz * cos};
    }

    /**
     * @param side metres to the right of the centre
     * @param forward metres in front of the centre
     * @return the world x and z of that point
     */
    public float[] toWorld(float side, float forward) {
        float sin = (float) StrictMath.sin(yaw);
        float cos = (float) StrictMath.cos(yaw);
        return new float[] {x + side * cos + forward * sin, z - side * sin + forward * cos};
    }

    /**
     * @param distance metres in front of the centre, along the front direction
     * @return the world x and z of that point
     */
    public float[] pointInFront(float distance) {
        return toWorld(0f, distance);
    }

    /** @return the distance from the centre to a corner */
    public float circumradius() {
        return (float) Math.sqrt(halfWidth * halfWidth + halfDepth * halfDepth);
    }
}
