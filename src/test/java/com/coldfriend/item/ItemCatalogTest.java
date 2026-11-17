package com.coldfriend.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ItemCatalogTest {

    private static Properties props(String... keyValues) {
        Properties properties = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return properties;
    }

    @Test
    void idsMustBeLowerSnakeCase() {
        assertEquals("water_container", new ItemId("water_container").value());
        for (String bad : new String[] {"", "Firewood", "1log", "fire wood", "fire-wood", null}) {
            assertThrows(IllegalArgumentException.class, () -> new ItemId(bad), "accepted: " + bad);
        }
    }

    @Test
    void definitionsRejectNonsense() {
        ItemId id = new ItemId("rock");
        assertThrows(IllegalArgumentException.class, () -> new ItemDefinition(id, "", "", ItemCategory.MATERIAL, 5, 1f,
                "", "", true, true, true, 0f, ""));
        assertThrows(IllegalArgumentException.class, () -> new ItemDefinition(id, "Rock", "", ItemCategory.MATERIAL, 0,
                1f, "", "", true, true, true, 0f, ""));
        assertThrows(IllegalArgumentException.class, () -> new ItemDefinition(id, "Rock", "", ItemCategory.MATERIAL, 5,
                -1f, "", "", true, true, true, 0f, ""));
    }

    @Test
    void stackLimitsAreKept() {
        ItemCatalog catalog = TestItems.catalog();

        assertEquals(10, catalog.get(TestItems.LOG).maxStack());
        assertTrue(catalog.get(TestItems.LOG).stackable());
        assertFalse(catalog.get(TestItems.LAMP).stackable());
    }

    @Test
    void lookupFindsKnownItemsAndHandlesMissingOnes() {
        ItemCatalog catalog = TestItems.catalog();
        ItemId missing = new ItemId("unicorn");

        assertTrue(catalog.find(TestItems.PEBBLE).isPresent());
        assertTrue(catalog.find(missing).isEmpty());
        assertFalse(catalog.contains(missing));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> catalog.get(missing));
        assertTrue(error.getMessage().contains("unicorn"));
    }

    @Test
    void duplicateIdsAreRejected() {
        ItemDefinition one = TestItems.catalog().get(TestItems.LOG);

        assertThrows(IllegalArgumentException.class, () -> ItemCatalog.of(List.of(one, one)));
    }

    @Test
    void parsesPropertiesWithDefaults() {
        Properties p = props(
                "items", "rock",
                "item.rock.name", "Rock",
                "item.rock.category", "material",
                "item.rock.maxStack", "4",
                "startingKit", "rock:3");

        ItemCatalog catalog = ItemCatalog.parse(p);
        ItemDefinition rock = catalog.get(new ItemId("rock"));

        assertEquals("Rock", rock.name());
        assertEquals(ItemCategory.MATERIAL, rock.category());
        assertTrue(rock.canPickUp());
        assertTrue(rock.canDrop());
        assertFalse(rock.canPlace());
        assertEquals(List.of(new ItemStack(rock.id(), 3)), catalog.startingKit());
    }

    @Test
    void invalidDataFailsWithTheItemAndKey() {
        Properties missingName = props("items", "rock", "item.rock.category", "MATERIAL", "item.rock.maxStack", "4");
        Properties badStack = props("items", "rock", "item.rock.name", "Rock", "item.rock.category", "MATERIAL",
                "item.rock.maxStack", "many");
        Properties badKit = props("items", "rock", "item.rock.name", "Rock", "item.rock.category", "MATERIAL",
                "item.rock.maxStack", "4", "startingKit", "ghost:1");

        assertTrue(assertThrows(IllegalStateException.class, () -> ItemCatalog.parse(missingName)).getMessage()
                .contains("item.rock.name"));
        assertTrue(assertThrows(IllegalStateException.class, () -> ItemCatalog.parse(badStack)).getMessage()
                .contains("rock"));
        assertTrue(assertThrows(IllegalStateException.class, () -> ItemCatalog.parse(badKit)).getMessage()
                .contains("ghost"));
    }

    @Test
    void theShippedItemFileLoadsAndEveryItemIsUsable() throws IOException {
        ItemCatalog catalog = ItemCatalog.load(Path.of("config", "items.properties"));

        assertTrue(catalog.all().size() >= 6);
        for (ItemDefinition item : catalog.all()) {
            assertFalse(item.name().isBlank(), item.id() + " has no name");
            assertFalse(item.modelKey().isBlank(), item.id() + " has no model key");
        }
        for (ItemStack stack : catalog.startingKit()) {
            assertTrue(stack.quantity() <= catalog.get(stack.item()).maxStack() * 3);
        }
    }
}
