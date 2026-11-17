package com.coldfriend.world.gen;

/** Small numeric helpers shared by the generation stages. */
final class Maths {

    private static final float SMOOTH_A = 3f;
    private static final float SMOOTH_B = 2f;
    private static final int KEY_SHIFT = 32;
    private static final int SORT_FLIP = Integer.MAX_VALUE;
    private static final int SIGN_SHIFT = 31;

    private Maths() {
    }

    static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (SMOOTH_A - SMOOTH_B * t);
    }

    /**
     * Packs a float priority and an index into one long that sorts by priority, then by index. Lets a
     * priority queue of longs stand in for a queue of objects.
     */
    static long sortKey(float priority, int index) {
        int bits = Float.floatToIntBits(priority);
        int sortable = bits ^ ((bits >> SIGN_SHIFT) & SORT_FLIP);
        return ((long) sortable << KEY_SHIFT) | index;
    }

    /** @return the distance from a point to the line segment a-b */
    static float segmentDistance(float px, float pz, float ax, float az, float bx, float bz) {
        float sx = bx - ax;
        float sz = bz - az;
        float lengthSquared = sx * sx + sz * sz;
        float t = lengthSquared == 0f ? 0f : clamp(((px - ax) * sx + (pz - az) * sz) / lengthSquared, 0f, 1f);
        float dx = px - (ax + sx * t);
        float dz = pz - (az + sz * t);
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}
