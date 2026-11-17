package com.coldfriend.world.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldPath.PathPoint;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PathFinderTest {

    private static WorldData world;
    private static PathFinder finder;
    private static float fromX;
    private static float fromZ;
    private static float toX;
    private static float toZ;

    @BeforeAll
    static void setUp() {
        world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
        finder = new PathFinder(world.terrain(), world.water(), 4f, world.footprints());
        Footprint cabin = world.cabin().footprint();
        fromX = world.cabin().doorX();
        fromZ = world.cabin().doorZ();
        // A walkable goal about 40 m from the door: the first direction that gives a route.
        for (int degrees = 0; degrees < 360; degrees += 10) {
            double a = Math.toRadians(degrees);
            float x = fromX + (float) (Math.sin(a) * 40);
            float z = fromZ + (float) (Math.cos(a) * 40);
            if (!cabin.contains(x, z, 2f) && !finder.find(fromX, fromZ, x, z, 4000).isEmpty()) {
                toX = x;
                toZ = z;
                return;
            }
        }
        throw new IllegalStateException("no route from the cabin door in the test world");
    }

    @Test
    void aRouteRunsFromStartToGoalAndStaysOffTheBuildings() {
        List<PathPoint> route = finder.find(fromX, fromZ, toX, toZ, 4000);
        assertFalse(route.isEmpty());
        assertEquals(fromX, route.get(0).x(), 0f);
        assertEquals(toX, route.get(route.size() - 1).x(), 0f);
        for (PathPoint point : route) {
            for (Footprint building : world.footprints()) {
                assertFalse(building.contains(point.x(), point.z(), 0f), "route crosses a building at " + point);
            }
        }
    }

    @Test
    void theSameQuestionGivesTheSameRoute() {
        assertEquals(finder.find(fromX, fromZ, toX, toZ, 4000), finder.find(fromX, fromZ, toX, toZ, 4000));
    }

    @Test
    void aSearchThatRunsOutOfBudgetGivesUpWithNoRoute() {
        assertTrue(finder.find(fromX, fromZ, toX, toZ, 3).isEmpty());
    }

    @Test
    void theUnlimitedSearchStillFindsTheRoute() {
        assertFalse(finder.find(fromX, fromZ, toX, toZ).isEmpty());
    }
}
