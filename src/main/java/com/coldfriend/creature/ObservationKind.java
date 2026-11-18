package com.coldfriend.creature;

/** What the creature noticed. Each kind decays at its own rate; see {@link CreatureParameters}. */
public enum ObservationKind {
    /** The creature saw the player. */
    SIGHT,
    /** The creature heard something: a step, a door. Only an approximate place. */
    SOUND,
    /** The creature saw footprints in the snow. */
    TRACK,
    /** The creature noticed something had been dropped, placed or taken. */
    CHANGED;

    /** @return how long this kind of observation lasts, in game minutes */
    public double lifetime() {
        return switch (this) {
            case SIGHT -> CreatureParameters.SIGHT_LIFETIME;
            case SOUND -> CreatureParameters.SOUND_LIFETIME;
            case TRACK -> CreatureParameters.TRACK_LIFETIME;
            case CHANGED -> CreatureParameters.CHANGED_LIFETIME;
        };
    }
}
