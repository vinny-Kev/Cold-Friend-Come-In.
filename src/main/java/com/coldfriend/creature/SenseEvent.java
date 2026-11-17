package com.coldfriend.creature;

/**
 * Something the player did, where, and when. Made by the simulation and read by the creature. It is approximate
 * on purpose: the creature hears a place, not an exact position, and that is decided when it is heard.
 *
 * @param kind what happened
 * @param x world x where it happened, in metres
 * @param z world z where it happened, in metres
 * @param minute game time in minutes since the start of day 1
 */
public record SenseEvent(SenseKind kind, float x, float z, double minute) {
}
