package com.coldfriend.inventory;

import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.ItemDefinition;
import com.coldfriend.item.ItemId;
import com.coldfriend.item.ItemStack;
import java.util.Optional;

/**
 * What one owner carries: a fixed number of slots, each empty or holding one stack. Pure game logic with no
 * engine or screen types, so it can be tested alone and used by players now and villagers and merchants later.
 * Every operation either does what it says completely or changes nothing, except {@link #add}, which reports
 * exactly how much did not fit.
 */
public final class Inventory {

    /** Number of slots a player has. */
    public static final int PLAYER_SLOTS = 24;
    /** How many of the first slots the hotbar on the play screen shows. */
    public static final int HOTBAR_SLOTS = 4;

    /** How {@link #move} ended. */
    public enum MoveResult {
        /** The stack went to an empty slot. */
        MOVED,
        /** The stack joined another of the same item. Anything over the limit stayed where it was. */
        MERGED,
        /** The two slots traded contents. */
        SWAPPED,
        /** Nothing happened: the slots are the same, or the source was empty. */
        UNCHANGED
    }

    private final ItemCatalog catalog;
    private final ItemStack[] slots;
    private int selected;

    /**
     * @param catalog the item definitions, used for stack limits and weights
     * @param size number of slots; at least 1
     */
    public Inventory(ItemCatalog catalog, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("An inventory needs at least one slot");
        }
        this.catalog = catalog;
        this.slots = new ItemStack[size];
    }

    /** @return the number of slots */
    public int size() {
        return slots.length;
    }

    /**
     * @param index a slot number from 0
     * @return what is in the slot, or empty if it is empty
     * @throws IndexOutOfBoundsException if there is no such slot
     */
    public Optional<ItemStack> slot(int index) {
        checkSlot(index);
        return Optional.ofNullable(slots[index]);
    }

    /** @return the number of empty slots */
    public int emptySlots() {
        int count = 0;
        for (ItemStack stack : slots) {
            if (stack == null) {
                count++;
            }
        }
        return count;
    }

    /** @return the total quantity of an item across all slots */
    public int count(ItemId item) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.item().equals(item)) {
                total += stack.quantity();
            }
        }
        return total;
    }

    /** @return true if at least {@code quantity} of the item are held */
    public boolean has(ItemId item, int quantity) {
        requirePositive(quantity);
        return count(item) >= quantity;
    }

    /** @return the combined weight of everything carried, in kilograms; carrying is not limited by weight yet */
    public float totalWeight() {
        float total = 0f;
        for (ItemStack stack : slots) {
            if (stack != null) {
                total += catalog.get(stack.item()).weight() * stack.quantity();
            }
        }
        return total;
    }

    /** @return true if the whole stack would fit without changing anything */
    public boolean canFit(ItemStack stack) {
        return room(stack.item()) >= stack.quantity();
    }

    /**
     * Adds as much of a stack as fits, filling partly full stacks of the same item first and then empty slots.
     *
     * @param stack what to add
     * @return how much did not fit; 0 if everything was added
     * @throws IllegalArgumentException if the item is not in the catalogue
     */
    public int add(ItemStack stack) {
        int stackLimit = catalog.get(stack.item()).maxStack();
        int left = stack.quantity();
        for (int i = 0; i < slots.length && left > 0; i++) {
            if (slots[i] != null && slots[i].item().equals(stack.item()) && slots[i].quantity() < stackLimit) {
                int moved = Math.min(left, stackLimit - slots[i].quantity());
                slots[i] = slots[i].withQuantity(slots[i].quantity() + moved);
                left -= moved;
            }
        }
        for (int i = 0; i < slots.length && left > 0; i++) {
            if (slots[i] == null) {
                int moved = Math.min(left, stackLimit);
                slots[i] = new ItemStack(stack.item(), moved);
                left -= moved;
            }
        }
        return left;
    }

    /**
     * Removes a quantity of an item, taking from the last slots first. All or nothing.
     *
     * @return true if the quantity was held and has been removed; false, with nothing changed, if there is not
     *         enough
     */
    public boolean remove(ItemId item, int quantity) {
        requirePositive(quantity);
        if (count(item) < quantity) {
            return false;
        }
        int left = quantity;
        for (int i = slots.length - 1; i >= 0 && left > 0; i--) {
            if (slots[i] != null && slots[i].item().equals(item)) {
                int taken = Math.min(left, slots[i].quantity());
                shrink(i, taken);
                left -= taken;
            }
        }
        return true;
    }

    /**
     * Takes some or all of one slot.
     *
     * @param index the slot
     * @param quantity how many to take; at least 1
     * @return what was taken, or empty if the slot is empty or holds fewer than {@code quantity}
     */
    public Optional<ItemStack> take(int index, int quantity) {
        checkSlot(index);
        requirePositive(quantity);
        ItemStack held = slots[index];
        if (held == null || held.quantity() < quantity) {
            return Optional.empty();
        }
        shrink(index, quantity);
        return Optional.of(new ItemStack(held.item(), quantity));
    }

    /**
     * Moves the contents of one slot to another. An empty target takes the stack; the same item merges up to its
     * stack limit; a different item swaps places.
     *
     * @param from the slot to move from
     * @param to the slot to move to
     * @return what happened
     */
    public MoveResult move(int from, int to) {
        checkSlot(from);
        checkSlot(to);
        if (from == to || slots[from] == null) {
            return MoveResult.UNCHANGED;
        }
        if (slots[to] == null) {
            slots[to] = slots[from];
            slots[from] = null;
            return MoveResult.MOVED;
        }
        if (slots[to].item().equals(slots[from].item())) {
            int limit = catalog.get(slots[to].item()).maxStack();
            int moved = Math.min(slots[from].quantity(), limit - slots[to].quantity());
            if (moved <= 0) {
                return MoveResult.UNCHANGED;
            }
            slots[to] = slots[to].withQuantity(slots[to].quantity() + moved);
            shrink(from, moved);
            return MoveResult.MERGED;
        }
        ItemStack held = slots[from];
        slots[from] = slots[to];
        slots[to] = held;
        return MoveResult.SWAPPED;
    }

    /** @return the selected slot number */
    public int selected() {
        return selected;
    }

    /**
     * Selects a slot.
     *
     * @param index a slot number from 0
     * @throws IndexOutOfBoundsException if there is no such slot
     */
    public void select(int index) {
        checkSlot(index);
        selected = index;
    }

    /** @return the contents of the selected slot, if any */
    public Optional<ItemStack> selectedStack() {
        return Optional.ofNullable(slots[selected]);
    }

    /** @return the definition for a stack held here */
    public ItemDefinition definitionOf(ItemStack stack) {
        return catalog.get(stack.item());
    }

    private int room(ItemId item) {
        int limit = catalog.get(item).maxStack();
        int room = 0;
        for (ItemStack stack : slots) {
            if (stack == null) {
                room += limit;
            } else if (stack.item().equals(item)) {
                room += limit - stack.quantity();
            }
        }
        return room;
    }

    private void shrink(int index, int quantity) {
        int remaining = slots[index].quantity() - quantity;
        slots[index] = remaining == 0 ? null : slots[index].withQuantity(remaining);
    }

    private void checkSlot(int index) {
        if (index < 0 || index >= slots.length) {
            throw new IndexOutOfBoundsException("No slot " + index + " in an inventory of " + slots.length);
        }
    }

    private static void requirePositive(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1 but was " + quantity);
        }
    }
}
