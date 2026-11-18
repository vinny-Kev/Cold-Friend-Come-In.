package com.coldfriend.survival;

/** The four things a player must keep up. Each is a value from 0 (worst) to 100 (best). */
public enum Need {
    /** Body heat. Falls in the cold, rises by a fire. */
    WARMTH,
    /** Food. Falls over time, restored by eating. */
    NOURISHMENT,
    /** Water. Falls over time, restored by drinking. */
    HYDRATION,
    /** Falls only while another need is critical. At 0 the player dies. */
    HEALTH
}
