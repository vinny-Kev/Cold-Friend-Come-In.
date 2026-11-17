package com.coldfriend.environment;

import java.util.Random;

/**
 * Builds the shape of one bush as plain vertex arrays: a clump of faceted blobs, snow settled on the upward
 * faces. Every vertex carries what {@code Foliage.vert} needs: a sway weight that is zero at the ground and
 * grows toward the tips, and a random phase per blob so blobs flutter out of step.
 *
 * <p>Output is flat-shaded (three vertices per triangle) in bush-local space, standing on y = 0.
 */
final class BushMeshBuilder {

    /** Vertex data for one bush shape. */
    record Template(float[] positions, float[] normals, float[] texCoords, float[] colors) {

        int vertexCount() {
            return positions.length / 3;
        }
    }

    private static final long SHAPE_SEED = 0x5EED_B05EL;
    private static final int SIDES = 7;
    private static final int MIN_BLOBS = 4;
    private static final int BLOB_RANGE = 3;
    private static final float SPREAD = 0.55f;
    private static final float RADIUS_MIN = 0.45f;
    private static final float RADIUS_RANGE = 0.25f;
    private static final float CENTRE_LIFT = 0.5f;
    private static final float CENTRE_LIFT_RANGE = 0.3f;
    private static final float TOP = 0.8f;
    private static final float BOTTOM = 0.45f;
    /** Rings between the apexes as {height fraction of radius, width fraction of radius}. */
    private static final float[][] RINGS = {{0.4f, 0.75f}, {0f, 1f}, {-0.25f, 0.7f}};
    private static final int TRIANGLES_PER_SIDE = 6;
    private static final float JITTER = 0.2f;
    private static final float BUSH_HEIGHT = 1.4f;
    private static final float SNOW_START = 0.55f;
    private static final float SNOW_RANGE = 0.3f;
    private static final float[] LEAF = {0.20f, 0.28f, 0.20f};
    private static final float[] SNOW = {0.92f, 0.95f, 1.0f};
    private static final float LEAF_VARIATION = 0.25f;

    private BushMeshBuilder() {
    }

    /**
     * @param variant shape index; the same index always gives the same shape
     * @return vertex data for that shape
     */
    static Template template(int variant) {
        Random random = new Random(SHAPE_SEED + variant * 7919L);
        int blobs = MIN_BLOBS + random.nextInt(BLOB_RANGE);
        int vertices = blobs * SIDES * TRIANGLES_PER_SIDE * 3;
        Template t = new Template(new float[vertices * 3], new float[vertices * 3], new float[vertices * 2],
                new float[vertices * 4]);
        int next = 0;
        for (int b = 0; b < blobs; b++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            float spread = random.nextFloat() * SPREAD;
            float radius = RADIUS_MIN + random.nextFloat() * RADIUS_RANGE;
            float cx = (float) Math.cos(angle) * spread;
            float cz = (float) Math.sin(angle) * spread;
            float cy = radius * CENTRE_LIFT + random.nextFloat() * CENTRE_LIFT_RANGE;
            float phase = random.nextFloat();
            float shade = 1f + (random.nextFloat() * 2f - 1f) * LEAF_VARIATION;
            next = addBlob(t, next, cx, cy, cz, radius, phase, shade, random);
        }
        return t;
    }

    /** Adds one blob: three rings of points with an apex above and below. Returns the next free vertex index. */
    private static int addBlob(Template t, int first, float cx, float cy, float cz, float radius, float phase,
            float shade, Random random) {
        float[] jitter = new float[SIDES];
        for (int i = 0; i < SIDES; i++) {
            jitter[i] = 1f + JITTER * (random.nextFloat() * 2f - 1f);
        }
        float[][][] rings = new float[RINGS.length][SIDES][];
        for (int r = 0; r < RINGS.length; r++) {
            for (int i = 0; i < SIDES; i++) {
                double a = 2 * Math.PI * i / SIDES;
                float width = radius * RINGS[r][1] * jitter[i];
                rings[r][i] = new float[] {cx + (float) Math.cos(a) * width, cy + radius * RINGS[r][0],
                        cz + (float) Math.sin(a) * width};
            }
        }
        float[] top = {cx, cy + radius * TOP, cz};
        float[] bottom = {cx, cy - radius * BOTTOM, cz};
        float[] centre = {cx, cy, cz};
        int v = first;
        for (int i = 0; i < SIDES; i++) {
            int j = (i + 1) % SIDES;
            v = addTriangle(t, v, top, rings[0][i], rings[0][j], centre, phase, shade);
            for (int r = 0; r < RINGS.length - 1; r++) {
                v = addTriangle(t, v, rings[r][i], rings[r + 1][i], rings[r][j], centre, phase, shade);
                v = addTriangle(t, v, rings[r][j], rings[r + 1][i], rings[r + 1][j], centre, phase, shade);
            }
            v = addTriangle(t, v, bottom, rings[RINGS.length - 1][i], rings[RINGS.length - 1][j], centre, phase,
                    shade);
        }
        return v;
    }

    /** Adds a flat-shaded triangle wound so its normal points away from {@code centre}. */
    private static int addTriangle(Template t, int first, float[] p0, float[] p1, float[] p2, float[] centre,
            float phase, float shade) {
        float[] n = normal(p0, p1, p2);
        float outward = n[0] * ((p0[0] + p1[0] + p2[0]) / 3f - centre[0])
                + n[1] * ((p0[1] + p1[1] + p2[1]) / 3f - centre[1])
                + n[2] * ((p0[2] + p1[2] + p2[2]) / 3f - centre[2]);
        float[][] corners = outward >= 0f ? new float[][] {p0, p1, p2} : new float[][] {p0, p2, p1};
        if (outward < 0f) {
            n = new float[] {-n[0], -n[1], -n[2]};
        }
        float snow = Math.max(0f, Math.min(1f, (n[1] - SNOW_START) / SNOW_RANGE));
        for (int k = 0; k < 3; k++) {
            int v = first + k;
            float[] p = corners[k];
            System.arraycopy(p, 0, t.positions(), v * 3, 3);
            System.arraycopy(n, 0, t.normals(), v * 3, 3);
            float h = Math.max(0f, Math.min(1f, p[1] / BUSH_HEIGHT));
            t.texCoords()[v * 2] = h * h;
            t.texCoords()[v * 2 + 1] = phase;
            for (int ch = 0; ch < 3; ch++) {
                t.colors()[v * 4 + ch] = LEAF[ch] * shade + (SNOW[ch] - LEAF[ch] * shade) * snow;
            }
            t.colors()[v * 4 + 3] = 1f;
        }
        return first + 3;
    }

    private static float[] normal(float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0];
        float uy = b[1] - a[1];
        float uz = b[2] - a[2];
        float wx = c[0] - a[0];
        float wy = c[1] - a[1];
        float wz = c[2] - a[2];
        float nx = uy * wz - uz * wy;
        float ny = uz * wx - ux * wz;
        float nz = ux * wy - uy * wx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        return new float[] {nx / len, ny / len, nz / len};
    }
}
