package com.coldfriend.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Places water bodies on a {@link BaseTerrain}. Rivers and streams start at high ground and walk downhill
 * (with momentum, so they meander); where they get stuck in a hollow they end in a pond. Lakes fill local
 * minima. Geometry and frozen state use separate random streams, so changing how much freezes never moves
 * a river. Output depends only on the seed and {@link #GENERATION_VERSION}.
 */
final class WaterGenerator {

    /** Bump when generation changes the layout for an existing seed. */
    static final int GENERATION_VERSION = 1;

    private static final long FREEZE_SALT = 0x5DEECE66DL;
    private static final int RIVER_COUNT = 2;
    private static final int STREAM_COUNT = 3;
    private static final int LAKE_COUNT = 2;
    private static final int SOURCE_ATTEMPTS = 60;
    private static final float SOURCE_MARGIN = 24f;
    private static final float SOURCE_SPACING = 50f;
    private static final float EDGE_MARGIN = 2f;
    private static final float STEP = 3f;
    private static final float GRADIENT_STEP = 6f;
    private static final float MOMENTUM = 0.7f;
    private static final float CLEARANCE = 0.3f;
    private static final float STUCK_RISE = 2.5f;
    private static final float JOIN_MARGIN = 6f;
    private static final double MAX_TURN = 0.35;
    private static final float SELF_CLEARANCE = 8f;
    private static final int SELF_SKIP = 4;
    private static final float JOIN_EDGE = 0.5f;
    private static final int MAX_STEPS = 120;
    private static final int MIN_POINTS = 8;
    private static final float WIDTH_VARIATION = 0.15f;
    private static final float WIDTH_FREQUENCY = 0.21f;
    private static final float RIVER_WIDTH_START = 2.5f;
    private static final float RIVER_WIDTH_END = 4f;
    private static final float STREAM_WIDTH_START = 1.2f;
    private static final float STREAM_WIDTH_END = 1.6f;
    private static final float RIVER_DEPTH_RATIO = 0.4f;
    private static final float STREAM_DEPTH_RATIO = 0.3f;
    private static final float POND_RADIUS_MIN = 5f;
    private static final float POND_RADIUS_RANGE = 2f;
    private static final float POND_DEPTH = 1.2f;
    private static final float LAKE_GRID = 10f;
    private static final float LAKE_MARGIN = 36f;
    private static final float LAKE_SPACING = 60f;
    private static final float LAKE_RADIUS_MIN = 12f;
    private static final float LAKE_RADIUS_RANGE = 6f;
    private static final float LAKE_DEPTH = 2.5f;
    private static final float RIM_CLEARANCE = 0.15f;
    private static final float OUTER_RIM_FACTOR = 1.3f;
    private static final int RIM_SAMPLES = 12;
    private static final float MIN_ICE = 0.06f;
    private static final float MAX_ICE = 0.5f;

    private final TerrainCarver carver;
    private final Random random;
    private final Random freezeRandom;
    private final List<float[]> sources = new ArrayList<>();
    private final List<WaterBody> bodies = new ArrayList<>();
    private final WaterSample probe = new WaterSample();

    private WaterGenerator(BaseTerrain base, long mixedSeed) {
        this.carver = new TerrainCarver(base, bodies);
        this.random = new Random(mixedSeed);
        this.freezeRandom = new Random(mixedSeed ^ FREEZE_SALT);
    }

    /**
     * Generates the water bodies for a seed.
     *
     * @param seed world seed
     * @param base the terrain the water is placed on
     * @return immutable list of bodies
     */
    static List<WaterBody> generate(long seed, BaseTerrain base) {
        WaterGenerator generator = new WaterGenerator(base, seed * 0x9E3779B97F4A7C15L + GENERATION_VERSION);
        generator.addLakes();
        for (int i = 0; i < RIVER_COUNT; i++) {
            generator.addFlowing(WaterType.RIVER, RIVER_WIDTH_START, RIVER_WIDTH_END, RIVER_DEPTH_RATIO);
        }
        for (int i = 0; i < STREAM_COUNT; i++) {
            generator.addFlowing(WaterType.STREAM, STREAM_WIDTH_START, STREAM_WIDTH_END, STREAM_DEPTH_RATIO);
        }
        return List.copyOf(generator.bodies);
    }

    /** Ground height shaped by the water placed so far, so new rivers follow existing valleys and lakes. */
    private float ground(float x, float z) {
        return carver.height(x, z);
    }

    private record Trace(List<float[]> points, boolean endsInBasin) {
    }

    private void addFlowing(WaterType type, float widthStart, float widthEnd, float depthRatio) {
        float[] source = pickSource();
        float phase = random.nextFloat() * (float) (2 * Math.PI);
        Trace trace = trace(source[0], source[1]);
        int n = trace.points().size();
        if (n < MIN_POINTS) {
            return;
        }
        float[] xs = new float[n];
        float[] zs = new float[n];
        float[] hw = new float[n];
        float[] surface = new float[n];
        float[] depth = new float[n];
        for (int i = 0; i < n; i++) {
            float[] p = trace.points().get(i);
            float f = i / (float) (n - 1);
            xs[i] = p[0];
            zs[i] = p[1];
            surface[i] = p[2];
            hw[i] = (widthStart + (widthEnd - widthStart) * f)
                    * (1f + WIDTH_VARIATION * (float) Math.sin(i * WIDTH_FREQUENCY + phase));
            depth[i] = hw[i] * depthRatio;
        }
        WaterBody pond = trace.endsInBasin() ? makePond(xs[n - 1], zs[n - 1], surface[n - 1]) : null;
        if (pond != null) {
            // The river's last point sits in the pond, so it takes the pond's level: otherwise the two
            // channel beds blend into a hump above the pond surface.
            surface[n - 1] = Math.min(surface[n - 1], pond.surface(0));
        }
        bodies.add(build(type, xs, zs, hw, surface, depth));
        if (pond != null) {
            bodies.add(pond);
        }
    }

    /** True if a point is within {@code clearance} of any existing body's water edge. */
    private boolean nearExisting(float x, float z, float clearance) {
        probe.reset();
        for (WaterBody body : bodies) {
            if (body.mayInfluence(x, z)) {
                body.sample(x, z, probe);
            }
        }
        return probe.body != null && probe.margin < clearance;
    }

    /** Picks the highest of several random points that is far enough from earlier sources. */
    private float[] pickSource() {
        float range = BaseTerrain.HALF_EXTENT - SOURCE_MARGIN;
        float[] best = {0f, 0f};
        float bestHeight = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < SOURCE_ATTEMPTS; i++) {
            float x = (random.nextFloat() * 2f - 1f) * range;
            float z = (random.nextFloat() * 2f - 1f) * range;
            float h = ground(x, z);
            if (h > bestHeight && farFromSources(x, z) && !nearExisting(x, z, JOIN_MARGIN)) {
                best = new float[] {x, z};
                bestHeight = h;
            }
        }
        sources.add(best);
        return best;
    }

    private boolean farFromSources(float x, float z) {
        for (float[] s : sources) {
            if (Math.hypot(x - s[0], z - s[1]) < SOURCE_SPACING) {
                return false;
            }
        }
        return true;
    }

    /** Walks downhill with momentum; surface never rises and stays below the ground at the centreline. */
    private Trace trace(float startX, float startZ) {
        float x = startX;
        float z = startZ;
        float[] dir = downhill(x, z);
        float h = ground(x, z);
        float lowest = h;
        float surface = h - CLEARANCE;
        List<float[]> points = new ArrayList<>();
        boolean basin = false;
        for (int step = 0; step < MAX_STEPS; step++) {
            points.add(new float[] {x, z, surface});
            float[] d = downhill(x, z);
            float dx = MOMENTUM * dir[0] + (1f - MOMENTUM) * d[0];
            float dz = MOMENTUM * dir[1] + (1f - MOMENTUM) * d[1];
            float len = (float) Math.hypot(dx, dz);
            if (len > 1e-4f) {
                turn(dir, dx / len, dz / len);
            }
            x += dir[0] * STEP;
            z += dir[1] * STEP;
            float limit = BaseTerrain.HALF_EXTENT - EDGE_MARGIN;
            if (Math.abs(x) > limit || Math.abs(z) > limit) {
                break;
            }
            h = ground(x, z);
            if (h > lowest + STUCK_RISE || curlsBackOn(points, x, z)) {
                basin = true;
                break;
            }
            lowest = Math.min(lowest, h);
            surface = Math.min(surface, h - CLEARANCE);
            if (nearExisting(x, z, JOIN_EDGE)) {
                points.add(new float[] {x, z, surface}); // arrived at a lake or river: end at its edge
                break;
            }
        }
        return new Trace(points, basin);
    }

    /**
     * Turns {@code dir} toward the wanted direction by at most {@link #MAX_TURN} radians. Keeps the turning
     * radius above a river's half width, so its ribbon never folds over itself on a bend.
     */
    private static void turn(float[] dir, float wantX, float wantZ) {
        double angle = Math.atan2(dir[0] * wantZ - dir[1] * wantX, dir[0] * wantX + dir[1] * wantZ);
        double clamped = Math.max(-MAX_TURN, Math.min(MAX_TURN, angle));
        double cos = Math.cos(clamped);
        double sin = Math.sin(clamped);
        float nx = (float) (dir[0] * cos - dir[1] * sin);
        float nz = (float) (dir[0] * sin + dir[1] * cos);
        dir[0] = nx;
        dir[1] = nz;
    }

    /** True if a point is close to the river's own earlier path, i.e. it is circling inside a hollow. */
    private static boolean curlsBackOn(List<float[]> points, float x, float z) {
        for (int i = 0; i < points.size() - SELF_SKIP; i++) {
            if (Math.hypot(x - points.get(i)[0], z - points.get(i)[1]) < SELF_CLEARANCE) {
                return true;
            }
        }
        return false;
    }

    /** Unit downhill direction on a smoothed gradient, or an arbitrary fixed direction on flat ground. */
    private float[] downhill(float x, float z) {
        float gx = ground(x + GRADIENT_STEP, z) - ground(x - GRADIENT_STEP, z);
        float gz = ground(x, z + GRADIENT_STEP) - ground(x, z - GRADIENT_STEP);
        float len = (float) Math.hypot(gx, gz);
        if (len < 1e-4f) {
            return new float[] {1f, 0f};
        }
        return new float[] {-gx / len, -gz / len};
    }

    /** A pond for a river that ended in a hollow, or null if it would overlap existing water. */
    private WaterBody makePond(float x, float z, float surfaceCap) {
        float radius = POND_RADIUS_MIN + random.nextFloat() * POND_RADIUS_RANGE;
        if (nearExisting(x, z, radius + JOIN_MARGIN)) {
            return null;
        }
        float surface = Math.min(surfaceCap, rimHeight(x, z, radius) - RIM_CLEARANCE);
        return basin(WaterType.POND, x, z, radius, surface, POND_DEPTH);
    }

    private void addLakes() {
        List<float[]> minima = new ArrayList<>();
        float range = BaseTerrain.HALF_EXTENT - LAKE_MARGIN;
        for (float x = -range; x <= range; x += LAKE_GRID) {
            for (float z = -range; z <= range; z += LAKE_GRID) {
                if (isLocalMinimum(x, z)) {
                    minima.add(new float[] {x, z, ground(x, z)});
                }
            }
        }
        minima.sort(Comparator.comparingDouble(m -> m[2]));
        List<float[]> chosen = new ArrayList<>();
        for (float[] m : minima) {
            if (chosen.size() == LAKE_COUNT) {
                break;
            }
            if (chosen.stream().allMatch(c -> Math.hypot(m[0] - c[0], m[1] - c[1]) >= LAKE_SPACING)) {
                chosen.add(m);
                float radius = LAKE_RADIUS_MIN + random.nextFloat() * LAKE_RADIUS_RANGE;
                float surface = rimHeight(m[0], m[1], radius) - RIM_CLEARANCE;
                bodies.add(basin(WaterType.LAKE, m[0], m[1], radius, surface, LAKE_DEPTH));
            }
        }
    }

    private boolean isLocalMinimum(float x, float z) {
        float h = ground(x, z);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && ground(x + dx * LAKE_GRID, z + dz * LAKE_GRID) <= h) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Lowest ground around a circle, so the water level never sits above any point of the rim. */
    private float rimHeight(float cx, float cz, float radius) {
        float lowest = Float.POSITIVE_INFINITY;
        for (float r : new float[] {radius, radius * OUTER_RIM_FACTOR}) {
            for (int i = 0; i < RIM_SAMPLES; i++) {
                double angle = 2 * Math.PI * i / RIM_SAMPLES;
                lowest = Math.min(lowest,
                        ground(cx + (float) Math.cos(angle) * r, cz + (float) Math.sin(angle) * r));
            }
        }
        return lowest;
    }

    private WaterBody basin(WaterType type, float x, float z, float radius, float surface, float depth) {
        return build(type, new float[] {x}, new float[] {z}, new float[] {radius}, new float[] {surface},
                new float[] {depth});
    }

    private WaterBody build(WaterType type, float[] xs, float[] zs, float[] hw, float[] surface,
            float[] depth) {
        boolean frozen = freezeRandom.nextFloat() < freezeChance(type);
        float ice = MIN_ICE + freezeRandom.nextFloat() * (MAX_ICE - MIN_ICE);
        return new WaterBody(type, xs, zs, hw, surface, depth, frozen, ice);
    }

    private static float freezeChance(WaterType type) {
        return switch (type) {
            case RIVER -> 0.35f;
            case STREAM -> 0.6f;
            case LAKE -> 0.85f;
            case POND -> 0.9f;
        };
    }
}
