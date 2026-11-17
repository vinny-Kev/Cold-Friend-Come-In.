package com.coldfriend.world.items;

import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemDefinition;
import com.coldfriend.player.PlayerMovement;
import com.coldfriend.world.items.PlacementRule.Spot;
import com.coldfriend.world.model.Cabin;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.CabinLayout.Surface;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.TerrainRules;
import com.coldfriend.world.query.WorldCollision;
import java.util.List;
import java.util.Optional;

/**
 * Decides whether an item may be dropped or placed at a spot. It holds two lists of small rules, one for putting
 * an item carefully (placing) and one for letting it fall (dropping). The simulation asks again on every real
 * placement, so a preview on screen never decides anything.
 */
public final class PlacementValidator {

    /** Farthest from the player an item can be put down, in metres. */
    public static final float REACH = 4.5f;
    private static final float WORLD_MARGIN = 1f;
    private static final float MIN_RADIUS = 0.1f;
    private static final float WATER_TOLERANCE = 0.05f;
    private static final float LINE_RADIUS = 0.05f;

    private final WorldData world;
    private final WorldCollision collision;
    private final WorldItems items;
    private final ItemCatalog catalog;
    private final List<PlacementRule> placeRules;
    private final List<PlacementRule> dropRules;

    /**
     * @param world the generated world
     * @param collision the world's obstacles
     * @param items the items already in the world
     * @param catalog the item definitions
     */
    public PlacementValidator(WorldData world, WorldCollision collision, WorldItems items, ItemCatalog catalog) {
        this.world = world;
        this.collision = collision;
        this.items = items;
        this.catalog = catalog;
        // Order matters: the first rule that fails gives the reason shown to the player. Overlap comes before the
        // general obstacle check because placed objects are obstacles too, and "another object is there" is clearer.
        this.placeRules = List.of(this::insideWorld, this::withinReach, this::reachable, this::terrainUsable,
                this::notOverlappingObjects, this::notBlocked, this::notOnPlayer);
        this.dropRules = List.of(this::insideWorld, this::reachable, this::terrainUsable, this::notOnPlacedObjects,
                this::notBlocked);
    }

    /**
     * Checks a deliberate placement.
     *
     * @param item what is being placed
     * @param playerX world x of the player
     * @param playerZ world z of the player
     * @param x wanted world x
     * @param z wanted world z
     * @return whether it is allowed, why not, and the height it would rest at
     */
    public PlacementCheck checkPlace(ItemDefinition item, float playerX, float playerZ, float x, float z) {
        if (!item.canPlace()) {
            return PlacementCheck.refused(PlacementProblem.NOT_ALLOWED);
        }
        return run(placeRules, item, playerX, playerZ, x, z);
    }

    /**
     * Checks letting an item fall at a spot.
     *
     * @return whether it is allowed, why not, and the height it would rest at
     */
    public PlacementCheck checkDrop(ItemDefinition item, float playerX, float playerZ, float x, float z) {
        if (!item.canDrop()) {
            return PlacementCheck.refused(PlacementProblem.NOT_ALLOWED);
        }
        return run(dropRules, item, playerX, playerZ, x, z);
    }

    /**
     * @param x world x
     * @param z world z
     * @return the height an item resting at this spot would sit at: a table top, the cabin floor, or the ground
     */
    public float restHeight(float x, float z) {
        for (Surface surface : CabinLayout.supportSurfaces(world.cabin())) {
            if (surface.area().contains(x, z, 0f)) {
                return surface.top();
            }
        }
        Cabin cabin = world.cabin();
        if (cabin.footprint().contains(x, z, -CabinLayout.WALL_THICKNESS)) {
            return cabin.floorY() + CabinLayout.FLOOR_THICKNESS;
        }
        return world.terrain().heightAt(x, z);
    }

    /**
     * @param fromX world x of the player
     * @param fromZ world z of the player
     * @param x world x of the target
     * @param z world z of the target
     * @return true if nothing solid, such as a wall, lies between the player and the target
     */
    public boolean canReach(float fromX, float fromZ, float x, float z) {
        Surface support = supportAt(x, z);
        return collision.lineClear(fromX, fromZ, x, z, LINE_RADIUS, support == null ? null : support.area());
    }

    private PlacementCheck run(List<PlacementRule> rules, ItemDefinition item, float playerX, float playerZ,
            float x, float z) {
        Surface support = supportAt(x, z);
        float restY = restHeight(x, z);
        Spot spot = new Spot(x, z, Math.max(MIN_RADIUS, item.placeRadius()), restY,
                support == null ? null : support.area(), playerX, playerZ);
        for (PlacementRule rule : rules) {
            Optional<PlacementProblem> problem = rule.check(spot);
            if (problem.isPresent()) {
                return PlacementCheck.refused(problem.get());
            }
        }
        return PlacementCheck.allowed(restY, support != null);
    }

    private Surface supportAt(float x, float z) {
        for (Surface surface : CabinLayout.supportSurfaces(world.cabin())) {
            if (surface.area().contains(x, z, 0f)) {
                return surface;
            }
        }
        return null;
    }

    private Optional<PlacementProblem> insideWorld(Spot spot) {
        float size = world.sizeMeters();
        boolean inside = spot.x() >= WORLD_MARGIN && spot.z() >= WORLD_MARGIN && spot.x() <= size - WORLD_MARGIN
                && spot.z() <= size - WORLD_MARGIN;
        return inside ? Optional.empty() : Optional.of(PlacementProblem.OUTSIDE_WORLD);
    }

    private Optional<PlacementProblem> withinReach(Spot spot) {
        float dx = spot.x() - spot.playerX();
        float dz = spot.z() - spot.playerZ();
        return dx * dx + dz * dz <= REACH * REACH ? Optional.empty() : Optional.of(PlacementProblem.TOO_FAR);
    }

    /** The player must be able to get a hand there: no wall or other solid between them and the spot. */
    private Optional<PlacementProblem> reachable(Spot spot) {
        boolean clear = collision.lineClear(spot.playerX(), spot.playerZ(), spot.x(), spot.z(), LINE_RADIUS,
                spot.surface());
        return clear ? Optional.empty() : Optional.of(PlacementProblem.UNREACHABLE);
    }

    /** On the ground the spot must be dry and not too steep. A raised surface is always level. */
    private Optional<PlacementProblem> terrainUsable(Spot spot) {
        if (spot.onSurface()) {
            return Optional.empty();
        }
        if (world.water().depthAt(world.terrain(), spot.x(), spot.z()) > WATER_TOLERANCE) {
            return Optional.of(PlacementProblem.IN_WATER);
        }
        if (world.terrain().slopeAt(spot.x(), spot.z(), 2f) > TerrainRules.MAX_WALK_SLOPE) {
            return Optional.of(PlacementProblem.TOO_STEEP);
        }
        return Optional.empty();
    }

    /** On a raised surface the whole item must sit on it; elsewhere nothing solid may overlap it. */
    private Optional<PlacementProblem> notBlocked(Spot spot) {
        if (spot.onSurface()) {
            boolean fits = spot.surface().contains(spot.x(), spot.z(), -spot.radius());
            return fits ? Optional.empty() : Optional.of(PlacementProblem.BLOCKED);
        }
        return collision.obstructed(spot.x(), spot.z(), spot.radius())
                ? Optional.of(PlacementProblem.BLOCKED) : Optional.empty();
    }

    private Optional<PlacementProblem> notOnPlayer(Spot spot) {
        float dx = spot.x() - spot.playerX();
        float dz = spot.z() - spot.playerZ();
        float limit = PlayerMovement.BODY_RADIUS + spot.radius();
        return dx * dx + dz * dz < limit * limit ? Optional.of(PlacementProblem.ON_PLAYER) : Optional.empty();
    }

    private Optional<PlacementProblem> notOverlappingObjects(Spot spot) {
        return overlapping(spot, false) ? Optional.of(PlacementProblem.OVERLAPS) : Optional.empty();
    }

    private Optional<PlacementProblem> notOnPlacedObjects(Spot spot) {
        return overlapping(spot, true) ? Optional.of(PlacementProblem.OVERLAPS) : Optional.empty();
    }

    private boolean overlapping(Spot spot, boolean placedOnly) {
        for (WorldItem other : items.all()) {
            if (placedOnly && !other.placed()) {
                continue;
            }
            if (Math.abs(other.y() - spot.restY()) > spot.radius() + MIN_RADIUS) {
                continue;
            }
            float radius = Math.max(MIN_RADIUS, catalog.get(other.stack().item()).placeRadius());
            float dx = other.x() - spot.x();
            float dz = other.z() - spot.z();
            float limit = radius + spot.radius();
            if (dx * dx + dz * dz < limit * limit) {
                return true;
            }
        }
        return false;
    }
}
