package com.coldfriend.state;

/**
 * Choices for starting a world that are not part of the world itself. The defaults are what a normal game uses;
 * the others exist for development and testing.
 *
 * @param spawn where the player starts: {@code cabin}, {@code cabindoor}, {@code cabinview}, {@code village}
 *        or {@code river}
 * @param lighting the time of day
 * @param scenario a development script such as {@code inventory}, or empty for none
 */
public record PlayOptions(String spawn, LightingProfile lighting, String scenario) {

    /** A normal game: start in the cabin at dusk with no script. */
    public static final PlayOptions NORMAL = new PlayOptions("cabin", LightingProfile.DUSK, "");
}
