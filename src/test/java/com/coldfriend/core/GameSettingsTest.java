package com.coldfriend.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameSettingsTest {

    private static final Resolution HD = new Resolution(1280, 720);
    private static final Resolution FHD = new Resolution(1920, 1080);

    private static GameSettings base() {
        return new GameSettings(80, 70, 60, HD, false, true, Intensity.NORMAL, Intensity.NORMAL, true);
    }

    @Test
    void backdropChangesApplyWithoutADisplayRestart() {
        GameSettings original = base();

        assertFalse(original.requiresDisplayRestart(original.withFog(Intensity.HEAVY)));
        assertFalse(original.requiresDisplayRestart(original.withSnow(Intensity.OFF)));
        assertFalse(original.requiresDisplayRestart(original.withCameraSway(false)));
    }

    @Test
    void intensityStepsWrapAndParseIgnoresCase() {
        assertEquals(Intensity.OFF, Intensity.HEAVY.step(1));
        assertEquals(Intensity.HEAVY, Intensity.OFF.step(-1));
        assertEquals(Intensity.LIGHT, Intensity.parse(" light "));
    }

    @Test
    void effectVolumeIsMasterTimesEffects() {
        GameSettings settings = new GameSettings(80, 70, 50, HD, false, true, Intensity.NORMAL, Intensity.NORMAL, true);

        assertEquals(0.4f, settings.effectVolume(), 1e-6f);
        assertEquals(0f, settings.withSfxVolume(0).effectVolume(), 1e-6f);
        assertEquals(1f, settings.withMasterVolume(100).withSfxVolume(100).effectVolume(), 1e-6f);
    }

    @Test
    void volumesAreClampedToPercentageRange() {
        GameSettings settings = new GameSettings(150, -20, 50, HD, false, true, Intensity.NORMAL, Intensity.NORMAL, true);

        assertEquals(GameSettings.MAX_VOLUME, settings.masterVolume());
        assertEquals(GameSettings.MIN_VOLUME, settings.musicVolume());
        assertEquals(50, settings.sfxVolume());
    }

    @Test
    void withMethodsChangeOnlyTheRequestedField() {
        GameSettings changed = base().withFullscreen(true);

        assertTrue(changed.fullscreen());
        assertEquals(base().masterVolume(), changed.masterVolume());
        assertEquals(HD, changed.resolution());
        assertTrue(changed.vsync());
    }

    @Test
    void resolutionFullscreenAndVsyncChangesNeedADisplayRestart() {
        GameSettings original = base();

        assertTrue(original.requiresDisplayRestart(original.withResolution(FHD)));
        assertTrue(original.requiresDisplayRestart(original.withFullscreen(true)));
        assertTrue(original.requiresDisplayRestart(original.withVsync(false)));
    }

    @Test
    void volumeChangesDoNotNeedADisplayRestart() {
        GameSettings original = base();

        assertFalse(original.requiresDisplayRestart(original.withMasterVolume(10)));
        assertFalse(original.requiresDisplayRestart(original.withMusicVolume(10)));
        assertFalse(original.requiresDisplayRestart(original.withSfxVolume(10)));
    }
}
