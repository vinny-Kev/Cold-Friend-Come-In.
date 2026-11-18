package com.coldfriend.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldClockTest {

    private static final int STEPS = 20;
    private static final ClockParameters P = ClockParameters.DEFAULT;

    /** Steps per in-game minute with the default 20-minute day: 24 000 steps per day / 1440. */
    private static final double STEPS_PER_MINUTE = 1200.0 * STEPS / WorldClock.MINUTES_PER_DAY;

    private static void advanceMinutes(WorldClock clock, double minutes) {
        long steps = Math.round(minutes * STEPS_PER_MINUTE);
        for (long i = 0; i < steps; i++) {
            clock.advanceStep();
        }
    }

    @Test
    void startsOnDayOneAtTheGivenMinute() {
        WorldClock clock = new WorldClock(P, STEPS, 420);
        assertEquals(1, clock.day());
        assertEquals(420.0, clock.minuteOfDay(), 1e-9);
        assertEquals(DayPhase.DAY, clock.phase());
    }

    @Test
    void oneDayLastsTheConfiguredRealTime() {
        WorldClock clock = new WorldClock(P, STEPS, 0);
        // 1200 real seconds at 20 steps per second is 24 000 steps.
        for (int i = 0; i < 23_999; i++) {
            assertFalse(clock.advanceStep(), "day changed early at step " + i);
        }
        assertTrue(clock.advanceStep(), "the 24 000th step starts day 2");
        assertEquals(2, clock.day());
        assertEquals(0.0, clock.minuteOfDay(), 1e-9);
        assertEquals(0.06, clock.minutesPerStep(), 1e-12);
    }

    @Test
    void phasesChangeAtTheirBoundaries() {
        WorldClock clock = new WorldClock(P, STEPS, 0);
        assertEquals(DayPhase.NIGHT, clock.phaseAt(0));
        assertEquals(DayPhase.NIGHT, clock.phaseAt(329.99));
        assertEquals(DayPhase.DAWN, clock.phaseAt(330));
        assertEquals(DayPhase.DAWN, clock.phaseAt(419.99));
        assertEquals(DayPhase.DAY, clock.phaseAt(420));
        assertEquals(DayPhase.DAY, clock.phaseAt(1019.99));
        assertEquals(DayPhase.DUSK, clock.phaseAt(1020));
        assertEquals(DayPhase.DUSK, clock.phaseAt(1169.99));
        assertEquals(DayPhase.NIGHT, clock.phaseAt(1170));
        assertEquals(DayPhase.NIGHT, clock.phaseAt(1439.99));
    }

    @Test
    void wrapsAtMidnightAndCountsDays() {
        WorldClock clock = new WorldClock(P, STEPS, 1380);
        advanceMinutes(clock, 120);
        assertEquals(2, clock.day());
        assertEquals(60.0, clock.minuteOfDay(), 1e-6);
        assertEquals(DayPhase.NIGHT, clock.phase());
    }

    @Test
    void timeOnlyMovesWhenStepped() {
        WorldClock clock = new WorldClock(P, STEPS, 600);
        double before = clock.minuteOfDay();
        assertEquals(before, clock.minuteOfDay());
        clock.advanceStep();
        assertEquals(before + clock.minutesPerStep(), clock.minuteOfDay(), 1e-9);
    }

    @Test
    void sameStepsGiveTheSameTime() {
        WorldClock a = new WorldClock(P, STEPS, 420);
        WorldClock b = new WorldClock(P, STEPS, 420);
        advanceMinutes(a, 1000);
        advanceMinutes(b, 1000);
        assertEquals(a.minuteOfDay(), b.minuteOfDay());
        assertEquals(a.day(), b.day());
    }

    @Test
    void rejectsBadSettings() {
        assertThrows(IllegalArgumentException.class, () -> new WorldClock(P, STEPS, 1440));
        assertThrows(IllegalArgumentException.class, () -> new WorldClock(P, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClockParameters(1200, 500, 420, 1020, 1170, 420));
    }
}
