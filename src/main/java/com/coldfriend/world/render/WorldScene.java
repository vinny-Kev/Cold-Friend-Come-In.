package com.coldfriend.world.render;

import com.coldfriend.assets.PropCatalog;
import com.coldfriend.assets.PropKind;
import com.coldfriend.assets.PropModel;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.Placement;
import com.coldfriend.world.model.WorldData;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.instancing.InstancedNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The drawn world: terrain and water in square chunks, and scenery batched per chunk with hardware instancing
 * so thousands of trees cost a few draw calls. Scenery chunks are attached only near the viewer. This class
 * holds visuals only; everything the game decides lives in {@link WorldData}.
 */
public final class WorldScene {

    private static final Logger LOG = LoggerFactory.getLogger(WorldScene.class);
    private static final int CHUNK_CELLS = 32;
    private static final float LARGE_PROP_RADIUS = 220f;
    private static final float SMALL_PROP_RADIUS = 90f;
    private static final float HALF_DIAGONAL = 0.7072f;
    private static final String LIGHTING = "Common/MatDefs/Light/Lighting.j3md";
    private static final ColorRGBA WHITE = ColorRGBA.White;

    private final Node root = new Node("world");
    private final Node propsRoot = new Node("props");
    private final float chunkSize;
    private final int chunksPerSide;
    private final Node[] largeProps;
    private final Node[] smallProps;
    private CabinView cabin;

    /**
     * Builds all the geometry for a world. This is slow, so do it once while loading.
     *
     * @param world the generated world
     * @param catalog the model catalogue
     * @param assets the engine asset manager
     */
    public WorldScene(WorldData world, PropCatalog catalog, AssetManager assets) {
        HeightField terrain = world.terrain();
        this.chunkSize = CHUNK_CELLS * terrain.cellSize();
        this.chunksPerSide = terrain.cells() / CHUNK_CELLS;
        this.largeProps = new Node[chunksPerSide * chunksPerSide];
        this.smallProps = new Node[chunksPerSide * chunksPerSide];
        root.attachChild(propsRoot);
        buildGround(world, assets);
        buildProps(world, catalog);
        buildStructures(world, assets);
    }

    /** @return the node to attach to the scene; it holds the terrain, water and nearby scenery */
    public Node root() {
        return root;
    }

    /**
     * Shows scenery chunks near the viewer and hides the rest.
     *
     * @param x viewer world x
     * @param z viewer world z
     */
    public void update(float x, float z) {
        for (int cj = 0; cj < chunksPerSide; cj++) {
            for (int ci = 0; ci < chunksPerSide; ci++) {
                float dx = (ci + 0.5f) * chunkSize - x;
                float dz = (cj + 0.5f) * chunkSize - z;
                float distance = (float) Math.sqrt(dx * dx + dz * dz) - chunkSize * HALF_DIAGONAL;
                int index = cj * chunksPerSide + ci;
                show(largeProps[index], distance < LARGE_PROP_RADIUS);
                show(smallProps[index], distance < SMALL_PROP_RADIUS);
            }
        }
    }

    /** @param open whether the cabin door is drawn open */
    public void setCabinDoorOpen(boolean open) {
        cabin.setDoorOpen(open);
    }

    /**
     * Plays animations: the door swinging and the firelight flickering.
     *
     * @param tpf seconds since the last frame
     */
    public void animate(float tpf) {
        cabin.update(tpf);
    }

    /** Removes the world from the scene it is in. The geometry is released when nothing refers to it. */
    public void detach() {
        propsRoot.detachAllChildren();
        root.removeFromParent();
    }

    /** Draws the cabin and the village. */
    private void buildStructures(WorldData world, AssetManager assets) {
        VillageView village = new VillageView(world.village(), assets,
                world.terrain().heightAt(world.village().x(), world.village().z()));
        root.attachChild(village.root());
        village.addLightsTo(root);
        cabin = new CabinView(world.cabin(), assets);
        root.attachChild(cabin.root());
        cabin.addLightsTo(root);
    }

    private void show(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        if (visible && node.getParent() == null) {
            propsRoot.attachChild(node);
        } else if (!visible && node.getParent() != null) {
            node.removeFromParent();
        }
    }

    private void buildGround(WorldData world, AssetManager assets) {
        HeightField terrain = world.terrain();
        float[] colors = TerrainColors.compute(world);
        Material ground = new Material(assets, LIGHTING);
        ground.setBoolean("UseVertexColor", true);
        ground.setBoolean("UseMaterialColors", true);
        ground.setColor("Diffuse", WHITE);
        ground.setColor("Ambient", WHITE);
        ground.setColor("Specular", ColorRGBA.Black);
        Material water = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        water.setBoolean("VertexColor", true);
        water.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        water.getAdditionalRenderState().setDepthWrite(false);
        Node terrainNode = new Node("terrain");
        Node waterNode = new Node("water");
        for (int cj = 0; cj < chunksPerSide; cj++) {
            for (int ci = 0; ci < chunksPerSide; ci++) {
                Geometry chunk = new Geometry("ground-" + ci + "-" + cj,
                        TerrainMeshes.ground(terrain, colors, ci * CHUNK_CELLS, cj * CHUNK_CELLS, CHUNK_CELLS));
                chunk.setMaterial(ground);
                terrainNode.attachChild(chunk);
                Mesh waterMesh = TerrainMeshes.water(terrain, world.water(), ci * CHUNK_CELLS, cj * CHUNK_CELLS,
                        CHUNK_CELLS);
                if (waterMesh != null) {
                    Geometry surface = new Geometry("water-" + ci + "-" + cj, waterMesh);
                    surface.setMaterial(water);
                    surface.setQueueBucket(RenderQueue.Bucket.Transparent);
                    waterNode.attachChild(surface);
                }
            }
        }
        root.attachChild(terrainNode);
        root.attachChild(waterNode);
    }

    private void buildProps(WorldData world, PropCatalog catalog) {
        Map<Integer, List<Placement>> large = new HashMap<>();
        Map<Integer, List<Placement>> small = new HashMap<>();
        for (Placement p : world.placements()) {
            int ci = Math.min(chunksPerSide - 1, (int) (p.x() / chunkSize));
            int cj = Math.min(chunksPerSide - 1, (int) (p.z() / chunkSize));
            Map<Integer, List<Placement>> target = isSmall(p.kind()) ? small : large;
            target.computeIfAbsent(cj * chunksPerSide + ci, k -> new ArrayList<>()).add(p);
        }
        for (Map.Entry<Integer, List<Placement>> entry : large.entrySet()) {
            largeProps[entry.getKey()] = instanced("props-large-" + entry.getKey(), entry.getValue(), catalog);
        }
        for (Map.Entry<Integer, List<Placement>> entry : small.entrySet()) {
            smallProps[entry.getKey()] = instanced("props-small-" + entry.getKey(), entry.getValue(), catalog);
        }
    }

    private static boolean isSmall(PropKind kind) {
        return kind == PropKind.BUSH || kind == PropKind.CLUTTER;
    }

    private static InstancedNode instanced(String name, List<Placement> placements, PropCatalog catalog) {
        InstancedNode node = new InstancedNode(name);
        for (Placement p : placements) {
            PropModel model = catalog.model(p.kind(), p.variant());
            Spatial instance = model.template().clone(false);
            instance.setLocalTranslation(p.x(), p.y(), p.z());
            instance.setLocalRotation(new Quaternion().fromAngleAxis(p.yaw(), Vector3f.UNIT_Y));
            instance.setLocalScale(p.scale() * model.baseScale());
            node.attachChild(instance);
        }
        node.instance();
        return node;
    }
}
