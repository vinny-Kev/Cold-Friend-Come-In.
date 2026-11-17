package com.coldfriend.world.gen;

import com.coldfriend.world.query.PathFinder;
import com.coldfriend.world.model.Cabin;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.LocationKind;
import com.coldfriend.world.model.PathKind;
import com.coldfriend.world.model.RegionMap;
import com.coldfriend.world.model.River;
import com.coldfriend.world.model.Village;
import com.coldfriend.world.model.VillageBuilding;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldLocation;
import com.coldfriend.world.model.WorldPath;
import com.coldfriend.world.model.WorldPath.PathPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Places the cabin and the village, joins them with a trail, levels their sites, adds fords where paths cross
 * water, and finds places to collect water. Ranks sites using targets drawn from the seed, so each world's
 * cabin and village sit in different surroundings.
 */
final class StructureStage {

    /** What the stage produced. The terrain was edited in place. */
    record Plan(Cabin cabin, Village village, List<WorldPath> paths, List<WorldLocation> locations,
            Exclusions exclusions) {
    }

    private static final String STAGE = "structures";
    private static final float DENSITY_MIN = 0.15f;
    private static final float DENSITY_MAX = 0.9f;
    private static final float DOORSTEP_DISTANCE = 1.3f;
    private static final float SQUARE_RADIUS = 9f;
    private static final float SQUARE_BLEND = 7f;
    private static final float TRAIL_WIDTH = 2f;
    private static final float VILLAGE_PATH_WIDTH = 2.5f;
    private static final float BUILDING_CLEARANCE = 3f;
    private static final float SQUARE_CLEARANCE = 12f;
    private static final float PATH_CLEARANCE = 1.2f;

    private StructureStage() {
    }

    static Plan generate(HeightField terrain, WaterMap water, RegionMap regions, List<River> rivers,
            WorldGenParameters p, RandomStreams streams) {
        SplittableRandom random = streams.random(STAGE);
        PlacementParameters place = p.placement();
        float targetRiver = Maths.lerp(place.cabinRiverMin(), place.cabinRiverMax(), (float) random.nextDouble());
        float targetDensity = Maths.lerp(DENSITY_MIN, DENSITY_MAX, (float) random.nextDouble());
        float targetVillage = Maths.lerp(place.villageDistanceMin(), place.villageDistanceMax(),
                (float) random.nextDouble());
        RiverProximity proximity = new RiverProximity(rivers);
        PathFinder open = new PathFinder(terrain, water, place.pathGrid(), List.of());

        for (CabinSiting.Site cabinSite : CabinSiting.rank(terrain, water, regions, proximity, p, random,
                targetRiver, targetDensity)) {
            for (VillageSiting.Site villageSite : VillageSiting.rank(terrain, water, proximity, p, random,
                    cabinSite.x(), cabinSite.z(), targetVillage)) {
                float yaw = (float) StrictMath.atan2(villageSite.x() - cabinSite.x(), villageSite.z() - cabinSite.z());
                Footprint cabinFootprint = new Footprint(cabinSite.x(), cabinSite.z(), yaw,
                        place.cabinHalfWidth(), place.cabinHalfDepth());
                float[] door = cabinFootprint.pointInFront(place.cabinHalfDepth() + DOORSTEP_DISTANCE);
                List<PathPoint> trail = open.find(door[0], door[1], villageSite.x(), villageSite.z());
                if (trail.isEmpty()) {
                    continue;
                }
                VillageSiting.Layout layout = VillageSiting.layout(terrain, water, p, random, villageSite, trail);
                if (layout != null) {
                    return build(terrain, water, p, cabinFootprint, door, villageSite, layout, trail);
                }
            }
        }
        throw new IllegalStateException("No valid site for the cabin and village");
    }

    private static Plan build(HeightField terrain, WaterMap water, WorldGenParameters p, Footprint cabinFootprint,
            float[] door, VillageSiting.Site site, VillageSiting.Layout layout, List<PathPoint> trailPoints) {
        PlacementParameters place = p.placement();
        float cabinFloor = TerrainShaping.floorHeight(terrain, cabinFootprint);
        TerrainShaping.levelRect(terrain, cabinFootprint, cabinFloor, place.flattenMargin());
        List<Footprint> footprints = new ArrayList<>();
        footprints.add(cabinFootprint);
        for (VillageBuilding building : layout.buildings()) {
            TerrainShaping.levelRect(terrain, building.footprint(), building.floorY(), place.flattenMargin());
            footprints.add(building.footprint());
        }
        TerrainShaping.levelCircle(terrain, site.x(), site.z(), SQUARE_RADIUS, layout.squareHeight(), SQUARE_BLEND);

        List<WorldPath> paths = new ArrayList<>();
        paths.add(new WorldPath("trail-cabin-village", PathKind.TRAIL, trailPoints, TRAIL_WIDTH));
        PathFinder closed = new PathFinder(terrain, water, place.pathGrid(), footprints);
        for (VillageBuilding building : layout.buildings()) {
            List<PathPoint> route = closed.find(building.doorX(), building.doorZ(), site.x(), site.z());
            if (route.isEmpty()) {
                route = List.of(new PathPoint(building.doorX(), building.doorZ()), new PathPoint(site.x(), site.z()));
            }
            paths.add(new WorldPath("path-" + building.id(), PathKind.VILLAGE_PATH, route, VILLAGE_PATH_WIDTH));
        }
        for (WorldPath path : paths) {
            TerrainShaping.carveFords(terrain, water, path, place.fordDepth());
        }

        List<WorldLocation> locations = new ArrayList<>();
        locations.add(new WorldLocation("cabin-door", LocationKind.CABIN_DOOR, door[0], door[1]));
        locations.addAll(ResourceStage.waterAccess(terrain, water, new PathFinder(terrain, water, place.pathGrid(),
                footprints), door, site));

        Exclusions exclusions = new Exclusions();
        exclusions.addRect(cabinFootprint, BUILDING_CLEARANCE);
        for (VillageBuilding building : layout.buildings()) {
            exclusions.addRect(building.footprint(), BUILDING_CLEARANCE);
        }
        exclusions.addCircle(site.x(), site.z(), SQUARE_CLEARANCE);
        for (WorldPath path : paths) {
            exclusions.addCorridor(path.points(), path.width() / 2f + PATH_CLEARANCE);
        }
        Village village = new Village(site.x(), site.z(), place.villageRadius(), layout.buildings(),
                layout.locations());
        return new Plan(new Cabin(cabinFootprint, cabinFloor, door[0], door[1]), village, paths, locations,
                exclusions);
    }
}
