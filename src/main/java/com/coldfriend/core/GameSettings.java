package com.coldfriend.core;

import java.util.Objects;

/**
 * Player-adjustable settings. Immutable; use the {@code with...} methods to derive changed copies.
 *
 * @param masterVolume master volume percentage, 0 to 100
 * @param musicVolume music volume percentage, 0 to 100
 * @param sfxVolume sound effects volume percentage, 0 to 100
 * @param resolution window or fullscreen resolution
 * @param fullscreen whether the game runs fullscreen
 * @param vsync whether vertical sync is enabled
 */
public record GameSettings(
        int masterVolume,
        int musicVolume,
        int sfxVolume,
        Resolution resolution,
        boolean fullscreen,
        boolean vsync) {

    /** Lowest allowed volume percentage. */
    public static final int MIN_VOLUME = 0;
    /** Highest allowed volume percentage. */
    public static final int MAX_VOLUME = 100;
    /** Amount a volume changes per adjustment step. */
    public static final int VOLUME_STEP = 10;

    public GameSettings {
        Objects.requireNonNull(resolution, "resolution");
        masterVolume = clampVolume(masterVolume);
        musicVolume = clampVolume(musicVolume);
        sfxVolume = clampVolume(sfxVolume);
    }

    /**
     * Returns whether changing from this value to {@code other} needs a display restart to take effect.
     *
     * @param other the updated settings
     * @return true if resolution, fullscreen or vsync differ
     */
    public boolean requiresDisplayRestart(GameSettings other) {
        return !resolution.equals(other.resolution)
                || fullscreen != other.fullscreen
                || vsync != other.vsync;
    }

    public GameSettings withMasterVolume(int value) {
        return new GameSettings(value, musicVolume, sfxVolume, resolution, fullscreen, vsync);
    }

    public GameSettings withMusicVolume(int value) {
        return new GameSettings(masterVolume, value, sfxVolume, resolution, fullscreen, vsync);
    }

    public GameSettings withSfxVolume(int value) {
        return new GameSettings(masterVolume, musicVolume, value, resolution, fullscreen, vsync);
    }

    public GameSettings withResolution(Resolution value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, value, fullscreen, vsync);
    }

    public GameSettings withFullscreen(boolean value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, resolution, value, vsync);
    }

    public GameSettings withVsync(boolean value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, resolution, fullscreen, value);
    }

    private static int clampVolume(int value) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, value));
    }
}
