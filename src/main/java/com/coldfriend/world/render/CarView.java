package com.coldfriend.world.render;

import com.coldfriend.world.ParkedCar;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Draws the parked car from boxes: an old saloon with a dark interior showing through the windows. A placeholder for a
 * modelled car, which needs an approved asset; the shape and position are the same either way.
 */
final class CarView {

    private static final ColorRGBA BODY = new ColorRGBA(0.36f, 0.42f, 0.30f, 1f);
    private static final ColorRGBA ROOF = new ColorRGBA(0.22f, 0.25f, 0.20f, 1f);
    private static final ColorRGBA INTERIOR = new ColorRGBA(0.10f, 0.09f, 0.09f, 1f);
    private static final ColorRGBA SEAT = new ColorRGBA(0.30f, 0.16f, 0.12f, 1f);
    private static final ColorRGBA TYRE = new ColorRGBA(0.05f, 0.05f, 0.05f, 1f);
    private static final float LOWER_TOP = 0.75f;
    private static final float ROOF_TOP = 1.25f;
    private static final float HALF_TRACK = 0.8f;
    private static final float TYRE_RADIUS = 0.33f;
    private static final String MODEL = "models/vehicles/parked_car.glb";

    private final Node root = new Node("parked-car");

    CarView(ParkedCar car, float groundY, AssetManager assets) {
        root.setLocalTranslation(car.x(), groundY, car.z());
        root.setLocalRotation(new Quaternion().fromAngleAxis(car.yaw(), Vector3f.UNIT_Y));
        Material body = RenderParts.lit(assets, BODY);
        Material roof = RenderParts.lit(assets, ROOF);
        Material interior = RenderParts.lit(assets, INTERIOR);
        Material seat = RenderParts.lit(assets, SEAT);
        Material tyre = RenderParts.lit(assets, TYRE);
        float hw = ParkedCar.HALF_WIDTH;
        float hl = ParkedCar.HALF_LENGTH;
        // The model has its own interior. Without it, a boxed body, wheels and seats.
        if (RenderParts.fitModel(assets, MODEL, root, hw, ROOF_TOP, hl, 0f, 0f, 0f) == null) {
            RenderParts.box(root, "body", hw, (LOWER_TOP - 0.3f) / 2f, hl, 0f, 0.3f + (LOWER_TOP - 0.3f) / 2f, 0f, body);
            RenderParts.box(root, "cabin", hw * 0.85f, (ROOF_TOP - LOWER_TOP) / 2f, hl * 0.55f, 0f,
                    LOWER_TOP + (ROOF_TOP - LOWER_TOP) / 2f, -hl * 0.1f, roof);
            RenderParts.box(root, "interior", hw * 0.75f, (ROOF_TOP - LOWER_TOP) / 2f - 0.05f, hl * 0.45f, 0f,
                    LOWER_TOP + (ROOF_TOP - LOWER_TOP) / 2f, -hl * 0.1f, interior);
            RenderParts.box(root, "seat-left", 0.25f, 0.25f, 0.3f, -0.4f, LOWER_TOP + 0.1f, -hl * 0.1f, seat);
            RenderParts.box(root, "seat-right", 0.25f, 0.25f, 0.3f, 0.4f, LOWER_TOP + 0.1f, -hl * 0.1f, seat);
            for (float side : new float[] {-HALF_TRACK, HALF_TRACK}) {
                for (float forward : new float[] {-hl * 0.6f, hl * 0.6f}) {
                    RenderParts.box(root, "tyre", 0.12f, TYRE_RADIUS, TYRE_RADIUS, side, TYRE_RADIUS, forward, tyre);
                }
            }
        }
    }

    Node root() {
        return root;
    }
}
