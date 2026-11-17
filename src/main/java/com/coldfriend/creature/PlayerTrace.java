package com.coldfriend.creature;

/**
 * What the creature remembers about one player, apart from the timed observations: where it last saw them, and when
 * it last believed they were watching it. Times are game minutes; NaN means never.
 */
public final class PlayerTrace {

    private float lastSeenX;
    private float lastSeenZ;
    private double lastSeenMinute = Double.NaN;
    private double watchedMinute = Double.NaN;

    /** Records that the creature saw the player at a place. */
    void sawAt(float x, float z, double minute) {
        lastSeenX = x;
        lastSeenZ = z;
        lastSeenMinute = minute;
    }

    /** Records that the creature believed the player was watching it. */
    void watchedAt(double minute) {
        watchedMinute = minute;
    }

    /** @return true if the creature has ever seen this player */
    public boolean hasSeen() {
        return !Double.isNaN(lastSeenMinute);
    }

    /** @return where the player was last seen, x */
    public float lastSeenX() {
        return lastSeenX;
    }

    /** @return where the player was last seen, z */
    public float lastSeenZ() {
        return lastSeenZ;
    }

    /** @return game minute the player was last seen, or NaN */
    public double lastSeenMinute() {
        return lastSeenMinute;
    }

    /** @return game minute the creature last believed it was being watched, or NaN */
    public double watchedMinute() {
        return watchedMinute;
    }
}
