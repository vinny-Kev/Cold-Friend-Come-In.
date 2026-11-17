package com.coldfriend.ui;

import java.util.List;

/**
 * Selection state and navigation for a vertical menu. Holds no rendering code, so it can be unit tested.
 * Navigation wraps around and skips disabled items.
 */
public final class MenuModel {

    private final List<MenuItem> items;
    private int selected;

    /**
     * @param items the menu rows, in display order; must not be empty
     */
    public MenuModel(List<MenuItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("A menu needs at least one item");
        }
        this.items = List.copyOf(items);
        this.selected = firstEnabledIndex();
    }

    public List<MenuItem> items() {
        return items;
    }

    public int selectedIndex() {
        return selected;
    }

    /**
     * Moves the selection by {@code delta} enabled items, wrapping at the ends.
     *
     * @param delta positive moves down, negative moves up
     */
    public void moveSelection(int delta) {
        int direction = Integer.signum(delta);
        int steps = Math.abs(delta);
        for (int i = 0; i < steps; i++) {
            selected = nextEnabledIndex(selected, direction);
        }
    }

    /**
     * Selects a row if it exists and is enabled.
     *
     * @param index row index
     */
    public void select(int index) {
        if (index >= 0 && index < items.size() && items.get(index).isEnabled()) {
            selected = index;
        }
    }

    /**
     * Selects and confirms a row, as a mouse click does. Does nothing if the row is missing or disabled.
     *
     * @param index row index
     */
    public void activate(int index) {
        if (index >= 0 && index < items.size() && items.get(index).isEnabled()) {
            selected = index;
            items.get(index).confirm();
        }
    }

    /** Runs the selected item's confirm action. */
    public void confirmSelected() {
        items.get(selected).confirm();
    }

    /**
     * Adjusts the selected item's value.
     *
     * @param step -1 or +1
     */
    public void adjustSelected(int step) {
        items.get(selected).adjust(step);
    }

    private int firstEnabledIndex() {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEnabled()) {
                return i;
            }
        }
        return 0;
    }

    private int nextEnabledIndex(int from, int direction) {
        int index = from;
        for (int i = 0; i < items.size(); i++) {
            index = Math.floorMod(index + direction, items.size());
            if (items.get(index).isEnabled()) {
                return index;
            }
        }
        return from;
    }
}
