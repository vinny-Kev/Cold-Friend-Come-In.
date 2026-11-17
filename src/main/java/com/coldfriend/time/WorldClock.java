package com.coldfriend.time;

/**
 * Game time: the authoritative day and time of day. It advances one fixed simulation step at a time and never
 * reads the computer's clock, so it stops while the game is paused and is the same for the same number of steps.
 *
 * <p>Time is kept as a whole number of steps, so day changes happen at an exact step with no rounding drift.
 * Only the simulation advances it; other code reads it through the simulation.
 */
public final class WorldClock {

    /** Minutes in one in-game day. */
    public static final int MINUTES_PER_DAY = 1440;

    private final ClockParameters parameters;
    private final long stepsPerDay;
    private long step;

    /**
     * @param parameters day length and phase times
     * @param stepsPerSecond simulation steps per real second
     * @param startMinute minute after midnight on day 1 that the clock starts at
     */
    public WorldClock(ClockParameters parameters, int stepsPerSecond, int startMinute) {
        if (stepsPerSecond < 1) {
            throw new IllegalArgumentException("There must be at least one step per second");
        }
        if (startMinute < 0 || startMinute >= MINUTES_PER_DAY) {
            throw new IllegalArgumentException("Start minute must be within one day but was " + startMinute);
        }
        this.parameters = parameters;
        this.stepsPerDay = (long) parameters.realSecondsPerDay() * stepsPerSecond;
        this.step = startMinute * stepsPerDay / MINUTES_PER_DAY;
    }

    /**
     * Advances the clock by one simulation step. Only the simulation calls this.
     *
     * @return true if this step began a new day
     */
    public boolean advanceStep() {
        step++;
        return step % stepsPerDay == 0;
    }

    /** @return in-game minutes that pass in one simulation step */
    public double minutesPerStep() {
        return (double) MINUTES_PER_DAY / stepsPerDay;
    }

    /** @return the day number, starting at 1 */
    public int day() {
        return (int) (step / stepsPerDay) + 1;
    }

    /** @return game minutes since the start of day 1, counting from the first step; never decreases */
    public double totalMinutes() {
        return (double) step * MINUTES_PER_DAY / stepsPerDay;
    }

    /** @return minutes after midnight, from 0 to just under 1440 */
    public double minuteOfDay() {
        return (double) (step % stepsPerDay) * MINUTES_PER_DAY / stepsPerDay;
    }

    /** @return the part of the day it is now */
    public DayPhase phase() {
        return phaseAt(minuteOfDay());
    }

    /**
     * @param minute minutes after midnight
     * @return the part of the day at that time
     */
    public DayPhase phaseAt(double minute) {
        if (minute >= parameters.nightStart() || minute < parameters.dawnStart()) {
            return DayPhase.NIGHT;
        }
        if (minute < parameters.dayStart()) {
            return DayPhase.DAWN;
        }
        return minute < parameters.duskStart() ? DayPhase.DAY : DayPhase.DUSK;
    }

    /** @return the clock's tuning */
    public ClockParameters parameters() {
        return parameters;
    }
}
