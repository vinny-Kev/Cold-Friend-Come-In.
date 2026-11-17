package com.coldfriend.audio;

/**
 * Counts how far a person has walked and says when a footstep is due. Engine-free so it can be tested directly.
 * Distance left over after a step carries into the next one, so steps stay evenly spaced.
 */
public final class StepCounter {

    private final float strideMetres;
    private float travelled;

    /** @param strideMetres distance between two footsteps, in metres; must be positive */
    public StepCounter(float strideMetres) {
        if (strideMetres <= 0f) {
            throw new IllegalArgumentException("stride must be positive: " + strideMetres);
        }
        this.strideMetres = strideMetres;
    }

    /**
     * Adds distance walked this frame.
     *
     * @param metres distance moved since the last call; negative values are ignored
     * @return true if a footstep is due
     */
    public boolean advance(float metres) {
        if (metres > 0f) {
            travelled += metres;
        }
        if (travelled >= strideMetres) {
            travelled -= strideMetres;
            return true;
        }
        return false;
    }

    /** Forgets distance already walked, for example after a teleport or when the player is placed. */
    public void reset() {
        travelled = 0f;
    }
}
