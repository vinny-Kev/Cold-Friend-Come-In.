package com.coldfriend.survival;

/**
 * A read-only snapshot of a hearth, for the screen and for later systems.
 *
 * @param state burning, embers, out or never lit
 * @param level how big the fire is, 0 to the maximum level; 0 when not lit
 * @param fuel fuel-hours in the stove
 * @param maxFuel most fuel the stove holds
 * @param heat how much heat it gives the room
 */
public record HearthStatus(HearthState state, double level, double fuel, double maxFuel, Heat heat) {
}
