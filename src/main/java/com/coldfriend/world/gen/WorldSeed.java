package com.coldfriend.world.gen;

import java.security.SecureRandom;

/**
 * The seed of a generated world.
 *
 * @param value the 64-bit seed
 */
public record WorldSeed(long value) {

    /** @return a new random seed for a fresh world */
    public static WorldSeed random() {
        return new WorldSeed(new SecureRandom().nextLong());
    }

    /**
     * Reads a player-supplied seed. A whole number is used as it is; any other text is hashed, so a word
     * always gives the same world.
     *
     * @param text the text the player entered
     * @return the seed
     * @throws IllegalArgumentException if the text is blank
     */
    public static WorldSeed parse(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("A seed cannot be blank");
        }
        try {
            return new WorldSeed(Long.parseLong(trimmed));
        } catch (NumberFormatException e) {
            return new WorldSeed(Mixer.hash(trimmed));
        }
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
