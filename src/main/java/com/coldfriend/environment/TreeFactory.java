package com.coldfriend.environment;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.util.Random;

/** Creates crooked, low-poly winter conifers from a shared random source. */
final class TreeFactory {

    private static final int CROWN_SIDES = 6;
    private static final int CROWN_TIERS = 3;
    private static final float TRUNK_RADIUS = 0.12f;
    private static final float TRUNK_HEIGHT = 1.8f;
    private static final float MIN_SCALE = 0.8f;
    private static final float SCALE_RANGE = 0.8f;
    private static final float MAX_LEAN = 0.12f;
    private static final float CROWN_JITTER = 0.25f;
    private static final float TIER_RADIUS = 1.6f;
    private static final float TIER_HEIGHT = 2.4f;
    private static final float TIER_SHRINK = 0.72f;
    private static final float TIER_LIFT = 1.3f;

    private final Material trunkMaterial;
    private final Material crownMaterial;
    private final Random random;

    TreeFactory(Material trunkMaterial, Material crownMaterial, Random random) {
        this.trunkMaterial = trunkMaterial;
        this.crownMaterial = crownMaterial;
        this.random = random;
    }

    /**
     * Creates one tree with a random scale, a slight lean, and tiered crowns.
     *
     * @param x world x position
     * @param z world z position
     * @return a node with its base on y = 0
     */
    Node createTree(float x, float z) {
        Node tree = new Node("tree");
        float scale = MIN_SCALE + random.nextFloat() * SCALE_RANGE;

        Geometry trunk = new Geometry("trunk", new Box(TRUNK_RADIUS, TRUNK_HEIGHT / 2f, TRUNK_RADIUS));
        trunk.setMaterial(trunkMaterial);
        trunk.setLocalTranslation(0f, TRUNK_HEIGHT / 2f, 0f);
        tree.attachChild(trunk);

        float tierRadius = TIER_RADIUS;
        float tierY = TRUNK_HEIGHT * 0.6f;
        for (int tier = 0; tier < CROWN_TIERS; tier++) {
            Geometry crown = new Geometry("crown",
                    LowPolyMeshes.cone(CROWN_SIDES, tierRadius, TIER_HEIGHT, CROWN_JITTER, random));
            crown.setMaterial(crownMaterial);
            crown.setLocalTranslation(0f, tierY, 0f);
            tree.attachChild(crown);
            tierRadius *= TIER_SHRINK;
            tierY += TIER_LIFT;
        }

        tree.setLocalScale(scale);
        tree.setLocalTranslation(x, 0f, z);
        Quaternion lean = new Quaternion().fromAngles(
                (random.nextFloat() * 2f - 1f) * MAX_LEAN,
                random.nextFloat() * FastMath.TWO_PI,
                (random.nextFloat() * 2f - 1f) * MAX_LEAN);
        tree.setLocalRotation(lean);
        return tree;
    }
}
