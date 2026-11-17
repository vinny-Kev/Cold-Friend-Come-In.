package com.coldfriend.world;

/**
 * Turns variable frame times into a whole number of fixed simulation steps, so gameplay never depends on the
 * frame rate. Time is counted in whole nanoseconds, so the same total elapsed time always gives the same number
 * of steps, however it was split into frames.
 *
 * <p>A long stall (a window drag, a slow disk) would otherwise owe the simulation many steps at once and make the
 * next frames slower still. At most {@link #MAX_STEPS_PER_UPDATE} steps run per update; time owed beyond that is
 * dropped, so the game runs briefly slower than real time instead of catching up in a burst.
 */
public final class FixedStep {

    /** Simulation steps per second of game time. */
    public static final int STEPS_PER_SECOND = 20;
    /** Length of one step in seconds. */
    public static final float STEP_SECONDS = 1f / STEPS_PER_SECOND;
    /** Most steps one update may run; the rest of a long frame is dropped. */
    public static final int MAX_STEPS_PER_UPDATE = 5;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long STEP_NANOS = NANOS_PER_SECOND / STEPS_PER_SECOND;

    private long owedNanos;
    private long droppedSteps;

    /**
     * Adds a frame's time and says how many steps are now due.
     *
     * @param frameSeconds time since the last update; zero, negative and not-a-number count as no time
     * @return how many fixed steps to run now, from 0 to {@link #MAX_STEPS_PER_UPDATE}
     */
    public int advance(float frameSeconds) {
        if (!(frameSeconds > 0f)) {
            return 0;
        }
        owedNanos += Math.round((double) frameSeconds * NANOS_PER_SECOND);
        long due = owedNanos / STEP_NANOS;
        owedNanos -= due * STEP_NANOS;
        if (due > MAX_STEPS_PER_UPDATE) {
            droppedSteps += due - MAX_STEPS_PER_UPDATE;
            return MAX_STEPS_PER_UPDATE;
        }
        return (int) due;
    }

    /** @return how far the next step is from being due, from 0 (just ran) to just under 1; for drawing between steps */
    public float alpha() {
        return (float) ((double) owedNanos / STEP_NANOS);
    }

    /** Forgets time owed but not yet run, for example when the game resumes after a pause. */
    public void reset() {
        owedNanos = 0L;
    }

    /** @return how many steps have been dropped because frames were too long */
    public long droppedSteps() {
        return droppedSteps;
    }
}
