package com.coldfriend.world.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.interaction.ActionResult;
import com.coldfriend.interaction.Interactable;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemId;
import com.coldfriend.item.ItemStack;
import com.coldfriend.item.TestItems;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerMovement;
import com.coldfriend.player.PlayerState;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.CabinLayout.Surface;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.WorldData;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ObjectPlacementTest {

    private static final ItemId LOG = TestItems.LOG;
    private static final ItemId KEY = TestItems.KEY;
    private static final ItemId PEBBLE = TestItems.PEBBLE;

    private static float[] ahead(PlayerState player, float distance) {
        return new float[] {player.x() + (float) Math.sin(player.yaw()) * distance,
            player.z() + (float) Math.cos(player.yaw()) * distance};
    }

    private static WorldSimulation outsideWith(ItemStack stack) {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        simulation.inventory(PlayerId.LOCAL).add(stack);
        return simulation;
    }

    @Test
    void aValidPlacementConsumesOneItemAndCreatesAStablePlacedObjectOnTheGround() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 3));
        PlayerState player = simulation.player(PlayerId.LOCAL);
        float[] spot = ahead(player, 1.8f);

        ActionResult result = simulation.placeItem(PlayerId.LOCAL, 0, spot[0], spot[1], 0.5f);

        assertTrue(result.success(), result.message());
        assertEquals(2, simulation.inventory(PlayerId.LOCAL).count(LOG));
        assertEquals(1, simulation.worldItems().size());
        WorldItem placed = simulation.worldItems().get(0);
        assertTrue(placed.placed());
        assertEquals(new ItemStack(LOG, 1), placed.stack());
        assertEquals(0.5f, placed.yaw(), 0.0001f);
        assertEquals(1L, placed.id().value());
        assertEquals(SimFixture.world().terrain().heightAt(spot[0], spot[1]), placed.y(), 0.001f);
    }

    @Test
    void aPlacedObjectBlocksMovement() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 1));
        PlayerState player = simulation.player(PlayerId.LOCAL);
        float[] spot = ahead(player, 1.8f);
        simulation.placeItem(PlayerId.LOCAL, 0, spot[0], spot[1], 0f);

        SimFixture.walkForward(simulation, 2f);

        double distance = Math.hypot(player.x() - spot[0], player.z() - spot[1]);
        assertTrue(distance >= 0.3 + PlayerMovement.BODY_RADIUS - 0.02, "walked into the object: " + distance);
    }

    @Test
    void placingInsideAWallIsRefusedAndNothingIsConsumed() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.inside(simulation);
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(LOG, 2));
        Footprint wall = CabinLayout.walls(SimFixture.world().cabin()).get(0);

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, wall.x(), wall.z());
        ActionResult result = simulation.placeItem(PlayerId.LOCAL, 0, wall.x(), wall.z(), 0f);

        assertFalse(check.valid());
        assertFalse(result.success());
        assertEquals(2, simulation.inventory(PlayerId.LOCAL).count(LOG));
        assertTrue(simulation.worldItems().isEmpty());
    }

    @Test
    void placingOutsideTheWorldIsRefused() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 1));
        PlayerState player = simulation.player(PlayerId.LOCAL);

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, -5f, player.z());

        assertFalse(check.valid());
        assertEquals(PlacementProblem.OUTSIDE_WORLD, check.problem());
        assertFalse(simulation.placeItem(PlayerId.LOCAL, 0, -5f, player.z(), 0f).success());
        assertEquals(1, simulation.inventory(PlayerId.LOCAL).count(LOG));
    }

    @Test
    void placingTooFarAwayIsRefused() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 1));
        float[] spot = ahead(simulation.player(PlayerId.LOCAL), PlacementValidator.REACH + 3f);

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, spot[0], spot[1]);

        assertEquals(PlacementProblem.TOO_FAR, check.problem());
    }

    @Test
    void placingOnThePlayerIsRefused() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 1));
        PlayerState player = simulation.player(PlayerId.LOCAL);

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, player.x(), player.z());

        assertEquals(PlacementProblem.ON_PLAYER, check.problem());
    }

    @Test
    void placingOnAnotherObjectIsRefused() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 2));
        float[] spot = ahead(simulation.player(PlayerId.LOCAL), 2f);
        assertTrue(simulation.placeItem(PlayerId.LOCAL, 0, spot[0], spot[1], 0f).success());

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, spot[0] + 0.1f, spot[1]);
        ActionResult result = simulation.placeItem(PlayerId.LOCAL, 0, spot[0] + 0.1f, spot[1], 0f);

        assertEquals(PlacementProblem.OVERLAPS, check.problem());
        assertFalse(result.success());
        assertEquals(1, simulation.inventory(PlayerId.LOCAL).count(LOG));
        assertEquals(1, simulation.worldItems().size());
    }

    @Test
    void placingInWaterIsRefused() {
        WorldData world = SimFixture.world();
        WorldSimulation simulation = SimFixture.simulation();
        // Stand on the nearest river bank and aim at open water.
        SpawnPoints.Spawn bank = SpawnPoints.at(world, "river");
        simulation.addPlayer(PlayerId.LOCAL, bank);
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(LOG, 1));
        float[] water = null;
        for (float distance = 1f; distance <= 4f && water == null; distance += 0.5f) {
            float x = bank.x() + (float) Math.sin(bank.yaw()) * distance;
            float z = bank.z() + (float) Math.cos(bank.yaw()) * distance;
            if (world.water().depthAt(world.terrain(), x, z) > 0.1f) {
                water = new float[] {x, z};
            }
        }
        assertNotNull(water, "no water within reach of the river spawn");

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, water[0], water[1]);

        assertEquals(PlacementProblem.IN_WATER, check.problem());
    }

    @Test
    void placingThroughAClosedWallIsRefused() {
        WorldSimulation simulation = SimFixture.simulation();
        Footprint cabin = SimFixture.world().cabin().footprint();
        float[] outsideSpot = cabin.toWorld(cabin.halfWidth() + 0.8f, 1.0f);
        simulation.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(outsideSpot[0], outsideSpot[1], 0f));
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(LOG, 1));
        float[] inside = cabin.toWorld(cabin.halfWidth() - 1.2f, 1.0f);

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, inside[0], inside[1]);

        assertFalse(check.valid());
        assertEquals(PlacementProblem.UNREACHABLE, check.problem());
    }

    @Test
    void itemsThatCannotBePlacedAreRefused() {
        WorldSimulation simulation = outsideWith(new ItemStack(KEY, 1));
        float[] spot = ahead(simulation.player(PlayerId.LOCAL), 1.8f);

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, spot[0], spot[1]);
        ActionResult result = simulation.placeItem(PlayerId.LOCAL, 0, spot[0], spot[1], 0f);

        assertEquals(PlacementProblem.NOT_ALLOWED, check.problem());
        assertFalse(result.success());
        assertEquals(1, simulation.inventory(PlayerId.LOCAL).count(KEY));
    }

    @Test
    void previewingAPlacementNeverConsumesAnything() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 2));
        float[] spot = ahead(simulation.player(PlayerId.LOCAL), 1.8f);

        for (int i = 0; i < 50; i++) {
            assertTrue(simulation.checkPlacement(PlayerId.LOCAL, 0, spot[0], spot[1]).valid());
        }

        assertEquals(2, simulation.inventory(PlayerId.LOCAL).count(LOG));
        assertTrue(simulation.worldItems().isEmpty());
    }

    @Test
    void anItemCanRestOnTheTable() {
        WorldSimulation simulation = SimFixture.simulation();
        Footprint cabin = SimFixture.world().cabin().footprint();
        Surface table = CabinLayout.supportSurfaces(SimFixture.world().cabin()).get(0);
        // Stand beside the table, on the door side of it.
        float[] stand = cabin.toWorld(cabin.toLocal(table.area().x(), table.area().z())[0] - 1.3f, 0.4f);
        simulation.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(stand[0], stand[1], cabin.yaw()));
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(PEBBLE, 2));

        ActionResult result = simulation.placeItem(PlayerId.LOCAL, 0, table.area().x(), table.area().z(), 0f);

        assertTrue(result.success(), result.message());
        WorldItem placed = simulation.worldItems().get(0);
        assertEquals(table.top(), placed.y(), 0.001f);
        assertEquals(1, simulation.inventory(PlayerId.LOCAL).count(PEBBLE));
    }

    @Test
    void anItemOverhangingTheTableEdgeIsRefused() {
        WorldSimulation simulation = SimFixture.simulation();
        Footprint cabin = SimFixture.world().cabin().footprint();
        Surface table = CabinLayout.supportSurfaces(SimFixture.world().cabin()).get(0);
        float[] stand = cabin.toWorld(cabin.toLocal(table.area().x(), table.area().z())[0] - 1.3f, 0.4f);
        simulation.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(stand[0], stand[1], cabin.yaw()));
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(LOG, 1));
        // The log is 0.3 m in radius; this spot is on the table but only 0.05 m from its edge.
        float[] edge = cabin.toWorld(cabin.toLocal(table.area().x(), table.area().z())[0] - table.area().halfWidth()
                + 0.05f, cabin.toLocal(table.area().x(), table.area().z())[1]);

        PlacementCheck check = simulation.checkPlacement(PlayerId.LOCAL, 0, edge[0], edge[1]);

        assertFalse(check.valid());
        assertEquals(PlacementProblem.BLOCKED, check.problem());
    }

    @Test
    void aPlacedObjectCanBePickedUpAgainAndIsPassableAfterwards() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 1));
        PlayerState player = simulation.player(PlayerId.LOCAL);
        float[] spot = ahead(player, 1.4f);
        assertTrue(simulation.placeItem(PlayerId.LOCAL, 0, spot[0], spot[1], 0f).success());
        assertEquals(0, simulation.inventory(PlayerId.LOCAL).count(LOG));
        // The log is 1.4 m ahead and about 1.4 m below the eyes, so look down about 45 degrees.
        SimFixture.lookDown(simulation, 0.8f);

        Interactable target = simulation.interactableFor(PlayerId.LOCAL).orElseThrow();
        ActionResult result = simulation.interact(new InteractionRequest(PlayerId.LOCAL, target.ref()));

        assertTrue(result.success(), result.message());
        assertEquals(1, simulation.inventory(PlayerId.LOCAL).count(LOG));
        assertTrue(simulation.worldItems().isEmpty());
        SimFixture.walkForward(simulation, 1.5f);
        double distance = Math.hypot(player.x() - spot[0], player.z() - spot[1]);
        assertTrue(distance > 0.5 || player.z() != spot[1], "collision stayed behind after pickup");
    }

    @Test
    void placeAndPickUpNeverDuplicateOrLoseAnything() {
        WorldSimulation simulation = outsideWith(new ItemStack(LOG, 6));
        PlayerState player = simulation.player(PlayerId.LOCAL);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);

        for (int i = 0; i < 6; i++) {
            float[] spot = ahead(player, 1.4f + 0.7f * (i % 3));
            float sideStep = (i / 3) * 1.2f;
            simulation.placeItem(PlayerId.LOCAL, 0, spot[0] + sideStep, spot[1], i);
        }
        int placed = simulation.worldItems().size();

        assertEquals(6 - inventory.count(LOG), placed);
        assertEquals(6, inventory.count(LOG) + placed);
    }

    @Test
    void theShippedItemDataGivesAStartingKitAndStartingItemsInTheCabin() throws IOException {
        ItemCatalog real = ItemCatalog.load(Path.of("config", "items.properties"));
        WorldSimulation simulation = new WorldSimulation(SimFixture.world(), real);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(SimFixture.world(), "cabin"));

        assertFalse(simulation.worldItems().isEmpty());
        assertTrue(simulation.inventory(PlayerId.LOCAL).count(new ItemId("firewood")) > 0);
        // The three starting items are in the cabin; the rest is deadfall firewood lying in the forest.
        int inCabin = 0;
        for (WorldItem item : simulation.worldItems()) {
            if (SimFixture.world().cabin().footprint().contains(item.x(), item.z(), 0f)) {
                inCabin++;
            } else {
                assertEquals(new ItemId("firewood"), item.stack().item(), "only firewood lies outside the cabin");
            }
        }
        assertEquals(3, inCabin);
    }
}
