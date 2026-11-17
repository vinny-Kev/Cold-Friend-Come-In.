package com.coldfriend.world.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of the cabin: walls with a door gap, the door, and the furniture. Positions are given in the cabin's
 * own directions (sideways and forward from its centre, where forward points out through the door), then turned
 * into world footprints. Collision and drawing both use this, so what you see is what you bump into.
 */
public final class CabinLayout {

    /** A box in the cabin's own directions. */
    public record Part(float side, float forward, float halfSide, float halfForward) {
    }

    /**
     * A window in one of the wall pieces returned by {@link #wallParts}.
     *
     * @param wall index of the wall piece: 0 back, 1 left, 2 right, 3 front left, 4 front right
     * @param along metres from the wall piece centre, along the wall (toward the door for side walls)
     * @param halfWidth half the window width
     * @param bottom height of the sill above the floor
     * @param top height of the top of the window above the floor
     */
    public record Window(int wall, float along, float halfWidth, float bottom, float top) {
    }

    /** A piece of furniture. */
    public record Furnishing(String id, Part part, float height) {
    }

    /** Thickness of the floor boards, in metres. */
    public static final float FLOOR_THICKNESS = 0.03f;
    /** Thickness of the walls, in metres. */
    public static final float WALL_THICKNESS = 0.25f;
    /** Height of the walls, in metres. */
    public static final float WALL_HEIGHT = 2.7f;
    /** Width of the doorway, in metres. */
    public static final float DOOR_WIDTH = 1.2f;
    /** Height of the doorway, in metres. */
    public static final float DOOR_HEIGHT = 2.1f;

    private static final float WINDOW_HALF_WIDTH = 0.45f;
    private static final float WINDOW_SILL = 1.0f;
    private static final float WINDOW_TOP = 1.9f;
    private static final float BED_HALF_SIDE = 0.5f;
    private static final float BED_HALF_FORWARD = 1.0f;
    private static final float STOVE_HALF = 0.3f;
    private static final float TABLE_HALF = 0.45f;
    private static final float TABLE_FORWARD = 0.4f;
    private static final float BED_HEIGHT = 0.5f;
    private static final float STOVE_HEIGHT = 1.0f;
    private static final float TABLE_HEIGHT = 0.8f;

    private CabinLayout() {
    }

    /** @return the wall pieces: back, left, right, and the two front pieces either side of the door */
    public static List<Part> wallParts(Footprint f) {
        float hw = f.halfWidth();
        float hd = f.halfDepth();
        float t = WALL_THICKNESS;
        float gap = DOOR_WIDTH / 2f;
        float front = (hw - gap) / 2f;
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(0f, -hd + t / 2f, hw, t / 2f));
        parts.add(new Part(-hw + t / 2f, 0f, t / 2f, hd));
        parts.add(new Part(hw - t / 2f, 0f, t / 2f, hd));
        parts.add(new Part(-(gap + front), hd - t / 2f, front, t / 2f));
        parts.add(new Part(gap + front, hd - t / 2f, front, t / 2f));
        return parts;
    }

    /** @return the cabin windows: two in each side wall, one at the back and one beside the door */
    public static List<Window> windows() {
        return List.of(
                new Window(0, 0f, WINDOW_HALF_WIDTH, WINDOW_SILL, WINDOW_TOP),
                new Window(1, -1.0f, WINDOW_HALF_WIDTH, WINDOW_SILL, WINDOW_TOP),
                new Window(1, 1.0f, WINDOW_HALF_WIDTH, WINDOW_SILL, WINDOW_TOP),
                new Window(2, -1.0f, WINDOW_HALF_WIDTH, WINDOW_SILL, WINDOW_TOP),
                new Window(2, 1.0f, WINDOW_HALF_WIDTH, WINDOW_SILL, WINDOW_TOP),
                new Window(3, -0.3f, WINDOW_HALF_WIDTH, WINDOW_SILL, WINDOW_TOP));
    }

    /** @return the closed door, filling the doorway */
    public static Part doorPart(Footprint f) {
        return new Part(0f, f.halfDepth() - WALL_THICKNESS / 2f, DOOR_WIDTH / 2f, WALL_THICKNESS / 2f);
    }

    /** @return the bed, stove and table */
    public static List<Furnishing> furnishings(Footprint f) {
        float hw = f.halfWidth();
        float hd = f.halfDepth();
        float t = WALL_THICKNESS;
        return List.of(
                new Furnishing("bed", new Part(-hw + t + BED_HALF_SIDE, -hd + t + BED_HALF_FORWARD, BED_HALF_SIDE,
                        BED_HALF_FORWARD), BED_HEIGHT),
                new Furnishing("stove", new Part(hw - t - STOVE_HALF, -hd + t + STOVE_HALF, STOVE_HALF, STOVE_HALF),
                        STOVE_HEIGHT),
                new Furnishing("table", new Part(hw - t - TABLE_HALF - 0.4f, TABLE_FORWARD, TABLE_HALF, TABLE_HALF),
                        TABLE_HEIGHT));
    }

    /**
     * A flat top that things can rest on.
     *
     * @param area where it is, in world space
     * @param top height of its top surface in metres
     */
    public record Surface(Footprint area, float top) {
    }

    /** @return the tops items may be placed on; for now the table */
    public static List<Surface> supportSurfaces(Cabin cabin) {
        List<Surface> surfaces = new ArrayList<>();
        List<Furnishing> items = furnishings(cabin.footprint());
        for (Furnishing furnishing : items) {
            if ("table".equals(furnishing.id())) {
                float[] centre = cabin.footprint().toWorld(furnishing.part().side(), furnishing.part().forward());
                surfaces.add(new Surface(new Footprint(centre[0], centre[1], cabin.footprint().yaw(),
                        furnishing.part().halfSide(), furnishing.part().halfForward()),
                        cabin.floorY() + FLOOR_THICKNESS + furnishing.height()));
            }
        }
        return surfaces;
    }

    /** @return the solid wall footprints in world space */
    public static List<Footprint> walls(Cabin cabin) {
        return toWorld(cabin.footprint(), wallParts(cabin.footprint()));
    }

    /** @return the closed door as a footprint in world space */
    public static Footprint door(Cabin cabin) {
        return toWorld(cabin.footprint(), doorPart(cabin.footprint()));
    }

    /** @return the furniture as footprints in world space */
    public static List<Footprint> furniture(Cabin cabin) {
        List<Footprint> out = new ArrayList<>();
        for (Furnishing furnishing : furnishings(cabin.footprint())) {
            out.add(toWorld(cabin.footprint(), furnishing.part()));
        }
        return out;
    }

    /**
     * @param cabin the cabin
     * @param side metres sideways from the centre
     * @param forward metres toward the door from the centre
     * @return the world x and z of that spot
     */
    public static float[] point(Cabin cabin, float side, float forward) {
        return cabin.footprint().toWorld(side, forward);
    }

    /** @return the world x and z of the middle of the doorway */
    public static float[] doorway(Cabin cabin) {
        return point(cabin, 0f, cabin.footprint().halfDepth());
    }

    private static List<Footprint> toWorld(Footprint f, List<Part> parts) {
        List<Footprint> out = new ArrayList<>();
        for (Part part : parts) {
            out.add(toWorld(f, part));
        }
        return out;
    }

    private static Footprint toWorld(Footprint f, Part part) {
        float[] centre = f.toWorld(part.side(), part.forward());
        return new Footprint(centre[0], centre[1], f.yaw(), part.halfSide(), part.halfForward());
    }
}
