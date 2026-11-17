package com.coldfriend.time;

/**
 * Tuning for the game clock. Times of day are in minutes after midnight. Values are part of the game's balance,
 * so they live here rather than in the configuration file (see docs/DECISIONS.md).
 *
 * @param realSecondsPerDay how many real seconds of play one in-game day lasts
 * @param dawnStart when dawn begins
 * @param dayStart when full day begins
 * @param duskStart when dusk begins
 * @param nightStart when night begins
 * @param newGameStart the time a normal new game starts at, on day 1
 */
public record ClockParameters(
        int realSecondsPerDay,
        int dawnStart,
        int dayStart,
        int duskStart,
        int nightStart,
        int newGameStart) {

    /** One day lasts 20 real minutes; dawn 05:30, day 07:00, dusk 17:00, night 19:30; new games start at 07:00. */
    public static final ClockParameters DEFAULT = new ClockParameters(1200, 330, 420, 1020, 1170, 420);

    /** Checks that the phases are in order within one day. */
    public ClockParameters {
        if (realSecondsPerDay < 1) {
            throw new IllegalArgumentException("A day must last at least one second");
        }
        if (!(0 <= dawnStart && dawnStart < dayStart && dayStart < duskStart && duskStart < nightStart
                && nightStart < WorldClock.MINUTES_PER_DAY)) {
            throw new IllegalArgumentException("Day phases must be in order within one day");
        }
        if (newGameStart < 0 || newGameStart >= WorldClock.MINUTES_PER_DAY) {
            throw new IllegalArgumentException("The start time must be within one day");
        }
    }
}
