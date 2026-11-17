package com.coldfriend.world.render;

import com.coldfriend.world.model.BuildingKind;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.Village;
import com.coldfriend.world.model.VillageBuilding;
import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the village: homes and the general store with roofs, doors, windows and chimneys, and the square with a
 * well and a lamp post. The buildings are empty shells for now; their doors are painted on. Window lights show
 * which homes would be lived in, but nothing in the simulation depends on them.
 */
final class VillageView {

    private static final ColorRGBA HOME_WALL = new ColorRGBA(0.55f, 0.42f, 0.30f, 1f);
    private static final ColorRGBA STORE_WALL = new ColorRGBA(0.50f, 0.25f, 0.20f, 1f);
    private static final ColorRGBA ROOF = new ColorRGBA(0.20f, 0.16f, 0.15f, 1f);
    private static final ColorRGBA DOOR = new ColorRGBA(0.22f, 0.15f, 0.10f, 1f);
    private static final ColorRGBA SIGN = new ColorRGBA(0.78f, 0.68f, 0.45f, 1f);
    private static final ColorRGBA STONE = new ColorRGBA(0.45f, 0.45f, 0.48f, 1f);
    private static final ColorRGBA WATER = new ColorRGBA(0.10f, 0.18f, 0.28f, 1f);
    private static final ColorRGBA POST = new ColorRGBA(0.25f, 0.18f, 0.12f, 1f);
    private static final ColorRGBA WINDOW_LIT = new ColorRGBA(1.0f, 0.74f, 0.4f, 1f);
    private static final ColorRGBA WINDOW_DARK = new ColorRGBA(0.10f, 0.12f, 0.16f, 1f);
    private static final ColorRGBA LAMP_LIGHT = new ColorRGBA(1.0f, 0.70f, 0.38f, 1f);
    private static final ColorRGBA FLOOR = new ColorRGBA(0.34f, 0.25f, 0.18f, 1f);
    private static final float WALL_HALF_THICKNESS = 0.1f;
    private static final float FLOOR_HALF = 0.03f;
    private static final float WINDOW_SILL = 1.0f;
    private static final float WINDOW_TOP = 1.9f;
    private static final float STORE_WINDOW_TOP = 2.2f;
    private static final float HOME_WINDOW_HALF = 0.45f;
    private static final float STORE_WINDOW_HALF = 0.8f;
    private static final float HOME_LIGHT_HEIGHT = 2.0f;
    private static final float HOME_LIGHT_STRENGTH = 1.0f;
    private static final float HOME_LIGHT_RADIUS = 6f;
    private static final float STORE_LIGHT_RADIUS = 8f;
    private static final float HOME_WALL_HEIGHT = 2.5f;
    private static final float STORE_WALL_HEIGHT = 3.0f;
    private static final float ROOF_OVERHANG = 0.35f;
    private static final float FACE = 0.03f;
    private static final float WINDOW_HALF_WIDTH = 0.4f;
    private static final float WINDOW_HALF_HEIGHT = 0.35f;
    private static final float WINDOW_HEIGHT = 1.5f;
    private static final float HOME_DOOR_HALF_WIDTH = 0.45f;
    private static final float STORE_DOOR_HALF_WIDTH = 0.7f;
    private static final float DOOR_HEIGHT = 2.0f;
    private static final float CHIMNEY_HALF = 0.25f;
    private static final float CHIMNEY_RISE = 1.6f;
    private static final float AWNING_REACH = 1.2f;
    private static final float AWNING_HEIGHT = 2.5f;
    private static final float WELL_HALF = 0.8f;
    private static final float WELL_HEIGHT = 0.9f;
    private static final float WELL_OFFSET = 3f;
    private static final float LAMP_HEIGHT = 3.6f;
    private static final float LAMP_OFFSET = -3f;
    private static final float LAMP_RADIUS = 16f;
    private static final float LAMP_STRENGTH = 1.2f;
    private static final float POST_HALF = 0.08f;
    private static final float BULB_HALF = 0.18f;

    private final Node root = new Node("village");
    private final PointLight lamp;
    private final List<PointLight> lights = new ArrayList<>();

    VillageView(Village village, AssetManager assets, float squareHeight) {
        int index = 0;
        for (VillageBuilding building : village.buildings()) {
            if (building.kind() == BuildingKind.GENERAL_STORE) {
                root.attachChild(store(building, assets));
            } else {
                root.attachChild(home(building, assets, index % 2 == 1));
                index++;
            }
        }
        buildSquare(village, assets, squareHeight);
        lamp = new PointLight(new Vector3f(village.x() + LAMP_OFFSET, squareHeight + LAMP_HEIGHT - 0.2f, village.z()),
                LAMP_LIGHT.mult(LAMP_STRENGTH), LAMP_RADIUS);
    }

    Node root() {
        return root;
    }

    void addLightsTo(Node scene) {
        scene.addLight(lamp);
        for (PointLight light : lights) {
            scene.addLight(light);
        }
    }

    private static Node placed(VillageBuilding building) {
        Footprint f = building.footprint();
        Node node = new Node(building.id());
        node.setLocalTranslation(f.x(), building.floorY(), f.z());
        node.setLocalRotation(new Quaternion().fromAngleAxis(f.yaw(), Vector3f.UNIT_Y));
        return node;
    }

    private Node home(VillageBuilding building, AssetManager assets, boolean lived) {
        Footprint f = building.footprint();
        Node node = placed(building);
        List<RenderParts.Opening> front = List.of(
                new RenderParts.Opening(-f.halfWidth() * 0.5f, HOME_WINDOW_HALF, WINDOW_SILL, WINDOW_TOP),
                new RenderParts.Opening(f.halfWidth() * 0.55f, HOME_WINDOW_HALF, WINDOW_SILL, WINDOW_TOP));
        shell(node, assets, f, HOME_WALL_HEIGHT, HOME_WALL, front);
        RenderParts.gabledRoof(node, f.halfWidth(), f.halfDepth(), HOME_WALL_HEIGHT, ROOF_OVERHANG,
                RenderParts.lit(assets, ROOF), RenderParts.lit(assets, HOME_WALL));
        door(node, assets, f, HOME_DOOR_HALF_WIDTH, 0f);
        RenderParts.box(node, "chimney", CHIMNEY_HALF, CHIMNEY_RISE / 2f, CHIMNEY_HALF, f.halfWidth() * 0.6f,
                HOME_WALL_HEIGHT + CHIMNEY_RISE / 2f, -f.halfDepth() * 0.5f, RenderParts.lit(assets, STONE));
        if (lived) {
            float[] inside = f.toWorld(0f, 0f);
            lights.add(new PointLight(new Vector3f(inside[0], building.floorY() + HOME_LIGHT_HEIGHT, inside[1]),
                    LAMP_LIGHT.mult(HOME_LIGHT_STRENGTH), HOME_LIGHT_RADIUS));
        }
        return node;
    }

    private Node store(VillageBuilding building, AssetManager assets) {
        Footprint f = building.footprint();
        Node node = placed(building);
        List<RenderParts.Opening> front = List.of(
                new RenderParts.Opening(-f.halfWidth() * 0.62f, STORE_WINDOW_HALF, WINDOW_SILL, STORE_WINDOW_TOP),
                new RenderParts.Opening(f.halfWidth() * 0.62f, STORE_WINDOW_HALF, WINDOW_SILL, STORE_WINDOW_TOP));
        shell(node, assets, f, STORE_WALL_HEIGHT, STORE_WALL, front);
        RenderParts.gabledRoof(node, f.halfWidth(), f.halfDepth(), STORE_WALL_HEIGHT, ROOF_OVERHANG,
                RenderParts.lit(assets, ROOF), RenderParts.lit(assets, STORE_WALL));
        door(node, assets, f, STORE_DOOR_HALF_WIDTH, 0f);
        RenderParts.box(node, "awning", STORE_DOOR_HALF_WIDTH + 0.8f, 0.05f, AWNING_REACH / 2f, 0f, AWNING_HEIGHT,
                f.halfDepth() + AWNING_REACH / 2f, RenderParts.lit(assets, ROOF));
        RenderParts.box(node, "sign", 1.2f, 0.3f, FACE, 0f, AWNING_HEIGHT + 0.55f, f.halfDepth() + FACE,
                RenderParts.lit(assets, SIGN));
        float[] inside = f.toWorld(0f, 0f);
        lights.add(new PointLight(new Vector3f(inside[0], building.floorY() + HOME_LIGHT_HEIGHT, inside[1]),
                LAMP_LIGHT.mult(HOME_LIGHT_STRENGTH), STORE_LIGHT_RADIUS));
        return node;
    }

    /** A hollow building: four walls (the front one with window openings), a floor, and nothing inside yet. */
    private static void shell(Node node, AssetManager assets, Footprint f, float height, ColorRGBA color,
            List<RenderParts.Opening> frontOpenings) {
        Material wall = RenderParts.lit(assets, color);
        Material frame = RenderParts.lit(assets, DOOR);
        Material glass = RenderParts.glass(assets);
        float t = WALL_HALF_THICKNESS;
        RenderParts.wallWithOpenings(node, "front", 0f, f.halfDepth() - t, false, f.halfWidth(), height, t,
                frontOpenings, wall, frame, glass);
        RenderParts.wallWithOpenings(node, "back", 0f, -f.halfDepth() + t, false, f.halfWidth(), height, t,
                List.of(), wall, frame, glass);
        for (int side = -1; side <= 1; side += 2) {
            RenderParts.wallWithOpenings(node, "side", side * (f.halfWidth() - t), 0f, true, f.halfDepth() - 2f * t,
                    height, t, List.of(), wall, frame, glass);
        }
        RenderParts.box(node, "floor", f.halfWidth() - 2f * t, FLOOR_HALF, f.halfDepth() - 2f * t, 0f, FLOOR_HALF, 0f,
                RenderParts.lit(assets, FLOOR));
    }

    private static void door(Node node, AssetManager assets, Footprint f, float halfWidth, float side) {
        RenderParts.box(node, "door", halfWidth, DOOR_HEIGHT / 2f, FACE, side, DOOR_HEIGHT / 2f,
                f.halfDepth() + FACE, RenderParts.lit(assets, DOOR));
    }

    private void buildSquare(Village village, AssetManager assets, float squareHeight) {
        Node square = new Node("square");
        square.setLocalTranslation(village.x(), squareHeight, village.z());
        RenderParts.box(square, "well", WELL_HALF, WELL_HEIGHT / 2f, WELL_HALF, WELL_OFFSET, WELL_HEIGHT / 2f, 0f,
                RenderParts.lit(assets, STONE));
        RenderParts.box(square, "well-water", WELL_HALF * 0.7f, 0.02f, WELL_HALF * 0.7f, WELL_OFFSET,
                WELL_HEIGHT + 0.01f, 0f, RenderParts.lit(assets, WATER));
        RenderParts.box(square, "lamp-post", POST_HALF, LAMP_HEIGHT / 2f, POST_HALF, LAMP_OFFSET, LAMP_HEIGHT / 2f, 0f,
                RenderParts.lit(assets, POST));
        RenderParts.box(square, "lamp", BULB_HALF, BULB_HALF, BULB_HALF, LAMP_OFFSET, LAMP_HEIGHT, 0f,
                RenderParts.glow(assets, WINDOW_LIT));
        root.attachChild(square);
    }
}
