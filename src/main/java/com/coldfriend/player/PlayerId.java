package com.coldfriend.player;

/**
 * Stable identity of a player. It never changes during a game and does not depend on anything drawn, so it can
 * identify the same player on every machine in a shared world.
 *
 * @param value the identifier
 */
public record PlayerId(long value) {

    /** The only player in a single-player game. */
    public static final PlayerId LOCAL = new PlayerId(1L);
}
