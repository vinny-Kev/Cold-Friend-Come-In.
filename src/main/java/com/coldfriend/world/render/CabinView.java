package com.coldfriend.world.render;

import com.coldfriend.world.model.Cabin;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.CabinLayout.Furnishing;
import com.coldfriend.world.model.CabinLayout.Part;
import com.coldfriend.world.model.Footprint;
import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the cabin from {@link CabinLayout}: log walls, a gabled roof, a door that swings, glowing windows, simple
 * furniture, and the warm lights inside and by the door. The door's open or shut state and the size of the fire
 * come from the simulation; this class only shows them. The room's light comes from the stove, so it fades as the
 * fire burns down and goes dark when the fire is out.
 */
final class CabinView {

    private static final ColorRGBA LOG_COLOR = new ColorRGBA(0.42f, 0.29f, 0.19f, 1f);
    private static final ColorRGBA ROOF_COLOR = new ColorRGBA(0.22f, 0.17f, 0.15f, 1f);
    private static final ColorRGBA FLOOR_COLOR = new ColorRGBA(0.34f, 0.24f, 0.16f, 1f);
    private static final ColorRGBA DOOR_COLOR = new ColorRGBA(0.25f, 0.17f, 0.11f, 1f);
    private static final ColorRGBA BED_COLOR = new ColorRGBA(0.5f, 0.32f, 0.28f, 1f);
    private static final ColorRGBA STOVE_COLOR = new ColorRGBA(0.18f, 0.18f, 0.2f, 1f);
    private static final ColorRGBA TABLE_COLOR = new ColorRGBA(0.45f, 0.33f, 0.22f, 1f);
    private static final ColorRGBA FIRE_LIGHT = new ColorRGBA(1.0f, 0.62f, 0.3f, 1f);
    private static final ColorRGBA LANTERN_LIGHT = new ColorRGBA(1.0f, 0.68f, 0.35f, 1f);
    private static final String TABLE_MODEL = "models/furniture/table.glb";
    private static final float ROOF_PITCH = 25f * FastMath.DEG_TO_RAD;
    private static final float ROOF_OVERHANG = 0.4f;
    private static final float ROOF_THICKNESS = 0.06f;
    private static final float FLOOR_THICKNESS = CabinLayout.FLOOR_THICKNESS;
    private static final float INTERIOR_LIGHT_HEIGHT = 2.0f;
    private static final float INTERIOR_LIGHT_RADIUS = 9f;
    /** Light strength per unit of fire level: a fire at level 0.45 matches the old fixed strength of 1.5. */
    private static final float FIRE_LIGHT_PER_LEVEL = 3.3f;
    private static final ColorRGBA EMBER_GLOW = new ColorRGBA(1.0f, 0.42f, 0.12f, 1f);
    private static final float GLOW_HALF_SIDE = 0.17f;
    private static final float GLOW_HALF_HEIGHT = 0.11f;
    private static final float GLOW_HEIGHT = 0.35f;
    private static final float GLOW_MIN = 0.25f;
    private static final float LANTERN_HEIGHT = 2.4f;
    private static final float LANTERN_DISTANCE = 1.4f;
    private static final float LANTERN_RADIUS = 11f;
    private static final float LANTERN_STRENGTH = 1.1f;
    private static final float DOOR_OPEN_ANGLE = -100f * FastMath.DEG_TO_RAD;
    private static final float DOOR_SPEED = 4f;
    private static final float FLICKER_SLOW = 7f;
    private static final float FLICKER_FAST = 13f;
    private static final float FLICKER_DEPTH = 0.07f;
    private static final float CHIMNEY_RISE = 2.2f;
    private static final float CHIMNEY_HALF = 0.2f;

    private final Node root = new Node("cabin");
    private final Node doorPivot = new Node("cabin-door-pivot");
    private final PointLight interior;
    private final PointLight lantern;
    private final ColorRGBA lightColor = new ColorRGBA();
    private final ColorRGBA glowColor = new ColorRGBA();
    private final Quaternion doorRotation = new Quaternion();
    private Material glowMaterial;
    private Geometry glow;
    private float fireLevel;
    private float doorAngle;
    private float doorTarget;
    private float time;

    CabinView(Cabin cabin, AssetManager assets) {
        Footprint f = cabin.footprint();
        root.setLocalTranslation(f.x(), cabin.floorY(), f.z());
        root.setLocalRotation(new Quaternion().fromAngleAxis(f.yaw(), Vector3f.UNIT_Y));
        Material wood = RenderParts.lit(assets, LOG_COLOR);
        Material frame = RenderParts.lit(assets, DOOR_COLOR);
        Material glass = RenderParts.glass(assets);
        List<Part> walls = CabinLayout.wallParts(f);
        for (int index = 0; index < walls.size(); index++) {
            Part wall = walls.get(index);
            boolean alongZ = wall.halfForward() > wall.halfSide();
            RenderParts.wallWithOpenings(root, "wall", wall.side(), wall.forward(), alongZ,
                    alongZ ? wall.halfForward() : wall.halfSide(), CabinLayout.WALL_HEIGHT,
                    alongZ ? wall.halfSide() : wall.halfForward(), openingsFor(index), wood, frame, glass);
        }
        Part gap = CabinLayout.doorPart(f);
        float lintelHeight = CabinLayout.WALL_HEIGHT - CabinLayout.DOOR_HEIGHT;
        RenderParts.box(root, "lintel", gap.halfSide(), lintelHeight / 2f, gap.halfForward(), gap.side(),
                CabinLayout.DOOR_HEIGHT + lintelHeight / 2f, gap.forward(), wood);
        RenderParts.box(root, "floor", f.halfWidth() - CabinLayout.WALL_THICKNESS, FLOOR_THICKNESS / 2f,
                f.halfDepth() - CabinLayout.WALL_THICKNESS, 0f, FLOOR_THICKNESS / 2f, 0f, RenderParts.lit(assets, FLOOR_COLOR));
        buildRoof(f, assets);
        buildDoor(gap, assets);
        buildFurniture(f, assets);

        float[] centre = CabinLayout.point(cabin, 0f, -0.4f);
        interior = new PointLight(new Vector3f(centre[0], cabin.floorY() + INTERIOR_LIGHT_HEIGHT, centre[1]),
                ColorRGBA.Black.clone(), INTERIOR_LIGHT_RADIUS);
        float[] porch = CabinLayout.point(cabin, 1.0f, f.halfDepth() + LANTERN_DISTANCE);
        lantern = new PointLight(new Vector3f(porch[0], cabin.floorY() + LANTERN_HEIGHT, porch[1]),
                LANTERN_LIGHT.mult(LANTERN_STRENGTH), LANTERN_RADIUS);
    }

    Node root() {
        return root;
    }

    /** Adds the cabin's lights to the scene node that should be lit by them. */
    void addLightsTo(Node scene) {
        scene.addLight(interior);
        scene.addLight(lantern);
    }

    void setDoorOpen(boolean open) {
        doorTarget = open ? DOOR_OPEN_ANGLE : 0f;
    }

    /** @param level the stove fire's level from the simulation; 0 when it is not lit */
    void setFireLevel(float level) {
        fireLevel = Math.max(0f, level);
    }

    /** Swings the door toward its target and flickers the lights. */
    void update(float tpf) {
        time += tpf;
        float step = DOOR_SPEED * tpf;
        doorAngle += Math.max(-step, Math.min(step, doorTarget - doorAngle));
        doorPivot.setLocalRotation(doorRotation.fromAngleAxis(doorAngle, Vector3f.UNIT_Y));
        float flicker = 1f + FLICKER_DEPTH * FastMath.sin(time * FLICKER_SLOW) + FLICKER_DEPTH * 0.6f
                * FastMath.sin(time * FLICKER_FAST);
        interior.setColor(lightColor.set(FIRE_LIGHT).multLocal(fireLevel * FIRE_LIGHT_PER_LEVEL * flicker));
        if (glow != null) {
            glow.setCullHint(fireLevel > 0f ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
            float brightness = Math.min(1f, GLOW_MIN + fireLevel) * flicker;
            glowMaterial.setColor("Color", glowColor.set(EMBER_GLOW).multLocal(brightness));
        }
    }

    private void buildRoof(Footprint f, AssetManager assets) {
        RenderParts.gabledRoof(root, f.halfWidth() - CabinLayout.WALL_THICKNESS / 2f, f.halfDepth(),
                CabinLayout.WALL_HEIGHT, ROOF_OVERHANG + CabinLayout.WALL_THICKNESS / 2f,
                RenderParts.lit(assets, ROOF_COLOR), RenderParts.lit(assets, LOG_COLOR));
    }

    private void buildDoor(Part gap, AssetManager assets) {
        doorPivot.setLocalTranslation(gap.side() - gap.halfSide(), 0f, gap.forward());
        RenderParts.box(doorPivot, "door", gap.halfSide(), CabinLayout.DOOR_HEIGHT / 2f, gap.halfForward(), gap.halfSide(),
                CabinLayout.DOOR_HEIGHT / 2f, 0f, RenderParts.lit(assets, DOOR_COLOR));
        root.attachChild(doorPivot);
    }

    private static List<RenderParts.Opening> openingsFor(int wallIndex) {
        List<RenderParts.Opening> openings = new ArrayList<>();
        for (CabinLayout.Window w : CabinLayout.windows()) {
            if (w.wall() == wallIndex) {
                openings.add(new RenderParts.Opening(w.along(), w.halfWidth(), w.bottom(), w.top()));
            }
        }
        return openings;
    }

    private void buildFurniture(Footprint f, AssetManager assets) {
        for (Furnishing item : CabinLayout.furnishings(f)) {
            Part p = item.part();
            if ("table".equals(item.id()) && RenderParts.fitModel(assets, TABLE_MODEL, root, p.halfSide(),
                    item.height(), p.halfForward(), p.side(), FLOOR_THICKNESS, p.forward()) != null) {
                continue;
            }
            ColorRGBA color = switch (item.id()) {
                case "bed" -> BED_COLOR;
                case "stove" -> STOVE_COLOR;
                default -> TABLE_COLOR;
            };
            RenderParts.box(root, item.id(), p.halfSide(), item.height() / 2f, p.halfForward(), p.side(),
                    FLOOR_THICKNESS + item.height() / 2f, p.forward(), RenderParts.lit(assets, color));
            if ("stove".equals(item.id())) {
                RenderParts.box(root, "chimney", CHIMNEY_HALF, CHIMNEY_RISE / 2f, CHIMNEY_HALF, p.side(),
                        item.height() + CHIMNEY_RISE / 2f, p.forward(), RenderParts.lit(assets, STOVE_COLOR));
                // The fire door on the stove's room side, glowing while the fire burns.
                glowMaterial = RenderParts.glow(assets, EMBER_GLOW.clone());
                glow = RenderParts.box(root, "stove-fire", GLOW_HALF_SIDE, GLOW_HALF_HEIGHT, 0.01f, p.side(),
                        FLOOR_THICKNESS + GLOW_HEIGHT, p.forward() + p.halfForward() + 0.012f, glowMaterial);
                glow.setCullHint(Spatial.CullHint.Always);
            }
        }
    }



}
