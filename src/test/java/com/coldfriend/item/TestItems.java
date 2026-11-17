package com.coldfriend.item;

import java.util.List;

/** Small item catalogues for tests, so tests do not depend on the game data file. */
public final class TestItems {

    public static final ItemId LOG = new ItemId("log");
    public static final ItemId PEBBLE = new ItemId("pebble");
    public static final ItemId LAMP = new ItemId("lamp");
    public static final ItemId KEY = new ItemId("key");

    private TestItems() {
    }

    /** @return a catalogue with a stackable placeable log (stack 10), a pebble (stack 3), a lamp that does not stack, and a key that cannot be dropped or placed */
    public static ItemCatalog catalog() {
        return ItemCatalog.of(List.of(
                new ItemDefinition(LOG, "Log", "A log.", ItemCategory.MATERIAL, 10, 2f, "log", "log", true, true, true,
                        0.3f, ""),
                new ItemDefinition(PEBBLE, "Pebble", "A pebble.", ItemCategory.MATERIAL, 3, 0.1f, "pebble", "", true,
                        true, true, 0.1f, ""),
                new ItemDefinition(LAMP, "Lamp", "A lamp.", ItemCategory.TOOL, 1, 1f, "lamp", "", true, true, true,
                        0.2f, "Light"),
                new ItemDefinition(KEY, "Key", "A key.", ItemCategory.TOOL, 1, 0.01f, "key", "", true, false, false,
                        0f, "")));
    }
}
