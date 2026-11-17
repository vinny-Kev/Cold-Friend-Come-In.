package com.coldfriend.world.render;

import com.coldfriend.assets.PropModel;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;

/**
 * The ghost shown while the player lines up a placement: the item's model on a flat ring that is green where the
 * item may go and red where it may not. This is presentation only. The simulation decides, again, when the player
 * confirms.
 */
public final class PlacementPreview {

    private static final ColorRGBA VALID = new ColorRGBA(0.2f, 1f, 0.3f, 0.5f);
    private static final ColorRGBA INVALID = new ColorRGBA(1f, 0.2f, 0.2f, 0.5f);
    private static final int RING_SAMPLES = 24;
    private static final float RING_THICKNESS = 0.02f;
    private static final float RING_LIFT = 0.03f;
    private static final float MIN_RING_RADIUS = 0.15f;

    private final Node root = new Node("placement-preview");
    private final Node modelHolder = new Node("preview-model");
    private final Material valid;
    private final Material invalid;
    private Geometry ring;

    /** @param assets the engine asset manager */
    public PlacementPreview(AssetManager assets) {
        valid = ringMaterial(assets, VALID);
        invalid = ringMaterial(assets, INVALID);
        root.attachChild(modelHolder);
        root.setCullHint(Spatial.CullHint.Always);
    }

    /** @return the node to attach to the scene while previewing */
    public Node root() {
        return root;
    }

    /**
     * Starts previewing an item.
     *
     * @param model the item's model
     * @param radius the room the item needs, in metres
     */
    public void show(PropModel model, float radius) {
        modelHolder.detachAllChildren();
        Spatial copy = model.template().clone(false);
        copy.setLocalScale(model.baseScale());
        modelHolder.attachChild(copy);
        if (ring != null) {
            ring.removeFromParent();
        }
        ring = new Geometry("preview-ring", new Cylinder(2, RING_SAMPLES, Math.max(MIN_RING_RADIUS, radius),
                RING_THICKNESS, true));
        ring.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        ring.setLocalTranslation(0f, RING_LIFT, 0f);
        ring.setQueueBucket(Bucket.Transparent);
        ring.setMaterial(invalid);
        root.attachChild(ring);
        root.setCullHint(Spatial.CullHint.Dynamic);
    }

    /** Hides the preview. */
    public void hide() {
        root.setCullHint(Spatial.CullHint.Always);
    }

    /**
     * Moves the preview.
     *
     * @param x world x
     * @param y height of the item's base
     * @param z world z
     * @param yaw rotation in radians
     * @param isValid whether the simulation would allow the item there
     */
    public void update(float x, float y, float z, float yaw, boolean isValid) {
        root.setLocalTranslation(x, y, z);
        modelHolder.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));
        if (ring != null) {
            ring.setMaterial(isValid ? valid : invalid);
        }
    }

    private static Material ringMaterial(AssetManager assets, ColorRGBA color) {
        Material material = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.getAdditionalRenderState().setDepthWrite(false);
        return material;
    }
}
