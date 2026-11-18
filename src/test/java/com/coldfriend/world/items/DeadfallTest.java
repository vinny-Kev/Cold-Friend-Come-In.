package com.coldfriend.world.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.TestItems;
import com.coldfriend.player.PlayerId;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.SurvivalActions;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.TerrainRules;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DeadfallTest {

    private static final float FRAME = 0.25f;
    private static WorldData world;
    private static ItemCatalog items;

    @BeforeAll
    static void setUp() throws IOException {
        world = SimFixture.world();
        items = ItemCatalog.load(Path.of("config", "items.properties"));
    }

    private static List<WorldItem> firewoodOutside(WorldSimulation simulation) {
        Footprint cabin = world.cabin().footprint();
        List<WorldItem> found = new ArrayList<>();
        for (WorldItem item : simulation.worldItems()) {
            if (item.stack().item().equals(SurvivalActions.FIREWOOD) && !cabin.contains(item.x(), item.z(), 0f)) {
                found.add(item);
            }
        }
        return found;
    }

    private static void runDays(WorldSimulation simulation, double days) {
        // One in-game day is 1200 real seconds; a 0.25 s frame is five steps.
        int frames = (int) Math.round(days * 1200 / FRAME);
        for (int i = 0; i < frames; i++) {
            simulation.update(FRAME, Map.of());
        }
    }

    @Test
    void aNewWorldHasWoodLyingAroundAndNoneInTheCabinBeyondTheStartingSupply() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        assertEquals(Deadfall.INITIAL_STACKS, firewoodOutside(simulation).size());
    }

    @Test
    void everyStackIsDryWalkableClearAndInTheRing() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        Footprint cabin = world.cabin().footprint();
        List<WorldItem> wood = firewoodOutside(simulation);
        for (WorldItem item : wood) {
            double distance = Math.hypot(item.x() - cabin.x(), item.z() - cabin.z());
            assertTrue(distance >= Deadfall.MIN_DISTANCE - 0.01 && distance <= Deadfall.MAX_DISTANCE + 0.01,
                    "distance " + distance);
            assertTrue(TerrainRules.walkable(world.terrain(), world.water(), item.x(), item.z()));
            assertTrue(world.water().depthAt(world.terrain(), item.x(), item.z()) <= 0.05f);
            int pieces = item.stack().quantity();
            assertTrue(pieces >= Deadfall.MIN_PIECES && pieces <= Deadfall.MAX_PIECES);
        }
        for (int i = 0; i < wood.size(); i++) {
            for (int j = i + 1; j < wood.size(); j++) {
                assertTrue(Math.hypot(wood.get(i).x() - wood.get(j).x(), wood.get(i).z() - wood.get(j).z()) >= 4.0,
                        "stacks too close together");
            }
        }
    }

    @Test
    void theSameSeedGivesTheSameWood() {
        List<WorldItem> a = firewoodOutside(new WorldSimulation(world, items));
        List<WorldItem> b = firewoodOutside(new WorldSimulation(world, items));
        assertEquals(a, b);
    }

    @Test
    void newWoodAppearsEachDayUntilTheCap() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabin"));
        // Start 07:00 day 1; midnight is 17 hours away.
        runDays(simulation, 17.0 / 24.0 + 0.01);
        assertEquals(Math.min(Deadfall.MAX_STACKS, Deadfall.INITIAL_STACKS + Deadfall.DAILY_STACKS),
                firewoodOutside(simulation).size(), "three more after the first midnight, capped at twelve");
        runDays(simulation, 3);
        assertEquals(Deadfall.MAX_STACKS, firewoodOutside(simulation).size(), "never above the cap");
    }

    @Test
    void differentDaysGiveDifferentSpots() {
        WorldSimulation first = new WorldSimulation(world, items);
        List<WorldItem> before = firewoodOutside(first);
        first.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabin"));
        runDays(first, 17.0 / 24.0 + 0.01);
        List<WorldItem> after = firewoodOutside(first);
        assertTrue(after.size() > before.size());
        WorldItem fresh = after.get(after.size() - 1);
        for (WorldItem old : before) {
            assertFalse(old.x() == fresh.x() && old.z() == fresh.z());
        }
    }

    @Test
    void aCatalogueWithoutFirewoodGetsNone() {
        WorldSimulation simulation = new WorldSimulation(world, TestItems.catalog());
        assertTrue(simulation.worldItems().stream()
                .noneMatch(i -> i.stack().item().equals(SurvivalActions.FIREWOOD)));
    }

    @Test
    void woodThePlayerDroppedDoesNotUseUpTheCap() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));
        int firewoodSlot = -1;
        for (int i = 0; i < simulation.inventory(PlayerId.LOCAL).size(); i++) {
            if (simulation.inventory(PlayerId.LOCAL).slot(i).filter(s -> s.item().equals(SurvivalActions.FIREWOOD))
                    .isPresent()) {
                firewoodSlot = i;
            }
        }
        for (int i = 0; i < 3; i++) {
            assertTrue(simulation.dropItem(PlayerId.LOCAL, firewoodSlot, 1).success());
        }
        int before = firewoodOutside(simulation).size();
        assertTrue(before >= Deadfall.INITIAL_STACKS + 3, "three dropped stacks lie outside as well");

        runDays(simulation, 17.0 / 24.0 + 0.01);

        assertEquals(before + Deadfall.DAILY_STACKS - 1, firewoodOutside(simulation).size(),
                "the cap counts only the ten-plus stacks made by the game, so two more fit (ten plus three, capped at twelve)");
    }
}
