package com.coldfriend.interaction;

/** The kinds of thing a player can use. */
public enum InteractableKind {
    /** A door that opens and closes. */
    DOOR,
    /** An item lying or placed in the world, which can be picked up. */
    ITEM,
    /** A stove or fireplace that burns fuel. */
    HEARTH,
    /** The car: get in, or get out when driving. */
    VEHICLE
}
