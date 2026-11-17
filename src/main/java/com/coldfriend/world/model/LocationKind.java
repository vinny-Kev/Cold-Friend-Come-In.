package com.coldfriend.world.model;

/** What a named place in the world is for. NPCs and the player will use these as destinations. */
public enum LocationKind {
    /** Outside the cabin door. */
    CABIN_DOOR,
    /** A place where water can be collected from a river. */
    WATER_ACCESS,
    /** The central gathering area of the village. */
    VILLAGE_SQUARE,
    /** Just outside a building's door. */
    DOORSTEP,
    /** Where a shopkeeper stands and customers trade. */
    STORE_COUNTER,
    /** A home's living space, where its resident belongs. */
    RESIDENCE
}
