package com.coldfriend.world.render;

import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.WaterMap;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** Builds the mesh for one square chunk of terrain or water. Meshes are built once and never changed. */
final class TerrainMeshes {

    private static final int COLOR_CHANNELS = 3;
    private static final float WATER_SINK = 0.1f;
    private static final float[] WATER_RGBA = {0.10f, 0.26f, 0.40f, 0.78f};

    private TerrainMeshes() {
    }

    /**
     * @param terrain the height field
     * @param colors per-vertex colours from {@link TerrainColors}
     * @param startI first vertex column of the chunk
     * @param startJ first vertex row of the chunk
     * @param cells chunk size in cells
     * @return the chunk's mesh
     */
    static Mesh ground(HeightField terrain, float[] colors, int startI, int startJ, int cells) {
        int side = cells + 1;
        float cell = terrain.cellSize();
        FloatBuffer positions = BufferUtils.createFloatBuffer(side * side * 3);
        FloatBuffer normals = BufferUtils.createFloatBuffer(side * side * 3);
        FloatBuffer vertexColors = BufferUtils.createFloatBuffer(side * side * 4);
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                int gi = startI + i;
                int gj = startJ + j;
                positions.put(gi * cell).put(terrain.get(gi, gj)).put(gj * cell);
                float dx = terrain.get(Math.max(0, gi - 1), gj) - terrain.get(Math.min(terrain.cells(), gi + 1), gj);
                float dz = terrain.get(gi, Math.max(0, gj - 1)) - terrain.get(gi, Math.min(terrain.cells(), gj + 1));
                float length = (float) Math.sqrt(dx * dx + 4f * cell * cell + dz * dz);
                normals.put(dx / length).put(2f * cell / length).put(dz / length);
                int base = (gj * terrain.vertices() + gi) * COLOR_CHANNELS;
                vertexColors.put(colors[base]).put(colors[base + 1]).put(colors[base + 2]).put(1f);
            }
        }
        Mesh mesh = quads(side, cells, positions);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, flip(normals));
        mesh.setBuffer(VertexBuffer.Type.Color, 4, flip(vertexColors));
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

    /**
     * @return a mesh of the water surface in the chunk, or null if the chunk has no water
     */
    static Mesh water(HeightField terrain, WaterMap water, int startI, int startJ, int cells) {
        int side = cells + 1;
        float cell = terrain.cellSize();
        boolean any = false;
        FloatBuffer positions = BufferUtils.createFloatBuffer(side * side * 3);
        FloatBuffer vertexColors = BufferUtils.createFloatBuffer(side * side * 4);
        FloatBuffer normals = BufferUtils.createFloatBuffer(side * side * 3);
        boolean[] wet = new boolean[side * side];
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                int gi = startI + i;
                int gj = startJ + j;
                float surface = water.surface(gi, gj);
                boolean isWet = surface > Float.NEGATIVE_INFINITY;
                wet[j * side + i] = isWet;
                any |= isWet;
                float y = isWet ? surface : terrain.get(gi, gj) - WATER_SINK;
                positions.put(gi * cell).put(y).put(gj * cell);
                normals.put(0f).put(1f).put(0f);
                vertexColors.put(WATER_RGBA[0]).put(WATER_RGBA[1]).put(WATER_RGBA[2]).put(WATER_RGBA[3]);
            }
        }
        if (!any) {
            return null;
        }
        FloatBuffer finished = flip(positions);
        int quads = 0;
        IntBuffer indices = BufferUtils.createIntBuffer(cells * cells * 6);
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                if (wet[j * side + i] || wet[j * side + i + 1] || wet[(j + 1) * side + i]
                        || wet[(j + 1) * side + i + 1]) {
                    addQuad(indices, side, i, j);
                    quads++;
                }
            }
        }
        indices.flip();
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, finished);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, flip(normals));
        mesh.setBuffer(VertexBuffer.Type.Color, 4, flip(vertexColors));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
        mesh.updateBound();
        mesh.updateCounts();
        return quads == 0 ? null : mesh;
    }

    private static Mesh quads(int side, int cells, FloatBuffer positions) {
        IntBuffer indices = BufferUtils.createIntBuffer(cells * cells * 6);
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                addQuad(indices, side, i, j);
            }
        }
        indices.flip();
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, flip(positions));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
        return mesh;
    }

    private static void addQuad(IntBuffer indices, int side, int i, int j) {
        int a = j * side + i;
        int b = (j + 1) * side + i;
        int c = j * side + i + 1;
        int d = (j + 1) * side + i + 1;
        indices.put(a).put(b).put(c).put(c).put(b).put(d);
    }

    private static FloatBuffer flip(FloatBuffer buffer) {
        buffer.flip();
        return buffer;
    }
}
