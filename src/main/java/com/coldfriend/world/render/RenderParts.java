package com.coldfriend.world.render;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Small drawing helpers shared by the cabin and village views: lit materials, boxes and triangles. */
final class RenderParts {

    private static final Logger LOG = LoggerFactory.getLogger(RenderParts.class);
    private static final float ROOF_PITCH = 25f * FastMath.DEG_TO_RAD;
    private static final float ROOF_THICKNESS = 0.06f;
    private static final String LIGHTING = "Common/MatDefs/Light/Lighting.j3md";

    private RenderParts() {
    }

    /** @return a plain lit material of one colour */
    static Material lit(AssetManager assets, ColorRGBA color) {
        Material material = new Material(assets, LIGHTING);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color);
        material.setColor("Specular", ColorRGBA.Black);
        return material;
    }

    /** A rectangular opening in a wall, measured along the wall from its centre and up from the floor. */
    record Opening(float centre, float halfWidth, float bottom, float top) {
    }

    private static final ColorRGBA GLASS_TINT = new ColorRGBA(0.55f, 0.75f, 0.85f, 0.2f);
    private static final float GLASS_SHININESS = 48f;
    private static final float FRAME_BAR = 0.05f;
    private static final float FRAME_PROUD = 0.02f;
    private static final float PANE_GAP = 0.004f;
    private static final float MIN_SEGMENT = 0.001f;
    private static final float QUARTER_TURN = FastMath.HALF_PI;

    /**
     * Clear tinted glass. It blends with what is behind it, does not write depth (so it cannot hide anything
     * behind it, whatever order glass and other things are drawn in) and is lit like everything else, so
     * interior and exterior lights show on it.
     */
    static Material glass(AssetManager assets) {
        Material material = new Material(assets, LIGHTING);
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", GLASS_TINT);
        material.setColor("Ambient", GLASS_TINT);
        material.setColor("Specular", ColorRGBA.White);
        material.setFloat("Shininess", GLASS_SHININESS);
        RenderState state = material.getAdditionalRenderState();
        state.setBlendMode(RenderState.BlendMode.Alpha);
        state.setDepthWrite(false);
        state.setFaceCullMode(RenderState.FaceCullMode.Back);
        return material;
    }

    /**
     * Adds a wall with real openings. The wall runs along its own x axis and is as thick as given. Solid wall
     * pieces fill the space around each opening, an opaque frame surrounds it, and two glass sheets facing
     * opposite ways close it, so the glass shows from both sides.
     *
     * @param parent node to add the wall to
     * @param name base name for the parts
     * @param x centre of the wall in the parent coordinates
     * @param z centre of the wall in the parent coordinates
     * @param alongZ true if the wall runs along the parent z axis instead of x
     * @param halfLength half the wall length
     * @param height wall height
     * @param halfThickness half the wall thickness
     * @param openings the openings, with positions along the wall measured from its centre
     * @param wall material for the solid wall
     * @param frame material for window frames
     * @param glass glass material, from {@link #glass}
     */
    static void wallWithOpenings(Node parent, String name, float x, float z, boolean alongZ, float halfLength,
            float height, float halfThickness, List<Opening> openings, Material wall, Material frame,
            Material glass) {
        Node node = new Node(name);
        node.setLocalTranslation(x, 0f, z);
        if (alongZ) {
            node.setLocalRotation(new Quaternion().fromAngleAxis(-QUARTER_TURN, Vector3f.UNIT_Y));
        }
        List<Opening> sorted = new ArrayList<>(openings);
        sorted.sort(Comparator.comparingDouble(Opening::centre));
        float cursor = -halfLength;
        for (Opening o : sorted) {
            float left = o.centre() - o.halfWidth();
            float right = o.centre() + o.halfWidth();
            segment(node, name, cursor, left, 0f, height, halfThickness, wall);
            segment(node, name, left, right, 0f, o.bottom(), halfThickness, wall);
            segment(node, name, left, right, o.top(), height, halfThickness, wall);
            frame(node, o, halfThickness, frame);
            glassSheets(node, o, glass);
            cursor = right;
        }
        segment(node, name, cursor, halfLength, 0f, height, halfThickness, wall);
        parent.attachChild(node);
    }

    private static void segment(Node node, String name, float from, float to, float bottom, float top,
            float halfThickness, Material material) {
        if (to - from < MIN_SEGMENT || top - bottom < MIN_SEGMENT) {
            return;
        }
        box(node, name, (to - from) / 2f, (top - bottom) / 2f, halfThickness, (from + to) / 2f,
                (bottom + top) / 2f, 0f, material);
    }

    private static void frame(Node node, Opening o, float halfThickness, Material material) {
        float depth = halfThickness + FRAME_PROUD;
        float height = o.top() - o.bottom();
        float middle = (o.top() + o.bottom()) / 2f;
        box(node, "frame-top", o.halfWidth(), FRAME_BAR / 2f, depth, o.centre(), o.top() - FRAME_BAR / 2f, 0f,
                material);
        box(node, "frame-bottom", o.halfWidth(), FRAME_BAR / 2f, depth, o.centre(), o.bottom() + FRAME_BAR / 2f, 0f,
                material);
        box(node, "frame-left", FRAME_BAR / 2f, height / 2f, depth, o.centre() - o.halfWidth() + FRAME_BAR / 2f,
                middle, 0f, material);
        box(node, "frame-right", FRAME_BAR / 2f, height / 2f, depth, o.centre() + o.halfWidth() - FRAME_BAR / 2f,
                middle, 0f, material);
        box(node, "frame-mullion", FRAME_BAR / 4f, height / 2f, depth * 0.6f, o.centre(), middle, 0f, material);
    }

    private static void glassSheets(Node node, Opening o, Material glass) {
        float width = o.halfWidth() * 2f - FRAME_BAR;
        float height = o.top() - o.bottom() - FRAME_BAR;
        float middle = (o.top() + o.bottom()) / 2f;
        for (int facing = -1; facing <= 1; facing += 2) {
            Geometry sheet = new Geometry("glass", new Quad(width, height));
            sheet.setMaterial(glass);
            sheet.setQueueBucket(Bucket.Transparent);
            sheet.setLocalTranslation(facing > 0 ? o.centre() - width / 2f : o.centre() + width / 2f,
                    middle - height / 2f, facing * PANE_GAP);
            if (facing < 0) {
                sheet.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y));
            }
            node.attachChild(sheet);
        }
    }

    /** @return a flat bright material that ignores lighting, for glowing windows */
    static Material glow(AssetManager assets, ColorRGBA color) {
        Material material = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        return material;
    }

    /** Adds a box with the given half sizes, centred at a position, to a node. */
    static Geometry box(Node parent, String name, float hx, float hy, float hz, float x, float y, float z,
            Material material) {
        Geometry geometry = new Geometry(name, new Box(hx, hy, hz));
        geometry.setMaterial(material);
        geometry.setLocalTranslation(x, y, z);
        parent.attachChild(geometry);
        return geometry;
    }

    /**
     * Adds a gabled roof whose ridge runs sideways, with a triangular gable closing each end.
     *
     * @param parent node in the building's own directions: x sideways, z forward, y up
     * @param halfWidth half the building's width
     * @param halfDepth half the building's depth
     * @param wallHeight height of the walls, where the roof starts
     * @param overhang how far the roof reaches beyond the walls
     * @param roof material for the roof panels
     * @param gable material for the gable ends
     */
    static void gabledRoof(Node parent, float halfWidth, float halfDepth, float wallHeight, float overhang,
            Material roof, Material gable) {
        float eave = halfDepth + overhang;
        float rise = eave * FastMath.tan(ROOF_PITCH);
        float length = (float) Math.sqrt(eave * eave + rise * rise);
        for (int direction = -1; direction <= 1; direction += 2) {
            Geometry panel = new Geometry("roof", new Box(halfWidth + overhang, ROOF_THICKNESS, length / 2f));
            panel.setMaterial(roof);
            panel.setLocalTranslation(0f, wallHeight + rise / 2f, direction * eave / 2f);
            panel.setLocalRotation(new Quaternion().fromAngleAxis(direction * ROOF_PITCH, Vector3f.UNIT_X));
            parent.attachChild(panel);
        }
        gable.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        for (int direction = -1; direction <= 1; direction += 2) {
            float x = direction * halfWidth;
            Geometry triangle = new Geometry("gable", triangle(new Vector3f(x, wallHeight, -halfDepth),
                    new Vector3f(x, wallHeight, halfDepth),
                    new Vector3f(x, wallHeight + rise * halfDepth / eave, 0f)));
            triangle.setMaterial(gable);
            parent.attachChild(triangle);
        }
    }

    /** @return a single triangle mesh with a flat normal */
    static Mesh triangle(Vector3f a, Vector3f b, Vector3f c) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(a, b, c));
        Vector3f normal = b.subtract(a).cross(c.subtract(a)).normalizeLocal();
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normal, normal, normal));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(0, 1, 2));
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

    /**
     * Loads a model, scales it to fit a box and stands it on the floor of a parent node.
     *
     * @param halfX half the model's width, in metres (x)
     * @param height the model's height, in metres
     * @param halfZ half the model's depth, in metres (z)
     * @param x where its centre sits, x
     * @param y where its bottom sits, y
     * @param z where its centre sits, z
     * @return the node holding the model, or null if the model cannot be loaded (the caller draws a fallback)
     */
    static Node fitModel(AssetManager assets, String path, Node parent, float halfX, float height, float halfZ,
            float x, float y, float z) {
        Spatial model;
        try {
            model = assets.loadModel(path);
        } catch (RuntimeException e) {
            LOG.warn("Could not load the model {}; drawing a fallback instead", path, e);
            return null;
        }
        Node holder = new Node(path);
        holder.attachChild(model);
        holder.updateGeometricState();
        BoundingBox bounds = (BoundingBox) holder.getWorldBound();
        Vector3f centre = bounds.getCenter();
        Vector3f extent = bounds.getExtent(null);
        float sx = halfX / extent.x;
        float sy = height / (2f * extent.y);
        float sz = halfZ / extent.z;
        model.setLocalScale(sx, sy, sz);
        model.setLocalTranslation(-centre.x * sx, -(centre.y - extent.y) * sy, -centre.z * sz);
        holder.setLocalTranslation(x, y, z);
        parent.attachChild(holder);
        return holder;
    }
}
