package com.coldfriend.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class GameConfigTest {

    private static GameConfig config(String... keyValues) {
        Properties props = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            props.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return GameConfig.of(props);
    }

    @Test
    void defaultSettingsAreReadFromSettingsKeys() {
        GameConfig config = config(
                "settings.default.masterVolume", "80",
                "settings.default.musicVolume", "70",
                "settings.default.sfxVolume", "60",
                "settings.default.resolution", "1600x900",
                "settings.default.fullscreen", "false",
                "settings.default.vsync", "true");

        GameSettings defaults = config.defaultSettings();

        assertEquals(new GameSettings(80, 70, 60, new Resolution(1600, 900), false, true), defaults);
    }

    @Test
    void missingKeyFailsWithItsName() {
        GameConfig config = config();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> config.getInt("menu.treeCount"));

        assertEquals("Missing config key: menu.treeCount", error.getMessage());
    }

    @Test
    void malformedNumberFailsWithItsName() {
        GameConfig config = config("menu.treeCount", "many");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> config.getInt("menu.treeCount"));

        assertEquals(true, error.getMessage().contains("menu.treeCount"));
    }

    @Test
    void booleanAcceptsTrueOrFalseOnly() {
        assertEquals(true, config("flag", "TRUE").getBoolean("flag"));
        assertFalse(config("flag", "false").getBoolean("flag"));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> config("flag", "ture").getBoolean("flag"));
        assertEquals(true, error.getMessage().contains("flag"));
    }
}
