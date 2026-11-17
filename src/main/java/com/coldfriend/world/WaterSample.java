package com.coldfriend.world;

/**
 * Reusable result holder for water queries, so per-frame queries allocate nothing. Holds the best (closest
 * to or deepest inside) water body found so far; call {@link #reset()} before a new query.
 */
public final class WaterSample {

    /** Body the sample refers to, or null if no body is near. */
    public WaterBody body;
    /** Horizontal distance to the body's centreline minus its half width; negative inside the water. */
    public float margin = Float.POSITIVE_INFINITY;
    /** Distance from the query point to the nearest point of the centreline. */
    public float distance;
    /** Half width of the body at the nearest centreline point. */
    public float halfWidth;
    /** Water surface height at the nearest centreline point. */
    public float surfaceY;
    /** Water depth at the centreline. */
    public float depth;
    /** Unit flow direction x; zero for still water. */
    public float flowX;
    /** Unit flow direction z; zero for still water. */
    public float flowZ;
    /** Approximate flow speed in metres per second; zero for still water. */
    public float flowSpeed;

    /** Clears the sample for reuse. */
    public void reset() {
        body = null;
        margin = Float.POSITIVE_INFINITY;
    }

    /** @return true if the query point lies within the water's horizontal boundary */
    public boolean isInside() {
        return body != null && margin <= 0f;
    }
}
