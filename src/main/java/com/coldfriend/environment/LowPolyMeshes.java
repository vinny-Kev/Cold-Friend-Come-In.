package com.coldfriend.environment;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds small faceted meshes from triangles. Each triangle carries its own flat normal, which gives the
 * low-poly look the game uses.
 */
final class LowPolyMeshes {

    private LowPolyMeshes() {
    }

    /**
     * A cone with its apex up and its base centred on the origin. The rim is jittered so no two cones match.
     *
     * @param sides number of sides around the rim, at least 3
     * @param radius rim radius before jitter
     * @param height apex height above the origin
     * @param jitter fraction of the radius by which each rim point may vary, 0 for a perfect cone
     * @param random source of the jitter
     * @return the cone mesh
     */
    static Mesh cone(int sides, float radius, float height, float jitter, Random random) {
        Vector3f apex = new Vector3f(0f, height, 0f);
        Vector3f[] rim = new Vector3f[sides];
        for (int i = 0; i < sides; i++) {
            float angle = FastMath.TWO_PI * i / sides;
            float r = radius * (1f + jitter * (random.nextFloat() * 2f - 1f));
            rim[i] = new Vector3f(FastMath.cos(angle) * r, 0f, FastMath.sin(angle) * r);
        }
        List<Vector3f> triangles = new ArrayList<>(sides * 3);
        for (int i = 0; i < sides; i++) {
            Vector3f a = rim[i];
            Vector3f b = rim[(i + 1) % sides];
            // Wind every triangle so its normal points away from the cone's axis.
            Vector3f outward = a.add(b).mult(0.5f).subtractLocal(apex.mult(0.25f));
            if (faceNormal(apex, a, b).dot(outward) >= 0f) {
                addTriangle(triangles, apex, a, b);
            } else {
                addTriangle(triangles, apex, b, a);
            }
        }
        return faceted(triangles);
    }

    /**
     * Builds a mesh from triangle vertices, three per triangle, with flat normals computed from winding.
     *
     * @param vertices three vertices per triangle, counter-clockwise when seen from outside
     * @return the mesh
     */
    static Mesh faceted(List<Vector3f> vertices) {
        FloatBuffer positions = BufferUtils.createFloatBuffer(vertices.size() * 3);
        FloatBuffer normals = BufferUtils.createFloatBuffer(vertices.size() * 3);
        for (int i = 0; i + 2 < vertices.size(); i += 3) {
            Vector3f a = vertices.get(i);
            Vector3f b = vertices.get(i + 1);
            Vector3f c = vertices.get(i + 2);
            Vector3f normal = faceNormal(a, b, c);
            for (Vector3f v : List.of(a, b, c)) {
                positions.put(v.x).put(v.y).put(v.z);
                normals.put(normal.x).put(normal.y).put(normal.z);
            }
        }
        positions.flip();
        normals.flip();
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Triangles);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, positions);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, normals);
        mesh.updateBound();
        return mesh;
    }

    private static void addTriangle(List<Vector3f> out, Vector3f a, Vector3f b, Vector3f c) {
        out.add(a);
        out.add(b);
        out.add(c);
    }

    private static Vector3f faceNormal(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f ab = b.subtract(a);
        Vector3f ac = c.subtract(a);
        return ab.cross(ac).normalizeLocal();
    }
}
