package com.coldfriend.creature;

/**
 * What the creature is doing. Each mode is chosen from what the creature remembers, never from the player's real
 * position. See {@link CreatureBrain} for how a mode is picked and how long it is kept.
 */
public enum CreatureMode {
    /** Walking slowly between spots in the forest, away from the cabin and the village. The default. */
    ROAM,
    /** Going to the strongest sound, footprints or disturbed ground it remembers, and searching there. */
    INVESTIGATE,
    /** Waiting in cover near the cabin, out of sight of where it last saw the player. */
    OBSERVE,
    /** Keeping out of sight, 30 m from where it last saw the player, and stalking them from cover. */
    STALK,
    /** Walking the ring about 15 m around the cabin, from cover. */
    CIRCLE,
    /** Standing still in cover, quiet, waiting. */
    HOLD,
    /** Leaving by cover after it believed the player was watching it. */
    WITHDRAW
}
