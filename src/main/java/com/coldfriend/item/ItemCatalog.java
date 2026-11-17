package com.coldfriend.item;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Every kind of item the game knows, by {@link ItemId}. Loaded from {@code config/items.properties}, so a new
 * item is a few lines of data and needs no code. The catalogue is read-only once built.
 */
public final class ItemCatalog {

    private static final String LIST_KEY = "items";
    private static final String KIT_KEY = "startingKit";
    private static final String PREFIX = "item.";
    private static final String STACK_SEPARATOR = ":";

    private final Map<ItemId, ItemDefinition> definitions = new LinkedHashMap<>();
    private final List<ItemStack> startingKit = new ArrayList<>();

    private ItemCatalog() {
    }

    /** @return a catalogue with no items, for tests and code that does not touch items */
    public static ItemCatalog empty() {
        return new ItemCatalog();
    }

    /**
     * @param items the definitions to include
     * @return a catalogue of exactly those items
     * @throws IllegalArgumentException if two definitions share an id
     */
    public static ItemCatalog of(Collection<ItemDefinition> items) {
        ItemCatalog catalog = new ItemCatalog();
        for (ItemDefinition item : items) {
            if (catalog.definitions.put(item.id(), item) != null) {
                throw new IllegalArgumentException("Two items share the id " + item.id());
            }
        }
        return catalog;
    }

    /**
     * Reads the catalogue from a properties file.
     *
     * @param file the file, normally {@code config/items.properties}
     * @return the catalogue
     * @throws IOException if the file cannot be read
     * @throws IllegalStateException if an item is missing a required value or has an invalid one; the message
     *         names the item and the key
     */
    public static ItemCatalog load(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return parse(properties);
    }

    /**
     * Builds the catalogue from already-loaded properties.
     *
     * @param properties the values
     * @return the catalogue
     * @throws IllegalStateException if a value is missing or invalid
     */
    public static ItemCatalog parse(Properties properties) {
        ItemCatalog catalog = new ItemCatalog();
        for (String name : split(properties.getProperty(LIST_KEY, ""))) {
            ItemDefinition definition = read(properties, name);
            if (catalog.definitions.put(definition.id(), definition) != null) {
                throw new IllegalStateException("Item " + name + " is listed twice");
            }
        }
        for (String entry : split(properties.getProperty(KIT_KEY, ""))) {
            catalog.startingKit.add(parseStack(catalog, entry));
        }
        return catalog;
    }

    /**
     * @param id an item id
     * @return the definition, or empty if there is no such item
     */
    public Optional<ItemDefinition> find(ItemId id) {
        return Optional.ofNullable(definitions.get(id));
    }

    /**
     * @param id an item id
     * @return the definition
     * @throws IllegalArgumentException if there is no such item
     */
    public ItemDefinition get(ItemId id) {
        ItemDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown item: " + id);
        }
        return definition;
    }

    /** @return true if the catalogue has this item */
    public boolean contains(ItemId id) {
        return definitions.containsKey(id);
    }

    /** @return every definition, in the order they are listed in the file */
    public List<ItemDefinition> all() {
        return List.copyOf(definitions.values());
    }

    /** @return what a new player starts with */
    public List<ItemStack> startingKit() {
        return List.copyOf(startingKit);
    }

    private static ItemDefinition read(Properties p, String name) {
        String base = PREFIX + name + ".";
        try {
            return new ItemDefinition(
                    new ItemId(name),
                    required(p, base + "name"),
                    p.getProperty(base + "description", ""),
                    ItemCategory.valueOf(required(p, base + "category").toUpperCase(Locale.ROOT)),
                    Integer.parseInt(required(p, base + "maxStack")),
                    Float.parseFloat(p.getProperty(base + "weight", "0")),
                    p.getProperty(base + "model", ""),
                    p.getProperty(base + "icon", ""),
                    Boolean.parseBoolean(p.getProperty(base + "pickUp", "true")),
                    Boolean.parseBoolean(p.getProperty(base + "drop", "true")),
                    Boolean.parseBoolean(p.getProperty(base + "place", "false")),
                    Float.parseFloat(p.getProperty(base + "placeRadius", "0")),
                    p.getProperty(base + "useVerb", ""));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid item '" + name + "': " + e.getMessage(), e);
        }
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value.trim();
    }

    private static ItemStack parseStack(ItemCatalog catalog, String entry) {
        String[] parts = entry.split(STACK_SEPARATOR);
        ItemId id = new ItemId(parts[0].trim());
        if (!catalog.contains(id)) {
            throw new IllegalStateException("startingKit refers to unknown item " + id);
        }
        int quantity = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
        return new ItemStack(id, quantity);
    }

    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split(",")) {
            if (!part.isBlank()) {
                parts.add(part.trim());
            }
        }
        return parts;
    }
}
