package com.coldfriend.world.gen;

/** Small numeric helpers shared by the generation stages. */
final class Maths {

    private static final float SMOOTH_A = 3f;
    private static final float SMOOTH_B = 2f;

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
