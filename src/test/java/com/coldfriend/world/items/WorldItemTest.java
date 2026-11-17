package com.coldfriend.world.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.interaction.ActionResult;
import com.coldfriend.interaction.Interactable;
import com.coldfriend.interaction.InteractableKind;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.inventory.Inventory;
import com.coldfriend.item.ItemId;
import com.coldfriend.item.ItemStack;
import com.coldfriend.item.TestItems;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerState;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.Footprint;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WorldItemTest {

    private static final ItemId LOG = TestItems.LOG;
    private static final ItemId LAMP = TestItems.LAMP;
    private static final ItemId KEY = TestItems.KEY;
    private static final float DOWN = 1.1f;

    private static int totalOf(WorldSimulation simulation, ItemId item) {
        int total = simulation.inventory(PlayerId.LOCAL).count(item);
        for (WorldItem worldItem : simulation.worldItems()) {
            if (worldItem.stack().item().equals(item)) {
                total += worldItem.stack().quantity();
            }
        }
        return total;
    }

    @Test
    void droppingRemovesTheRightQuantityAndSpawnsAStableWorldItemNearThePlayer() {
        WorldSimulation simulation = SimFixture.simulation();
        PlayerState player = SimFixture.outside(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(LOG, 5));

        ActionResult result = simulation.dropItem(PlayerId.LOCAL, 0, 2);

        assertTrue(result.success(), result.message());
        assertEquals(3, inventory.count(LOG));
        List<WorldItem> items = simulation.worldItems();
        assertEquals(1, items.size());
        WorldItem dropped = items.get(0);
        assertEquals(new ItemStack(LOG, 2), dropped.stack());
        assertFalse(dropped.placed());
        assertEquals(1L, dropped.id().value());
        double distance = Math.hypot(dropped.x() - player.x(), dropped.z() - player.z());
        assertTrue(distance > 0.4 && distance < 1.5, "dropped " + distance + " m away");
        assertEquals(SimFixture.world().terrain().heightAt(dropped.x(), dropped.z()), dropped.y(), 0.001f);
    }

    @Test
    void droppingTheWholeStackEmptiesTheSlot() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(LAMP, 1));

        assertTrue(simulation.dropItem(PlayerId.LOCAL, 0, 1).success());

        assertTrue(inventory.slot(0).isEmpty());
        assertEquals(LAMP, simulation.worldItems().get(0).stack().item());
    }

    @Test
    void badDropsChangeNothing() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(LOG, 3));
        inventory.add(new ItemStack(KEY, 1));

        assertFalse(simulation.dropItem(PlayerId.LOCAL, 0, 0).success());
        assertFalse(simulation.dropItem(PlayerId.LOCAL, 0, 4).success());
        assertFalse(simulation.dropItem(PlayerId.LOCAL, 5, 1).success());
        ActionResult key = simulation.dropItem(PlayerId.LOCAL, 1, 1);

        assertFalse(key.success());
        assertTrue(key.message().contains("Key"));
        assertEquals(3, inventory.count(LOG));
        assertEquals(1, inventory.count(KEY));
        assertTrue(simulation.worldItems().isEmpty());
    }

    @Test
    void anItemIsNeverDroppedInsideAWall() {
        WorldSimulation simulation = SimFixture.simulation();
        Footprint cabin = SimFixture.world().cabin().footprint();
        // Stand a short step in front of the back wall, facing it.
        float[] spot = CabinLayout.point(SimFixture.world().cabin(), 0f, -cabin.halfDepth() + 0.7f);
        simulation.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(spot[0], spot[1],
                cabin.yaw() + (float) Math.PI));
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(LOG, 1));

        ActionResult result = simulation.dropItem(PlayerId.LOCAL, 0, 1);

        assertTrue(result.success(), result.message());
        WorldItem dropped = simulation.worldItems().get(0);
        for (Footprint wall : CabinLayout.walls(SimFixture.world().cabin())) {
            assertFalse(wall.contains(dropped.x(), dropped.z(), 0.3f), "dropped into a wall");
        }
        assertTrue(cabin.contains(dropped.x(), dropped.z(), -0.2f), "dropped outside the cabin through the wall");
    }

    @Test
    void pickingUpMovesTheWholeItemIntoTheInventoryAndRemovesItFromTheWorld() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(LOG, 4));
        simulation.dropItem(PlayerId.LOCAL, 0, 4);
        SimFixture.lookDown(simulation, DOWN);

        Optional<Interactable> target = simulation.interactableFor(PlayerId.LOCAL);
        assertTrue(target.isPresent());
        assertEquals(InteractableKind.ITEM, target.get().kind());
        ActionResult result = simulation.interact(new InteractionRequest(PlayerId.LOCAL, target.get().ref()));

        assertTrue(result.success(), result.message());
        assertEquals(4, simulation.inventory(PlayerId.LOCAL).count(LOG));
        assertTrue(simulation.worldItems().isEmpty());
    }

    @Test
    void anItemOutOfReachCannotBePickedUp() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(LOG, 1));
        simulation.dropItem(PlayerId.LOCAL, 0, 1);
        PlayerId far = new PlayerId(2L);
        simulation.addPlayer(far, new SpawnPoints.Spawn(
                SimFixture.world().cabin().doorX() + 15f, SimFixture.world().cabin().doorZ(), 0f));

        ActionResult result = simulation.pickUpItem(far, simulation.worldItems().get(0).id());

        assertFalse(result.success());
        assertEquals(1, simulation.worldItems().size());
        assertEquals(0, simulation.inventory(far).count(LOG));
    }

    @Test
    void aFullInventoryRefusesAndLeavesTheItemWhereItIs() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(LOG, 2));
        simulation.dropItem(PlayerId.LOCAL, 0, 2);
        for (int i = 0; i < inventory.size(); i++) {
            inventory.add(new ItemStack(LAMP, 1));
        }
        SimFixture.lookDown(simulation, DOWN);
        Interactable target = simulation.interactableFor(PlayerId.LOCAL).orElseThrow();

        ActionResult result = simulation.interact(new InteractionRequest(PlayerId.LOCAL, target.ref()));

        assertFalse(result.success());
        assertTrue(result.message().contains("full"));
        assertEquals(1, simulation.worldItems().size());
        assertEquals(2, simulation.worldItems().get(0).stack().quantity());
        assertEquals(0, inventory.count(LOG));
    }

    @Test
    void aPartialPickupTakesWhatFitsAndLeavesTheRestWithTheSameId() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(LOG, 5));
        simulation.dropItem(PlayerId.LOCAL, 0, 5);
        WorldItem before = simulation.worldItems().get(0);
        for (int i = 0; i < inventory.size() - 1; i++) {
            inventory.add(new ItemStack(LAMP, 1));
        }
        inventory.add(new ItemStack(LOG, 8));
        SimFixture.lookDown(simulation, DOWN);
        Interactable target = simulation.interactableFor(PlayerId.LOCAL).orElseThrow();

        ActionResult result = simulation.interact(new InteractionRequest(PlayerId.LOCAL, target.ref()));

        assertTrue(result.success(), result.message());
        assertEquals(10, inventory.count(LOG));
        WorldItem after = simulation.worldItems().get(0);
        assertEquals(before.id(), after.id());
        assertEquals(3, after.stack().quantity());
        assertEquals(13, totalOf(simulation, LOG));
    }

    @Test
    void itemsCannotBePickedUpThroughAWall() {
        WorldSimulation simulation = SimFixture.simulation();
        Footprint cabin = SimFixture.world().cabin().footprint();
        SimFixture.insideAt(simulation, PlayerId.LOCAL, 0f, 1.5f);
        simulation.inventory(PlayerId.LOCAL).add(new ItemStack(LOG, 1));
        simulation.dropItem(PlayerId.LOCAL, 0, 1);
        // A second player stands just outside the right wall, close enough to touch the item otherwise.
        PlayerId outsider = new PlayerId(2L);
        float[] outsideSpot = cabin.toWorld(cabin.halfWidth() + 0.6f, 1.5f);
        simulation.addPlayer(outsider, new SpawnPoints.Spawn(outsideSpot[0], outsideSpot[1], 0f));
        WorldItem item = simulation.worldItems().get(0);

        ActionResult result = simulation.pickUpItem(outsider, item.id());

        assertFalse(result.success());
        assertEquals(1, simulation.worldItems().size());
    }

    @Test
    void dropAndPickUpNeverCreateOrLoseItems() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(LOG, 17));
        inventory.add(new ItemStack(TestItems.PEBBLE, 7));
        inventory.add(new ItemStack(LAMP, 3));
        int logs = totalOf(simulation, LOG);
        int pebbles = totalOf(simulation, TestItems.PEBBLE);
        int lamps = totalOf(simulation, LAMP);
        Random random = new Random(7);

        for (int step = 0; step < 200; step++) {
            if (random.nextBoolean()) {
                int slot = random.nextInt(inventory.size());
                inventory.slot(slot).ifPresent(stack ->
                        simulation.dropItem(PlayerId.LOCAL, slot, 1 + random.nextInt(stack.quantity())));
            } else {
                SimFixture.lookDown(simulation, DOWN);
                simulation.interactableFor(PlayerId.LOCAL).ifPresent(target ->
                        simulation.interact(new InteractionRequest(PlayerId.LOCAL, target.ref())));
            }
            SimFixture.walkForward(simulation, 0.02f);
        }

        assertEquals(logs, totalOf(simulation, LOG));
        assertEquals(pebbles, totalOf(simulation, TestItems.PEBBLE));
        assertEquals(lamps, totalOf(simulation, LAMP));
    }

    @Test
    void worldItemIdsAreUniqueAndIncrease() {
        WorldSimulation simulation = SimFixture.simulation();
        SimFixture.outside(simulation);
        Inventory inventory = simulation.inventory(PlayerId.LOCAL);
        inventory.add(new ItemStack(LAMP, 1));
        inventory.add(new ItemStack(LAMP, 1));
        simulation.dropItem(PlayerId.LOCAL, 0, 1);
        simulation.dropItem(PlayerId.LOCAL, 1, 1);

        List<WorldItem> items = simulation.worldItems();

        assertEquals(2, items.size());
        assertTrue(items.get(0).id().value() < items.get(1).id().value());
    }
}
