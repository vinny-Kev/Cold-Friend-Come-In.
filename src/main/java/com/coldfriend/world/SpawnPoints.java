package com.coldfriend.world;

import com.coldfriend.world.model.BuildingKind;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.LocationKind;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldLocation;

/** Places a player at a named spot in the world. The normal spot is inside the cabin; the others help with testing. */
public final class SpawnPoints {

    /** Where a player appears and which way they face. */
    public record Spawn(float x, float z, float yaw) {
    }

    private static final float VILLAGE_OFFSET = 14f;
    private static final float WINDOW_VIEW_DISTANCE = 2.2f;
    private static final float TABLE_STAND_SIDE = 0.9f;
    private static final float TABLE_STAND_FORWARD = 0.4f;
    private static final float HOME_VIEW_DISTANCE = 5f;
    private static final float INSIDE_FORWARD = -0.8f;
    private static final float CABIN_VIEW_DISTANCE = 11f;

    private SpawnPoints() {
    }

    /**
     * @param world the generated world
     * @param name {@code cabin} (the default, inside facing the door), {@code cabindoor} (just outside the door), {@code village} (beside the square, facing it) {@code river}
     *        (at the nearest place to collect water, facing the water) or {@code cabinview} (in front of the
     *        cabin, facing it), {@code windowoutside}, {@code windowinside}, {@code villagehome} (looking at windows) or
     *        {@code cabintable} (beside the cabin table)
     * @return the spawn point
     */
    public static Spawn at(WorldData world, String name) {
        return switch (name == null ? "cabin" : name) {
            case "village" -> village(world);
            case "river" -> river(world);
            case "cabinview" -> cabinView(world);
            case "windowoutside" -> windowOutside(world);
            case "cabintable" -> cabinTable(world);
            case "windowinside" -> windowInside(world);
            case "villagehome" -> villageHome(world);
            case "cabindoor" -> new Spawn(world.cabin().doorX(), world.cabin().doorZ(),
                    world.cabin().footprint().yaw());
            default -> inside(world);
        };
    }

    /** Inside the cabin, facing the door. */
    private static Spawn inside(WorldData world) {
        float[] spot = CabinLayout.point(world.cabin(), 0f, INSIDE_FORWARD);
        return new Spawn(spot[0], spot[1], world.cabin().footprint().yaw());
    }

    /** Inside the cabin beside the table, facing it. */
    private static Spawn cabinTable(WorldData world) {
        var footprint = world.cabin().footprint();
        float[] spot = footprint.toWorld(TABLE_STAND_SIDE, TABLE_STAND_FORWARD);
        return new Spawn(spot[0], spot[1], footprint.yaw() + (float) (StrictMath.PI / 2.0));
    }

    /** Outside the cabin beside a window in its left wall, facing the window. */
    private static Spawn windowOutside(WorldData world) {
        var footprint = world.cabin().footprint();
        float[] spot = footprint.toWorld(-footprint.halfWidth() - WINDOW_VIEW_DISTANCE, 1.0f);
        return new Spawn(spot[0], spot[1], footprint.yaw() + (float) (StrictMath.PI / 2.0));
    }

    /** Inside the cabin facing a window in its right wall. */
    private static Spawn windowInside(WorldData world) {
        var footprint = world.cabin().footprint();
        float[] spot = footprint.toWorld(0.4f, 1.0f);
        return new Spawn(spot[0], spot[1], footprint.yaw() + (float) (StrictMath.PI / 2.0));
    }

    /** In the village, in front of a home, facing its front windows. */
    private static Spawn villageHome(WorldData world) {
        for (var building : world.village().buildings()) {
            if (building.kind() == BuildingKind.HOME) {
                var f = building.footprint();
                float[] spot = f.pointInFront(f.halfDepth() + HOME_VIEW_DISTANCE);
                return new Spawn(spot[0], spot[1], f.yaw() + (float) StrictMath.PI);
            }
        }
        return village(world);
    }

    private static Spawn cabinView(WorldData world) {
        var footprint = world.cabin().footprint();
        float[] spot = footprint.pointInFront(footprint.halfDepth() + CABIN_VIEW_DISTANCE);
        return new Spawn(spot[0], spot[1], footprint.yaw() + (float) StrictMath.PI);
    }

    private static Spawn village(WorldData world) {
        float cabinX = world.cabin().footprint().x();
        float cabinZ = world.cabin().footprint().z();
        float dx = world.village().x() - cabinX;
        float dz = world.village().z() - cabinZ;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) StrictMath.atan2(dx, dz);
        return new Spawn(world.village().x() - dx / length * VILLAGE_OFFSET,
                world.village().z() - dz / length * VILLAGE_OFFSET, yaw);
    }

    private static Spawn river(WorldData world) {
        for (WorldLocation location : world.locations()) {
            if (location.kind() == LocationKind.WATER_ACCESS) {
                float yaw = 0f;
                float best = Float.MAX_VALUE;
                for (int k = 0; k < 16; k++) {
                    float angle = (float) (k * StrictMath.PI / 8.0);
                    float distance = world.water().distanceAt(location.x() + (float) StrictMath.sin(angle) * 4f,
                            location.z() + (float) StrictMath.cos(angle) * 4f);
                    if (distance < best) {
                        best = distance;
                        yaw = angle;
                    }
                }
                return new Spawn(location.x(), location.z(), yaw);
            }
        }
        return at(world, "cabin");
    }
}
