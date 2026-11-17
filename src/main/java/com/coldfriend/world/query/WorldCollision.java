package com.coldfriend.world.query;

import com.coldfriend.entity.EntityId;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.Placement;
import com.coldfriend.world.model.VillageBuilding;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where a person can stand and how they slide along obstacles. Combines the terrain rules (slope and water depth)
 * with round obstacles for trees and rocks and rectangular ones for buildings, the cabin's walls and furniture, and its door while it is shut. Pure logic with no engine types,
 * so the same code can run on a server later.
 */
public final class WorldCollision {

    private static final float GRID_CELL = 8f;
    private static final int PUSH_ITERATIONS = 3;
    private static final float EPSILON = 0.001f;
    private static final float LINE_STEP = 0.2f;

    private record Circle(float x, float z, float radius) {
    }

    private final HeightField terrain;
    private final WaterMap water;
    private final List<Footprint> solids;
    private final Footprint door;
    private boolean doorClosed = true;
    private final int side;
    private final List<List<Circle>> grid;
    private final Map<EntityId, Circle> placed = new LinkedHashMap<>();

    /**
     * @param world the generated world
     */
    public WorldCollision(WorldData world) {
        this.terrain = world.terrain();
        this.water = world.water();
        List<Footprint> all = new ArrayList<>();
        for (VillageBuilding building : world.village().buildings()) {
            all.add(building.footprint());
        }
        all.addAll(CabinLayout.walls(world.cabin()));
        all.addAll(CabinLayout.furniture(world.cabin()));
        this.solids = List.copyOf(all);
        this.door = CabinLayout.door(world.cabin());
        this.side = (int) Math.ceil(world.sizeMeters() / GRID_CELL) + 1;
        this.grid = new ArrayList<>(side * side);
        for (int k = 0; k < side * side; k++) {
            grid.add(new ArrayList<>());
        }
        for (Placement p : world.placements()) {
            if (p.kind().blocksMovement()) {
                int ci = cell(p.x());
                int cj = cell(p.z());
                grid.get(cj * side + ci).add(new Circle(p.x(), p.z(), p.colliderRadius()));
            }
        }
    }

    /**
     * Moves a person, sliding along obstacles and refusing steep ground and deep water.
     *
     * @param x current world x
     * @param z current world z
     * @param dx wanted movement in x
     * @param dz wanted movement in z
     * @param radius the person's radius in metres
     * @return the new world x and z
     */
    public float[] move(float x, float z, float dx, float dz, float radius) {
        float[] full = settle(x + dx, z + dz, radius);
        if (allowed(full[0], full[1], radius)) {
            return full;
        }
        float[] alongX = settle(x + dx, z, radius);
        if (allowed(alongX[0], alongX[1], radius)) {
            return alongX;
        }
        float[] alongZ = settle(x, z + dz, radius);
        if (allowed(alongZ[0], alongZ[1], radius)) {
            return alongZ;
        }
        return new float[] {x, z};
    }

    /** @return true if a person of this radius can stand at the point */
    public boolean canStandAt(float x, float z, float radius) {
        float[] settled = settle(x, z, radius);
        return Math.abs(settled[0] - x) < EPSILON && Math.abs(settled[1] - z) < EPSILON
                && allowed(x, z, radius);
    }

    /**
     * Makes a placed object solid.
     *
     * @param id the object
     * @param x world x
     * @param z world z
     * @param radius how far from its centre it blocks movement
     */
    public void addObstacle(EntityId id, float x, float z, float radius) {
        placed.put(id, new Circle(x, z, radius));
    }

    /** Makes a placed object passable again, for example when it is picked up. */
    public void removeObstacle(EntityId id) {
        placed.remove(id);
    }

    /**
     * @param x world x
     * @param z world z
     * @param radius radius of the thing being checked
     * @return true if a wall, building, furniture, shut door, tree, rock or placed object overlaps that circle
     */
    public boolean obstructed(float x, float z, float radius) {
        if (overlapsAnything(x, z, radius, null)) {
            return true;
        }
        int ci = cell(x);
        int cj = cell(z);
        for (int j = Math.max(0, cj - 1); j <= Math.min(side - 1, cj + 1); j++) {
            for (int i = Math.max(0, ci - 1); i <= Math.min(side - 1, ci + 1); i++) {
                for (Circle c : grid.get(j * side + i)) {
                    if (touches(c, x, z, radius)) {
                        return true;
                    }
                }
            }
        }
        for (Circle c : placed.values()) {
            if (touches(c, x, z, radius)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks that nothing solid lies on the straight line between two points.
     *
     * @param ignored a solid to leave out of the check, such as the table something is resting on; may be null
     * @return true if the line is clear
     */
    public boolean lineClear(float fromX, float fromZ, float toX, float toZ, float radius, Footprint ignored) {
        float dx = toX - fromX;
        float dz = toZ - fromZ;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(length / LINE_STEP));
        for (int k = 0; k <= steps; k++) {
            float t = (float) k / steps;
            if (overlapsAnything(fromX + dx * t, fromZ + dz * t, radius, ignored)) {
                return false;
            }
        }
        return true;
    }

    private static boolean touches(Circle c, float x, float z, float radius) {
        float ox = x - c.x();
        float oz = z - c.z();
        float limit = c.radius() + radius;
        return ox * ox + oz * oz < limit * limit;
    }

    /** @param closed whether the cabin door is shut; a shut door is solid */
    public void setDoorClosed(boolean closed) {
        this.doorClosed = closed;
    }

    private boolean allowed(float x, float z, float radius) {
        return TerrainRules.walkable(terrain, water, x, z) && !overlapsAnything(x, z, radius, null);
    }

    private boolean overlapsAnything(float x, float z, float radius, Footprint ignored) {
        for (Footprint f : solids) {
            if (!f.equals(ignored) && f.contains(x, z, radius)) {
                return true;
            }
        }
        return doorClosed && door.contains(x, z, radius);
    }

    /** Pushes a position out of any trees and rocks it overlaps. */
    private float[] settle(float x, float z, float radius) {
        float px = x;
        float pz = z;
        for (int pass = 0; pass < PUSH_ITERATIONS; pass++) {
            boolean moved = false;
            int ci = cell(px);
            int cj = cell(pz);
            for (int j = Math.max(0, cj - 1); j <= Math.min(side - 1, cj + 1); j++) {
                for (int i = Math.max(0, ci - 1); i <= Math.min(side - 1, ci + 1); i++) {
                    for (Circle c : grid.get(j * side + i)) {
                        float ox = px - c.x();
                        float oz = pz - c.z();
                        float limit = c.radius() + radius;
                        float distanceSquared = ox * ox + oz * oz;
                        if (distanceSquared < limit * limit) {
                            float distance = (float) Math.sqrt(distanceSquared);
                            if (distance < EPSILON) {
                                px += limit;
                            } else {
                                px = c.x() + ox / distance * limit;
                                pz = c.z() + oz / distance * limit;
                            }
                            moved = true;
                        }
                    }
                }
            }
            for (Circle c : placed.values()) {
                float ox = px - c.x();
                float oz = pz - c.z();
                float limit = c.radius() + radius;
                float distanceSquared = ox * ox + oz * oz;
                if (distanceSquared < limit * limit) {
                    float distance = (float) Math.sqrt(distanceSquared);
                    if (distance < EPSILON) {
                        px += limit;
                    } else {
                        px = c.x() + ox / distance * limit;
                        pz = c.z() + oz / distance * limit;
                    }
                    moved = true;
                }
            }
            if (!moved) {
                break;
            }
        }
        return new float[] {px, pz};
    }

    private int cell(float coordinate) {
        return Math.max(0, Math.min(side - 1, (int) (coordinate / GRID_CELL)));
    }
}
