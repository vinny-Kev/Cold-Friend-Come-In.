package com.coldfriend.environment;

import com.coldfriend.world.BaseTerrain;
import com.coldfriend.world.GeneratedWorld;
import com.coldfriend.world.WaterSample;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

/**
 * Builds a flat-shaded low-poly mesh of a {@link GeneratedWorld}'s carved terrain, coloured as snow with a
 * dark bed under water and a damp tint along the shore. Built once; there is no per-frame cost.
 */
public final class TerrainMeshBuilder {

    private static final int CELLS = 170;
    private static final float SHORE_TINT_RANGE = 2f;
    private static final float BED_DEPTH_FOR_DARKEST = 2f;
    private static final ColorRGBA SNOW = new ColorRGBA(0.88f, 0.91f, 0.96f, 1f);
    private static final ColorRGBA DAMP = new ColorRGBA(0.62f, 0.66f, 0.73f, 1f);
    /** Matches {@link #DAMP} so the bed meets the shore tint without a dark ring at the water's edge. */
    private static final ColorRGBA BED_SHALLOW = DAMP;
    private static final ColorRGBA BED_DEEP = new ColorRGBA(0.10f, 0.14f, 0.18f, 1f);

    private TerrainMeshBuilder() {
    }

    /**
     * Builds the terrain geometry.
     *
     * @param assets engine asset manager
     * @param world the world to draw
     * @return geometry using vertex-coloured lighting; release it with {@link MeshCleanup#release}
     */
    public static Geometry build(AssetManager assets, GeneratedWorld world) {
        int verts = CELLS + 1;
        float size = BaseTerrain.HALF_EXTENT * 2f;
        float cell = size / CELLS;
        float[] heights = new float[verts * verts];
        float[][] colors = new float[verts * verts][];
        WaterSample sample = new WaterSample();
        for (int j = 0; j < verts; j++) {
            for (int i = 0; i < verts; i++) {
                float x = -BaseTerrain.HALF_EXTENT + i * cell;
                float z = -BaseTerrain.HALF_EXTENT + j * cell;
                float h = world.terrainHeight(x, z);
                heights[j * verts + i] = h;
                world.sampleWater(x, z, sample);
                colors[j * verts + i] = colorAt(sample, h);
            }
        }

        int triangles = CELLS * CELLS * 2;
        float[] pos = new float[triangles * 9];
        float[] nrm = new float[triangles * 9];
        float[] col = new float[triangles * 12];
        int[] idx = new int[triangles * 3];
        int t = 0;
        for (int j = 0; j < CELLS; j++) {
            for (int i = 0; i < CELLS; i++) {
                int a = j * verts + i;
                int b = a + 1;
                int c = a + verts;
                int d = c + 1;
                t = addTriangle(pos, nrm, col, idx, t, heights, colors, cell, a, c, b);
                t = addTriangle(pos, nrm, col, idx, t, heights, colors, cell, b, c, d);
            }
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, pos);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, nrm);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, col);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, idx);
        mesh.updateBound();
        mesh.updateCounts();

        Material material = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseVertexColor", true);
        Geometry geometry = new Geometry("terrain", mesh);
        geometry.setMaterial(material);
        return geometry;
    }

    /** Colour of one grid vertex: bed under water, damp near the shore, snow elsewhere. */
    private static float[] colorAt(WaterSample s, float groundHeight) {
        ColorRGBA c = SNOW;
        if (s.body != null) {
            if (s.margin <= 0f) {
                float depth = Math.max(0f, s.surfaceY - groundHeight);
                c = lerp(BED_SHALLOW, BED_DEEP, Math.min(1f, depth / BED_DEPTH_FOR_DARKEST));
            } else if (s.margin < SHORE_TINT_RANGE) {
                c = lerp(DAMP, SNOW, s.margin / SHORE_TINT_RANGE);
            }
        }
        return new float[] {c.r, c.g, c.b, 1f};
    }

    private static ColorRGBA lerp(ColorRGBA a, ColorRGBA b, float t) {
        return new ColorRGBA(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t, a.b + (b.b - a.b) * t, 1f);
    }

    /** Appends one flat-shaded triangle (counter-clockwise from above) and returns the next triangle index. */
    private static int addTriangle(float[] pos, float[] nrm, float[] col, int[] idx, int t, float[] heights,
            float[][] colors, float cell, int v0, int v1, int v2) {
        int[] v = {v0, v1, v2};
        float[] px = new float[3];
        float[] py = new float[3];
        float[] pz = new float[3];
        int verts = CELLS + 1;
        for (int k = 0; k < 3; k++) {
            int gi = v[k] % verts;
            int gj = v[k] / verts;
            px[k] = -BaseTerrain.HALF_EXTENT + gi * cell;
            py[k] = heights[v[k]];
            pz[k] = -BaseTerrain.HALF_EXTENT + gj * cell;
        }
        float ux = px[1] - px[0];
        float uy = py[1] - py[0];
        float uz = pz[1] - pz[0];
        float wx = px[2] - px[0];
        float wy = py[2] - py[0];
        float wz = pz[2] - pz[0];
        float nx = uy * wz - uz * wy;
        float ny = uz * wx - ux * wz;
        float nz = ux * wy - uy * wx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len;
        ny /= len;
        nz /= len;
        for (int k = 0; k < 3; k++) {
            int o = t * 9 + k * 3;
            pos[o] = px[k];
            pos[o + 1] = py[k];
            pos[o + 2] = pz[k];
            nrm[o] = nx;
            nrm[o + 1] = ny;
            nrm[o + 2] = nz;
            idx[t * 3 + k] = t * 3 + k;
        }
        // Flat shading: every corner takes the average colour of the triangle.
        float[] avg = new float[4];
        for (int k = 0; k < 3; k++) {
            for (int ch = 0; ch < 4; ch++) {
                avg[ch] += colors[v[k]][ch] / 3f;
            }
        }
        for (int k = 0; k < 3; k++) {
            System.arraycopy(avg, 0, col, t * 12 + k * 4, 4);
        }
        return t + 1;
    }
}
