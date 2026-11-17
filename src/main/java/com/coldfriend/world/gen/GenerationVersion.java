package com.coldfriend.world.gen;

/**
 * The version of the world-generation algorithms. Bump it whenever a change would alter the world a seed
 * produces; a seed and a version together identify one world.
 */
public final class GenerationVersion {

    /** The version used for newly generated worlds. */
    public static final int CURRENT = 1;

    private GenerationVersion() {
    }
}
