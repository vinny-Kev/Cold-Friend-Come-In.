package com.coldfriend.entity;

/**
 * Stable identity of something that exists in the world, such as an item lying on the ground or a placed object.
 * It is handed out by the simulation, never changes while the thing exists, and does not depend on how it is drawn.
 *
 * @param value the identifier; positive
 */
public record EntityId(long value) {

    /** Rejects identifiers below one. */
    public EntityId {
        if (value < 1L) {
            throw new IllegalArgumentException("An entity id must be positive but was " + value);
        }
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
