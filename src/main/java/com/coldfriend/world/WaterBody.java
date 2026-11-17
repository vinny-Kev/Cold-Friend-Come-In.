package com.coldfriend.world;

/**
 * One body of water as world data: a centreline polyline with a width, surface height and depth at each
 * point. Rivers and streams have many points; lakes and ponds have a single point whose half width is the
 * radius. Containment is a distance test against the polyline, so it never depends on render geometry.
 *
 * <p>Immutable once built. Frozen state and ice thickness are fixed at generation.
 */
public final class WaterBody {

    /** Extra terrain-blend distance beyond the water's edge, as a multiple of the half width. */
    public static final float BANK_FACTOR = 1.5f;
    /** Minimum terrain-blend distance beyond the water's edge. */
    public static final float BANK_MIN = 3f;
    /** Extra bank width per metre of height difference between the water level and the surrounding ground. */
    public static final float BANK_SLOPE_RUN = 1.0f;
    /** Upper limit on the slope-dependent extra bank width. */
    public static final float BANK_EXTRA_MAX = 12f;

    private static final float MIN_FLOW = 0.2f;
    private static final float FLOW_PER_SLOPE = 8f;
    private static final float MAX_FLOW = 3f;
    private static final float MIN_SAFE_ICE = 0.1f;

    private final WaterType type;
    private final float[] xs;
    private final float[] zs;
    private final float[] halfWidths;
    private final float[] surfaces;
    private final float[] depths;
    private final boolean frozen;
    private final float iceThickness;
    private final float minX;
    private final float maxX;
    private final float minZ;
    private final float maxZ;

    /**
     * @param type body type
     * @param xs centreline x per point, in flow order
     * @param zs centreline z per point
     * @param halfWidths half width (radius for single-point bodies) per point, all positive
     * @param surfaces water surface height per point
     * @param depths water depth at the centreline per point, all positive
     * @param frozen whether the surface is ice
     * @param iceThickness ice thickness in metres; ignored (stored as 0) when not frozen
     */
    public WaterBody(WaterType type, float[] xs, float[] zs, float[] halfWidths, float[] surfaces,
            float[] depths, boolean frozen, float iceThickness) {
        int n = xs.length;
        if (n == 0 || zs.length != n || halfWidths.length != n || surfaces.length != n || depths.length != n) {
            throw new IllegalArgumentException("water body arrays must be non-empty and equal in length");
        }
        if (type.isFlowing() && n < 2) {
            throw new IllegalArgumentException("a flowing body needs at least two points");
        }
        this.type = type;
        this.xs = xs.clone();
        this.zs = zs.clone();
        this.halfWidths = halfWidths.clone();
        this.surfaces = surfaces.clone();
        this.depths = depths.clone();
        this.frozen = frozen;
        this.iceThickness = frozen ? iceThickness : 0f;
        float lx = Float.POSITIVE_INFINITY;
        float hx = Float.NEGATIVE_INFINITY;
        float lz = Float.POSITIVE_INFINITY;
        float hz = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            if (halfWidths[i] <= 0f || depths[i] <= 0f) {
                throw new IllegalArgumentException("half width and depth must be positive");
            }
            float reach = halfWidths[i] + bankWidth(halfWidths[i]) + BANK_EXTRA_MAX;
            lx = Math.min(lx, xs[i] - reach);
            hx = Math.max(hx, xs[i] + reach);
            lz = Math.min(lz, zs[i] - reach);
            hz = Math.max(hz, zs[i] + reach);
        }
        minX = lx;
        maxX = hx;
        minZ = lz;
        maxZ = hz;
    }

    /** @return the body type */
    public WaterType type() {
        return type;
    }

    /** @return true if the surface is ice */
    public boolean isFrozen() {
        return frozen;
    }

    /** @return ice thickness in metres, 0 when not frozen */
    public float iceThickness() {
        return iceThickness;
    }

    /** @return true if the ice is thick enough to carry a walking person (rule of thumb: 10 cm) */
    public boolean isIceSafe() {
        return frozen && iceThickness >= MIN_SAFE_ICE;
    }

    /** @return number of centreline points */
    public int pointCount() {
        return xs.length;
    }

    /** @param i point index @return centreline x */
    public float x(int i) {
        return xs[i];
    }

    /** @param i point index @return centreline z */
    public float z(int i) {
        return zs[i];
    }

    /** @param i point index @return half width (radius for single-point bodies) */
    public float halfWidth(int i) {
        return halfWidths[i];
    }

    /** @param i point index @return water surface height */
    public float surface(int i) {
        return surfaces[i];
    }

    /** @param i point index @return water depth at the centreline */
    public float depth(int i) {
        return depths[i];
    }

    /**
     * Approximate flow speed on the segment starting at point {@code i} (the last point reuses the final
     * segment), from the surface slope. Zero for still water.
     *
     * @param i point index
     * @return speed in metres per second
     */
    public float flowSpeed(int i) {
        if (!type.isFlowing()) {
            return 0f;
        }
        int a = Math.min(i, xs.length - 2);
        float len = (float) Math.hypot(xs[a + 1] - xs[a], zs[a + 1] - zs[a]);
        float drop = len == 0f ? 0f : (surfaces[a] - surfaces[a + 1]) / len;
        return Math.max(MIN_FLOW, Math.min(MAX_FLOW, MIN_FLOW + drop * FLOW_PER_SLOPE));
    }

    /** @return the fastest speed {@link #flowSpeed} can return */
    public static float maxFlowSpeed() {
        return MAX_FLOW;
    }

    /** Terrain-blend distance beyond the water's edge on flat ground, for a given half width. */
    static float bankWidth(float halfWidth) {
        return halfWidth * BANK_FACTOR + BANK_MIN;
    }

    /**
     * Terrain-blend distance beyond the water's edge when the surrounding ground differs from the water
     * level by {@code heightDifference}. Steeper surroundings get a wider, gentler bank instead of a cliff.
     */
    static float bankWidth(float halfWidth, float heightDifference) {
        return bankWidth(halfWidth) + Math.min(BANK_EXTRA_MAX, BANK_SLOPE_RUN * Math.abs(heightDifference));
    }

    /** @return true if the point is within this body's bounding box including its banks */
    boolean mayInfluence(float x, float z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * Measures this body against a point and records it in {@code out} if it is closer to (or deeper inside)
     * the water than what {@code out} already holds.
     *
     * @param px query x
     * @param pz query z
     * @param out accumulator
     */
    void sample(float px, float pz, WaterSample out) {
        int n = xs.length;
        if (n == 1) {
            float d = (float) Math.hypot(px - xs[0], pz - zs[0]);
            if (d - halfWidths[0] < out.margin) {
                offer(out, d, halfWidths[0], surfaces[0], depths[0], 0f, 0f, 0f);
            }
            return;
        }
        for (int i = 0; i < n - 1; i++) {
            float sx = xs[i + 1] - xs[i];
            float sz = zs[i + 1] - zs[i];
            float len2 = sx * sx + sz * sz;
            float t = len2 == 0f ? 0f : ((px - xs[i]) * sx + (pz - zs[i]) * sz) / len2;
            t = Math.max(0f, Math.min(1f, t));
            float d = (float) Math.hypot(px - (xs[i] + sx * t), pz - (zs[i] + sz * t));
            float w = halfWidths[i] + (halfWidths[i + 1] - halfWidths[i]) * t;
            if (d - w >= out.margin) {
                continue;
            }
            float len = (float) Math.sqrt(len2);
            float speed = flowSpeed(i);
            float s = surfaces[i] + (surfaces[i + 1] - surfaces[i]) * t;
            float dep = depths[i] + (depths[i + 1] - depths[i]) * t;
            offer(out, d, w, s, dep, len == 0f ? 0f : sx / len, len == 0f ? 0f : sz / len, speed);
        }
    }

    private void offer(WaterSample out, float d, float w, float s, float dep, float fx, float fz, float speed) {
        out.body = this;
        out.margin = d - w;
        out.distance = d;
        out.halfWidth = w;
        out.surfaceY = s;
        out.depth = dep;
        out.flowX = fx;
        out.flowZ = fz;
        out.flowSpeed = speed;
    }
}
