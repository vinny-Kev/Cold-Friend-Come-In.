package com.coldfriend.world.render;

import com.coldfriend.creature.CreatureStatus;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

/**
 * Development only: a red marker where the creature is. Reads a snapshot of the creature and never changes it. The
 * creature is not drawn as a model yet; this is the stand-in until a creature model is approved.
 */
public final class CreatureDebugView {

    private static final float HALF_WIDTH = 0.4f;
    private static final float HALF_HEIGHT = 1.0f;

    private final Node root = new Node("creature-debug");
    private final Geometry marker;

    public CreatureDebugView(AssetManager assets) {
        Material material = RenderParts.lit(assets, new ColorRGBA(0.9f, 0.1f, 0.1f, 1f));
        marker = RenderParts.box(root, "creature", HALF_WIDTH, HALF_HEIGHT, HALF_WIDTH, 0f, HALF_HEIGHT, 0f, material);
    }

    public Node root() {
        return root;
    }

    /**
     * Moves the marker to the creature.
     *
     * @param status the creature's snapshot
     * @param groundHeight the ground under the creature, in metres
     */
    public void update(CreatureStatus status, float groundHeight) {
        marker.setLocalTranslation(status.x(), groundHeight + HALF_HEIGHT, status.z());
    }
}
