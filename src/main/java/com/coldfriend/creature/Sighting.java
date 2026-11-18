package com.coldfriend.creature;

/**
 * What one look at the player established. Each fact is separate on purpose: the player seeing the creature, the
 * creature seeing the player, something blocking the view, and the creature's belief that it is being watched can
 * all differ from each other.
 *
 * @param playerCanSeeCreature the player's look cone covers the creature, within the player's range, unobstructed
 * @param creatureCanSeePlayer the creature's cone covers the player, within the creature's range, unobstructed
 * @param blocked scenery, a building or the ground cuts the line between them
 * @param creatureBelievesWatched the creature sees the player looking its way, close enough to tell
 */
public record Sighting(boolean playerCanSeeCreature, boolean creatureCanSeePlayer, boolean blocked,
        boolean creatureBelievesWatched) {
}
