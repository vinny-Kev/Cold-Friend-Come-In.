package com.coldfriend.environment;

import com.coldfriend.world.BaseTerrain;
import com.coldfriend.world.BushPlacer;
import com.coldfriend.world.BushSpawn;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Renders bushes that sway in the wind. Bushes are baked into one mesh per square area of the world (so
 * hundreds of bushes cost a handful of draw calls) and all share one material. The swaying is done in the
 * shader from engine time, so this class does no per-frame work. Call {@link #dispose()} when the world goes
 * away.
 */
public final class FoliageView {

    private static final String MATERIAL = "MatDefs/Foliage.j3md";
    /** Side of one batch in metres; culling works per batch, so smaller batches cull better. */
    private static final float CHUNK_SIZE = 64f;
    /** Bushes sit this far (per unit of scale) below the ground point so they never hover on a slope. */
    private static final float SINK = 0.15f;

    private final Node node = new Node("foliage");
    private final Material material;
    private final List<Mesh> meshes = new ArrayList<>();

    /**
     * Builds the batched geometry. Nothing is rendered until {@link #node()} is attached.
     *
     * @param assets engine asset manager
     * @param bushes where the bushes stand
     * @param lighting initial light values
     * @param wind initial wind
     */
    public FoliageView(AssetManager assets, List<BushSpawn> bushes, SceneLighting lighting, Wind wind) {
        material = new Material(assets, MATERIAL);
        setLighting(lighting);
        setWind(wind);
        BushMeshBuilder.Template[] templates = new BushMeshBuilder.Template[BushPlacer.VARIANTS];
        for (int v = 0; v < templates.length; v++) {
            templates[v] = BushMeshBuilder.template(v);
        }
        Map<Long, List<BushSpawn>> chunks = new TreeMap<>();
        for (BushSpawn bush : bushes) {
            chunks.computeIfAbsent(chunkKey(bush.x(), bush.z()), k -> new ArrayList<>()).add(bush);
        }
        for (Map.Entry<Long, List<BushSpawn>> chunk : chunks.entrySet()) {
            Mesh mesh = buildChunk(chunk.getValue(), templates);
            meshes.add(mesh);
            Geometry geometry = new Geometry("foliage-" + chunk.getKey(), mesh);
            geometry.setMaterial(material);
            geometry.setQueueBucket(RenderQueue.Bucket.Opaque);
            geometry.setShadowMode(RenderQueue.ShadowMode.Off);
            node.attachChild(geometry);
        }
    }

    private static long chunkKey(float x, float z) {
        long cx = (long) Math.floor((x + BaseTerrain.HALF_EXTENT) / CHUNK_SIZE);
        long cz = (long) Math.floor((z + BaseTerrain.HALF_EXTENT) / CHUNK_SIZE);
        return cx * 1000L + cz;
    }

    /** Bakes every bush of a chunk, placed in world space, into one mesh. */
    private static Mesh buildChunk(List<BushSpawn> bushes, BushMeshBuilder.Template[] templates) {
        int vertices = 0;
        for (BushSpawn b : bushes) {
            vertices += templates[b.variant()].vertexCount();
        }
        float[] pos = new float[vertices * 3];
        float[] nrm = new float[vertices * 3];
        float[] uv = new float[vertices * 2];
        float[] col = new float[vertices * 4];
        int[] idx = new int[vertices];
        int out = 0;
        for (BushSpawn b : bushes) {
            BushMeshBuilder.Template t = templates[b.variant()];
            float cos = (float) Math.cos(b.yaw());
            float sin = (float) Math.sin(b.yaw());
            for (int i = 0; i < t.vertexCount(); i++, out++) {
                float px = t.positions()[i * 3] * b.scale();
                float py = t.positions()[i * 3 + 1] * b.scale();
                float pz = t.positions()[i * 3 + 2] * b.scale();
                pos[out * 3] = b.x() + px * cos + pz * sin;
                pos[out * 3 + 1] = b.y() + py - SINK * b.scale();
                pos[out * 3 + 2] = b.z() - px * sin + pz * cos;
                float nx = t.normals()[i * 3];
                float nz = t.normals()[i * 3 + 2];
                nrm[out * 3] = nx * cos + nz * sin;
                nrm[out * 3 + 1] = t.normals()[i * 3 + 1];
                nrm[out * 3 + 2] = -nx * sin + nz * cos;
                uv[out * 2] = t.texCoords()[i * 2];
                uv[out * 2 + 1] = t.texCoords()[i * 2 + 1];
                for (int ch = 0; ch < 3; ch++) {
                    col[out * 4 + ch] = Math.min(1f, t.colors()[i * 4 + ch] * b.tint());
                }
                col[out * 4 + 3] = 1f;
                idx[out] = out;
            }
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, pos);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, nrm);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uv);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, col);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, idx);
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

    /** @return the scene node to attach to the world */
    public Node node() {
        return node;
    }

    /**
     * Changes the wind, for example calm to storm. Cheap: uniforms only, no rebuild.
     *
     * @param wind new wind
     */
    public void setWind(Wind wind) {
        material.setVector3("WindDirection", new Vector3f(wind.dirX(), 0f, wind.dirZ()).normalizeLocal());
        material.setFloat("WindStrength", wind.strength());
        material.setFloat("GustStrength", wind.gust());
    }

    /**
     * Updates the light values, for example when day turns to night. Cheap; call only when lighting changes.
     *
     * @param lighting new light values
     */
    public void setLighting(SceneLighting lighting) {
        material.setVector3("SunDirection", lighting.sunDirection().normalize());
        material.setColor("SunColor", lighting.sun());
        material.setColor("AmbientColor", lighting.ambient());
    }

    /** Detaches the foliage from the scene and frees its mesh buffers. The view must not be used afterwards. */
    public void dispose() {
        node.removeFromParent();
        node.detachAllChildren();
        for (Mesh mesh : meshes) {
            MeshCleanup.release(mesh);
        }
        meshes.clear();
    }

    /** @return number of batches currently held */
    int geometryCount() {
        int count = 0;
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry) {
                count++;
            }
        }
        return count;
    }
}
