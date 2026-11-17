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
 * @param fog strength of the distance fog on the menu backdrop
 * @param snow amount of falling snow on the menu backdrop
 * @param cameraSway whether the backdrop camera sways slowly
 */
public record GameSettings(
        int masterVolume,
        int musicVolume,
        int sfxVolume,
        Resolution resolution,
        boolean fullscreen,
        boolean vsync,
        Intensity fog,
        Intensity snow,
        boolean cameraSway) {

    /** Lowest allowed volume percentage. */
    public static final int MIN_VOLUME = 0;
    /** Highest allowed volume percentage. */
    public static final int MAX_VOLUME = 100;
    /** Amount a volume changes per adjustment step. */
    public static final int VOLUME_STEP = 10;

    public GameSettings {
        Objects.requireNonNull(resolution, "resolution");
        Objects.requireNonNull(fog, "fog");
        Objects.requireNonNull(snow, "snow");
        masterVolume = clampVolume(masterVolume);
        musicVolume = clampVolume(musicVolume);
        sfxVolume = clampVolume(sfxVolume);
    }

    /**
     * Returns whether changing from this value to {@code other} needs a display restart to take effect.
     * Fog, snow and camera sway apply immediately and never need one.
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
        return new GameSettings(value, musicVolume, sfxVolume, resolution, fullscreen, vsync, fog, snow, cameraSway);
    }

    public GameSettings withMusicVolume(int value) {
        return new GameSettings(masterVolume, value, sfxVolume, resolution, fullscreen, vsync, fog, snow, cameraSway);
    }

    public GameSettings withSfxVolume(int value) {
        return new GameSettings(masterVolume, musicVolume, value, resolution, fullscreen, vsync, fog, snow, cameraSway);
    }

    public GameSettings withResolution(Resolution value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, value, fullscreen, vsync, fog, snow, cameraSway);
    }

    public GameSettings withFullscreen(boolean value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, resolution, value, vsync, fog, snow, cameraSway);
    }

    public GameSettings withVsync(boolean value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, resolution, fullscreen, value, fog, snow,
                cameraSway);
    }

    public GameSettings withFog(Intensity value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, resolution, fullscreen, vsync, value, snow,
                cameraSway);
    }

    public GameSettings withSnow(Intensity value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, resolution, fullscreen, vsync, fog, value,
                cameraSway);
    }

    public GameSettings withCameraSway(boolean value) {
        return new GameSettings(masterVolume, musicVolume, sfxVolume, resolution, fullscreen, vsync, fog, snow, value);
    }

    /**
     * How loud sound effects play, such as footsteps and wind: master volume times sound-effects volume.
     *
     * @return a value from 0 (silent) to 1 (full)
     */
    public float effectVolume() {
        return masterVolume / (float) MAX_VOLUME * sfxVolume / MAX_VOLUME;
    }

    private static int clampVolume(int value) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, value));
    }
}
