package com.coldfriend.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsStoreTest {

    private static final GameSettings DEFAULTS = new GameSettings(
            80, 70, 80, new Resolution(1280, 720), false, true);

    @TempDir
    Path tempDir;

    @Test
    void missingFileYieldsDefaults() {
        SettingsStore store = new SettingsStore(tempDir.resolve("settings.properties"), DEFAULTS);

        assertEquals(DEFAULTS, store.load());
    }

    @Test
    void savedSettingsRoundTrip() throws IOException {
        Path file = tempDir.resolve("nested/settings.properties");
        SettingsStore store = new SettingsStore(file, DEFAULTS);
        GameSettings chosen = new GameSettings(10, 20, 30, new Resolution(1920, 1080), true, false);

        store.save(chosen);

        assertEquals(chosen, new SettingsStore(file, DEFAULTS).load());
        assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".tmp")));
    }

    @Test
    void invalidValuesFallBackToDefaultsWhileValidOnesAreKept() throws IOException {
        Path file = tempDir.resolve("settings.properties");
        Files.writeString(file, String.join("\n",
                "masterVolume=not-a-number",
                "musicVolume=33",
                "resolution=banana",
                "fullscreen=maybe",
                "vsync=false"), StandardCharsets.UTF_8);

        GameSettings loaded = new SettingsStore(file, DEFAULTS).load();

        assertEquals(DEFAULTS.masterVolume(), loaded.masterVolume());
        assertEquals(33, loaded.musicVolume());
        assertEquals(DEFAULTS.resolution(), loaded.resolution());
        assertEquals(DEFAULTS.fullscreen(), loaded.fullscreen());
        assertEquals(false, loaded.vsync());
    }
}
