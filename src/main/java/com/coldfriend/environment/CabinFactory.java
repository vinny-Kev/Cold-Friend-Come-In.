package com.coldfriend.environment;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/** Builds a placeholder log cabin: walls, gable roof, chimney, door, and a glowing window. */
final class CabinFactory {

    private static final float HALF_WIDTH = 3f;
    private static final float HALF_DEPTH = 2.4f;
    private static final float WALL_HEIGHT = 2.8f;
    private static final float ROOF_PITCH_DEG = 25f;
    private static final float ROOF_HALF_LENGTH = 1.45f;
    private static final float ROOF_HALF_WIDTH = 3.2f;
    private static final float ROOF_THICKNESS = 0.12f;
    private static final float ROOF_MIDPOINT_Y = 3.4f;
    private static final float ROOF_MIDPOINT_Z = 1.3f;
    private static final float WINDOW_Y = 1.4f;
    private static final float WINDOW_HALF_WIDTH = 0.7f;
    private static final float WINDOW_HALF_HEIGHT = 0.45f;
    private static final float FACE_OFFSET = 0.02f;
    private static final float DOOR_X = 1.5f;
    private static final float DOOR_HALF_WIDTH = 0.6f;
    private static final float DOOR_HALF_HEIGHT = 1.0f;
    private static final float CHIMNEY_X = 2.0f;
    private static final float CHIMNEY_Y = 3.6f;
    private static final float CHIMNEY_Z = -0.8f;
    private static final float CHIMNEY_HALF = 0.35f;
    private static final float CHIMNEY_HALF_HEIGHT = 1.0f;

    private final Material logMaterial;
    private final Material roofMaterial;
    private final Material doorMaterial;
    private final Material windowMaterial;

    CabinFactory(Material logMaterial, Material roofMaterial, Material doorMaterial, Material windowMaterial) {
        this.logMaterial = logMaterial;
        this.roofMaterial = roofMaterial;
        this.doorMaterial = doorMaterial;
        this.windowMaterial = windowMaterial;
    }

    /**
     * Creates the cabin with its base on y = 0, centred on the origin in x and z, facing +z.
     *
     * @return the cabin node
     */
    Node createCabin() {
        Node cabin = new Node("cabin");
        cabin.attachChild(part("walls", new Box(HALF_WIDTH, WALL_HEIGHT / 2f, HALF_DEPTH),
                logMaterial, 0f, WALL_HEIGHT / 2f, 0f));

        float frontZ = HALF_DEPTH + FACE_OFFSET;
        cabin.attachChild(part("window", new Box(WINDOW_HALF_WIDTH, WINDOW_HALF_HEIGHT, FACE_OFFSET),
                windowMaterial, 0f, WINDOW_Y, frontZ));
        cabin.attachChild(part("door", new Box(DOOR_HALF_WIDTH, DOOR_HALF_HEIGHT, FACE_OFFSET),
                doorMaterial, DOOR_X, DOOR_HALF_HEIGHT, frontZ));
        cabin.attachChild(part("chimney", new Box(CHIMNEY_HALF, CHIMNEY_HALF_HEIGHT, CHIMNEY_HALF),
                logMaterial, CHIMNEY_X, CHIMNEY_Y, CHIMNEY_Z));

        float angle = ROOF_PITCH_DEG * FastMath.DEG_TO_RAD;
        cabin.attachChild(roofSlope("roof-front", angle, ROOF_MIDPOINT_Z));
        cabin.attachChild(roofSlope("roof-back", -angle, -ROOF_MIDPOINT_Z));
        return cabin;
    }

    private Geometry roofSlope(String name, float pitch, float z) {
        Geometry slope = part(name, new Box(ROOF_HALF_WIDTH, ROOF_THICKNESS, ROOF_HALF_LENGTH),
                roofMaterial, 0f, ROOF_MIDPOINT_Y, z);
        slope.setLocalRotation(new Quaternion().fromAngleAxis(pitch, Vector3f.UNIT_X));
        return slope;
    }

    private static Geometry part(String name, Box shape, Material material, float x, float y, float z) {
        Geometry geometry = new Geometry(name, shape);
        geometry.setMaterial(material);
        geometry.setLocalTranslation(x, y, z);
        return geometry;
    }
}
