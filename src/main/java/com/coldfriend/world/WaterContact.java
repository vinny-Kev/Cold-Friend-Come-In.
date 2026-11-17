package com.coldfriend.world;

/** What an entity's feet are touching, as far as water is concerned. */
public enum WaterContact {
    /** Not in or on water. */
    DRY,
    /** Standing on frozen water. */
    ON_ICE,
    /** In open (unfrozen) water at least {@link WaterDetector#MIN_WET_DEPTH} deep. */
    IN_WATER
}
