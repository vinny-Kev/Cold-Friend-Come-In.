package com.coldfriend.environment;

import com.coldfriend.world.WaterBody;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

/**
 * Builds the visible surface for one {@link WaterBody}: a ribbon along a river's centreline or a disc for a
 * lake. Meshes are built once; animation happens in the shader. Per-vertex data (see {@code Water.vert}) is
 * texture coordinates for the ripple pattern plus shoreline fade, depth, flow speed and frozen flag.
 */
final class WaterMeshBuilder {

    /** Cross-section positions as a fraction of half width; the outer pair forms the shoreline band. */
    private static final float[] ACROSS = {-1f, -0.8f, 0f, 0.8f, 1f};
    private static final float[] SHORE = {0f, 1f, 1f, 1f, 0f};
    private static final float[] RING = {0f, 0.8f, 1f};
    private static final float[] RING_SHORE = {1f, 1f, 0f};
    private static final int DISC_SEGMENTS = 32;
    private static final float DEPTH_NORM = 3f;
    private static final float SURFACE_LIFT = 0.01f;

    private WaterMeshBuilder() {
    }

    /**
     * @param body the water body
     * @return a new mesh; the caller owns it and must dispose of it
     */
    static Mesh build(WaterBody body) {
        return body.pointCount() == 1 ? disc(body) : ribbon(body);
    }

    private static Mesh ribbon(WaterBody body) {
        int n = body.pointCount();
        int cols = ACROSS.length;
        float[] pos = new float[n * cols * 3];
        float[] uv = new float[n * cols * 2];
        float[] col = new float[n * cols * 4];
        float along = 0f;
        for (int i = 0; i < n; i++) {
            int prev = Math.max(i - 1, 0);
            int next = Math.min(i + 1, n - 1);
            float tx = body.x(next) - body.x(prev);
            float tz = body.z(next) - body.z(prev);
            float len = (float) Math.hypot(tx, tz);
            tx /= len;
            tz /= len;
            if (i > 0) {
                along += (float) Math.hypot(body.x(i) - body.x(i - 1), body.z(i) - body.z(i - 1));
            }
            float hw = body.halfWidth(i);
            float flow = body.flowSpeed(i) / WaterBody.maxFlowSpeed();
            for (int c = 0; c < cols; c++) {
                int v = i * cols + c;
                float offset = ACROSS[c] * hw;
                pos[v * 3] = body.x(i) - tz * offset;
                pos[v * 3 + 1] = body.surface(i) + SURFACE_LIFT;
                pos[v * 3 + 2] = body.z(i) + tx * offset;
                uv[v * 2] = offset;
                uv[v * 2 + 1] = along;
                setInfo(col, v, SHORE[c], profileDepth(body.depth(i), ACROSS[c]), flow, body.isFrozen());
            }
        }
        int[] idx = new int[(n - 1) * (cols - 1) * 6];
        int k = 0;
        for (int i = 0; i < n - 1; i++) {
            for (int c = 0; c < cols - 1; c++) {
                int a = i * cols + c;
                int b = a + 1;
                int d = a + cols;
                int e = d + 1;
                idx[k++] = a;
                idx[k++] = d;
                idx[k++] = b;
                idx[k++] = b;
                idx[k++] = d;
                idx[k++] = e;
            }
        }
        return mesh(pos, uv, col, idx);
    }

    private static Mesh disc(WaterBody body) {
        int rings = RING.length;
        int perRing = DISC_SEGMENTS;
        int count = 1 + (rings - 1) * perRing;
        float[] pos = new float[count * 3];
        float[] uv = new float[count * 2];
        float[] col = new float[count * 4];
        float radius = body.halfWidth(0);
        for (int r = 0; r < rings; r++) {
            int segments = r == 0 ? 1 : perRing;
            for (int s = 0; s < segments; s++) {
                int v = r == 0 ? 0 : 1 + (r - 1) * perRing + s;
                double angle = 2 * Math.PI * s / perRing;
                float x = body.x(0) + (float) Math.cos(angle) * radius * RING[r];
                float z = body.z(0) + (float) Math.sin(angle) * radius * RING[r];
                pos[v * 3] = x;
                pos[v * 3 + 1] = body.surface(0) + SURFACE_LIFT;
                pos[v * 3 + 2] = z;
                uv[v * 2] = x;
                uv[v * 2 + 1] = z;
                setInfo(col, v, RING_SHORE[r], profileDepth(body.depth(0), RING[r]), 0f, body.isFrozen());
            }
        }
        int[] idx = new int[(perRing + (rings - 2) * perRing * 2) * 3];
        int k = 0;
        for (int s = 0; s < perRing; s++) {
            int next = (s + 1) % perRing;
            idx[k++] = 0;
            idx[k++] = 1 + next;
            idx[k++] = 1 + s;
        }
        for (int r = 1; r < rings - 1; r++) {
            int inner = 1 + (r - 1) * perRing;
            int outer = inner + perRing;
            for (int s = 0; s < perRing; s++) {
                int next = (s + 1) % perRing;
                idx[k++] = inner + s;
                idx[k++] = inner + next;
                idx[k++] = outer + s;
                idx[k++] = inner + next;
                idx[k++] = outer + next;
                idx[k++] = outer + s;
            }
        }
        return mesh(pos, uv, col, idx);
    }

    /** Same parabolic profile the terrain carve uses, normalised for the shader's depth tint. */
    private static float profileDepth(float centreDepth, float fractionOfHalfWidth) {
        float shape = 1f - fractionOfHalfWidth * fractionOfHalfWidth;
        return Math.min(1f, centreDepth * shape / DEPTH_NORM);
    }

    private static void setInfo(float[] col, int v, float shore, float depth, float flow, boolean frozen) {
        col[v * 4] = shore;
        col[v * 4 + 1] = depth;
        col[v * 4 + 2] = flow;
        col[v * 4 + 3] = frozen ? 1f : 0f;
    }

    private static Mesh mesh(float[] pos, float[] uv, float[] col, int[] idx) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, pos);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uv);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, col);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, idx);
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }
}
