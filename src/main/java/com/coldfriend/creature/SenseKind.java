package com.coldfriend.creature;

/**
 * Things the player does that the creature may notice. Each kind has a loudness: how far away, in metres, it can be
 * heard before walls are counted. Loudness values are the design's proposals, to be tuned in playtest.
 */
public enum SenseKind {
    /** A step taken while walking. */
    FOOTSTEP_WALK(6f),
    /** A step taken while running. */
    FOOTSTEP_SPRINT(14f),
    /** The cabin door opened or closed. */
    DOOR(20f),
    /** An item dropped on the ground. */
    DROP(4f),
    /** An item placed in the world. */
    PLACE(3f),
    /** An item picked up from the ground (resource gathering). */
    PICK_UP(3f);

    private final float loudness;

    SenseKind(float loudness) {
        this.loudness = loudness;
    }

    /** @return how far away this sound can be heard, in metres, before walls reduce it */
    public float loudness() {
        return loudness;
    }
}
