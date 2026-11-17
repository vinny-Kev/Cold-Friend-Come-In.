package com.coldfriend.environment;

import com.coldfriend.world.GeneratedWorld;
import com.coldfriend.world.WaterBody;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders a world's water: one geometry per body, all sharing one material. Surface movement is done in the
 * shader from engine time, so this class does no per-frame work. Call {@link #dispose()} when the world goes
 * away to release the mesh buffers.
 */
public final class WaterView {

    private static final String MATERIAL = "MatDefs/Water.j3md";
    private static final ColorRGBA SHALLOW = new ColorRGBA(0.30f, 0.48f, 0.56f, 1f);
    private static final ColorRGBA DEEP = new ColorRGBA(0.06f, 0.16f, 0.24f, 1f);
    private static final ColorRGBA ICE = new ColorRGBA(0.80f, 0.90f, 0.96f, 1f);

    private final Node node = new Node("water");
    private final Material material;
    private final List<Mesh> meshes = new ArrayList<>();

    /**
     * Builds geometry for every water body. Nothing is rendered until {@link #node()} is attached.
     *
     * @param assets engine asset manager
     * @param world world whose water is drawn
     * @param lighting initial light values
     */
    public WaterView(AssetManager assets, GeneratedWorld world, SceneLighting lighting) {
        material = new Material(assets, MATERIAL);
        material.setColor("ShallowColor", SHALLOW);
        material.setColor("DeepColor", DEEP);
        material.setColor("IceColor", ICE);
        setLighting(lighting);
        for (WaterBody body : world.waterBodies()) {
            Mesh mesh = WaterMeshBuilder.build(body);
            meshes.add(mesh);
            Geometry geometry = new Geometry("water-" + body.type().name().toLowerCase(Locale.ROOT), mesh);
            geometry.setMaterial(material);
            geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
            geometry.setShadowMode(RenderQueue.ShadowMode.Off);
            node.attachChild(geometry);
        }
    }

    /** @return the scene node to attach to the world */
    public Node node() {
        return node;
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
        material.setColor("SkyColor", lighting.sky());
    }

    /** Detaches the water from the scene and frees its mesh buffers. The view must not be used afterwards. */
    public void dispose() {
        node.removeFromParent();
        node.detachAllChildren();
        for (Mesh mesh : meshes) {
            MeshCleanup.release(mesh);
        }
        meshes.clear();
    }

    /** @return number of water geometries currently held */
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
