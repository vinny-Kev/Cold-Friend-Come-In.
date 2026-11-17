package com.coldfriend.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.inventory.Inventory.MoveResult;
import com.coldfriend.item.ItemId;
import com.coldfriend.item.ItemStack;
import com.coldfriend.item.TestItems;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InventoryTest {

    private static final ItemId LOG = TestItems.LOG;
    private static final ItemId PEBBLE = TestItems.PEBBLE;
    private static final ItemId LAMP = TestItems.LAMP;

    private static Inventory inventory(int slots) {
        return new Inventory(TestItems.catalog(), slots);
    }

    @Test
    void addingFillsStacksThenEmptySlots() {
        Inventory inv = inventory(4);

        assertEquals(0, inv.add(new ItemStack(LOG, 4)));
        assertEquals(0, inv.add(new ItemStack(LOG, 9)));

        assertEquals(Optional.of(new ItemStack(LOG, 10)), inv.slot(0));
        assertEquals(Optional.of(new ItemStack(LOG, 3)), inv.slot(1));
        assertEquals(13, inv.count(LOG));
        assertEquals(2, inv.emptySlots());
    }

    @Test
    void maximumStackSizeIsEnforced() {
        Inventory inv = inventory(3);

        inv.add(new ItemStack(PEBBLE, 8));

        assertEquals(Optional.of(new ItemStack(PEBBLE, 3)), inv.slot(0));
        assertEquals(Optional.of(new ItemStack(PEBBLE, 3)), inv.slot(1));
        assertEquals(Optional.of(new ItemStack(PEBBLE, 2)), inv.slot(2));
    }

    @Test
    void aFullInventoryReportsWhatDidNotFitAndKeepsWhatDid() {
        Inventory inv = inventory(2);
        inv.add(new ItemStack(PEBBLE, 5));

        int left = inv.add(new ItemStack(PEBBLE, 4));

        assertEquals(3, left);
        assertEquals(6, inv.count(PEBBLE));
        assertEquals(0, inv.emptySlots());
        assertEquals(1, inv.add(new ItemStack(LAMP, 1)));
    }

    @Test
    void canFitDoesNotChangeAnything() {
        Inventory inv = inventory(1);
        inv.add(new ItemStack(PEBBLE, 2));

        assertTrue(inv.canFit(new ItemStack(PEBBLE, 1)));
        assertFalse(inv.canFit(new ItemStack(PEBBLE, 2)));
        assertFalse(inv.canFit(new ItemStack(LAMP, 1)));
        assertEquals(2, inv.count(PEBBLE));
    }

    @Test
    void removingTakesFromSeveralSlotsAndIsAllOrNothing() {
        Inventory inv = inventory(4);
        inv.add(new ItemStack(LOG, 14));

        assertTrue(inv.remove(LOG, 12));
        assertEquals(2, inv.count(LOG));
        assertFalse(inv.remove(LOG, 3));
        assertEquals(2, inv.count(LOG));
        assertTrue(inv.remove(LOG, 2));
        assertEquals(4, inv.emptySlots());
    }

    @Test
    void hasChecksQuantities() {
        Inventory inv = inventory(2);
        inv.add(new ItemStack(LOG, 5));

        assertTrue(inv.has(LOG, 5));
        assertFalse(inv.has(LOG, 6));
        assertFalse(inv.has(LAMP, 1));
    }

    @Test
    void invalidQuantitiesAreRejected() {
        Inventory inv = inventory(2);
        inv.add(new ItemStack(LOG, 5));

        assertThrows(IllegalArgumentException.class, () -> new ItemStack(LOG, 0));
        assertThrows(IllegalArgumentException.class, () -> new ItemStack(LOG, -2));
        assertThrows(IllegalArgumentException.class, () -> inv.remove(LOG, 0));
        assertThrows(IllegalArgumentException.class, () -> inv.take(0, -1));
        assertThrows(IllegalArgumentException.class, () -> inv.has(LOG, 0));
        assertEquals(5, inv.count(LOG));
    }

    @Test
    void unknownItemsAreRejected() {
        Inventory inv = inventory(2);

        assertThrows(IllegalArgumentException.class, () -> inv.add(new ItemStack(new ItemId("ghost"), 1)));
        assertEquals(2, inv.emptySlots());
    }

    @Test
    void takingPartOfAStackLeavesTheRest() {
        Inventory inv = inventory(2);
        inv.add(new ItemStack(LOG, 6));

        assertEquals(Optional.of(new ItemStack(LOG, 2)), inv.take(0, 2));
        assertEquals(Optional.of(new ItemStack(LOG, 4)), inv.slot(0));
        assertTrue(inv.take(0, 5).isEmpty());
        assertTrue(inv.take(1, 1).isEmpty());
        assertEquals(Optional.of(new ItemStack(LOG, 4)), inv.take(0, 4));
        assertTrue(inv.slot(0).isEmpty());
    }

    @Test
    void movingToAnEmptySlotMovesTheStack() {
        Inventory inv = inventory(3);
        inv.add(new ItemStack(LOG, 4));

        assertEquals(MoveResult.MOVED, inv.move(0, 2));

        assertTrue(inv.slot(0).isEmpty());
        assertEquals(Optional.of(new ItemStack(LOG, 4)), inv.slot(2));
    }

    @Test
    void movingOntoTheSameItemMergesUpToTheLimit() {
        Inventory inv = inventory(3);
        inv.add(new ItemStack(PEBBLE, 3));
        inv.add(new ItemStack(PEBBLE, 2));

        assertEquals(MoveResult.UNCHANGED, inv.move(1, 0));
        inv.take(0, 1);
        assertEquals(MoveResult.MERGED, inv.move(1, 0));

        assertEquals(Optional.of(new ItemStack(PEBBLE, 3)), inv.slot(0));
        assertEquals(Optional.of(new ItemStack(PEBBLE, 1)), inv.slot(1));
        assertEquals(4, inv.count(PEBBLE));
    }

    @Test
    void movingOntoADifferentItemSwaps() {
        Inventory inv = inventory(2);
        inv.add(new ItemStack(LOG, 4));
        inv.add(new ItemStack(LAMP, 1));

        assertEquals(MoveResult.SWAPPED, inv.move(0, 1));

        assertEquals(Optional.of(new ItemStack(LAMP, 1)), inv.slot(0));
        assertEquals(Optional.of(new ItemStack(LOG, 4)), inv.slot(1));
    }

    @Test
    void movingNothingOrToTheSameSlotChangesNothing() {
        Inventory inv = inventory(2);
        inv.add(new ItemStack(LOG, 4));

        assertEquals(MoveResult.UNCHANGED, inv.move(1, 0));
        assertEquals(MoveResult.UNCHANGED, inv.move(0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> inv.move(0, 5));
        assertEquals(4, inv.count(LOG));
    }

    @Test
    void selectionTracksASlot() {
        Inventory inv = inventory(3);
        inv.add(new ItemStack(LOG, 4));
        inv.add(new ItemStack(LAMP, 1));

        assertEquals(Optional.of(new ItemStack(LOG, 4)), inv.selectedStack());
        inv.select(1);
        assertEquals(Optional.of(new ItemStack(LAMP, 1)), inv.selectedStack());
        inv.select(2);
        assertTrue(inv.selectedStack().isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> inv.select(3));
        assertEquals(2, inv.selected());
    }

    @Test
    void totalWeightAddsEverything() {
        Inventory inv = inventory(3);
        inv.add(new ItemStack(LOG, 3));
        inv.add(new ItemStack(LAMP, 1));

        assertEquals(7f, inv.totalWeight(), 0.0001f);
    }

    @Test
    void anInventoryNeedsAtLeastOneSlot() {
        assertThrows(IllegalArgumentException.class, () -> inventory(0));
    }
}
