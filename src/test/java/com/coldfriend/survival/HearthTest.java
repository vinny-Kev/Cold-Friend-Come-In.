package com.coldfriend.survival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class HearthTest {

    /** The default rules, but starting with two fuel-hours (level 0.3), so the timings below are easy to follow. */
    private static final HearthParameters P = new HearthParameters(1.0, 6.0, 0.15, 0.9, 0.15, 0.3, 0.1, 30.0, 2.0,
            true);
    private static final HearthParameters COLD_START = new HearthParameters(1.0, 6.0, 0.15, 0.9, 0.15, 0.3, 0.1,
            30.0, 0.0, false);
    private static final double STEP = 0.06;

    private static void run(Hearth h, double minutes) {
        long steps = Math.round(minutes / STEP);
        for (long i = 0; i < steps; i++) {
            h.update(STEP);
        }
    }

    @Test
    void aNewWorldStartsWithAStrongFire() {
        Hearth h = new Hearth(HearthParameters.DEFAULT);
        assertEquals(HearthState.BURNING, h.status().state());
        assertEquals(3.0, h.status().fuel());
        assertEquals(0.45, h.level(), 1e-9);
        assertEquals(Heat.STRONG, h.heat());
    }

    @Test
    void twoFuelHoursAreExactlyAStrongFire() {
        Hearth h = new Hearth(P);
        assertEquals(HearthState.BURNING, h.status().state());
        assertEquals(2.0, h.status().fuel());
        assertEquals(0.3, h.level(), 1e-9);
        assertEquals(Heat.STRONG, h.heat());
    }

    @Test
    void addingFirewoodRaisesFuelAndLevelUpToTheCapacity() {
        Hearth h = new Hearth(P);
        assertTrue(h.addFirewood(2));
        assertEquals(4.0, h.status().fuel(), 1e-9);
        assertEquals(0.6, h.level(), 1e-9);
        assertTrue(h.addFirewood(2));
        assertEquals(0.9, h.level(), 1e-9);
        assertFalse(h.canAdd(1));
        assertFalse(h.addFirewood(1), "a full stove refuses more");
        assertEquals(6.0, h.status().fuel(), 1e-9);
        assertFalse(h.addFirewood(0));
    }

    @Test
    void fuelBurnsAtTheFireLevelPerHour() {
        Hearth h = new Hearth(P);
        // Fuel decays as fuel' = -0.15 fuel: after one hour 2.0 * e^-0.15.
        run(h, 60);
        assertEquals(2.0 * Math.exp(-0.15), h.status().fuel(), 0.001);
    }

    @Test
    void aStrongFireFadesToLowHeatThenEmbersThenOut() {
        Hearth h = new Hearth(P);
        run(h, 1);
        assertEquals(Heat.LOW, h.heat(), "just below 0.3 the heat is low");
        // Fuel reaches 1.0 (level 0.15) after ln 2 / 0.15 hours, about 277 minutes.
        run(h, 280);
        assertEquals(HearthState.EMBERS, h.status().state());
        assertEquals(Heat.LOW, h.heat());
        while (h.status().fuel() > 0.0) {
            h.update(STEP);
        }
        assertTrue(h.status().state().lit(), "embers glow for a while after the fuel is gone");
        run(h, 29);
        assertEquals(HearthState.EMBERS, h.status().state());
        run(h, 2);
        assertEquals(HearthState.OUT, h.status().state());
        assertEquals(Heat.NONE, h.heat());
        assertEquals(0.0, h.level());
    }

    @Test
    void feedingEmbersBringsBackFlamesWithoutALight() {
        Hearth h = new Hearth(P);
        run(h, 400);
        assertEquals(HearthState.EMBERS, h.status().state());
        assertTrue(h.addFirewood(2));
        assertEquals(HearthState.BURNING, h.status().state());
    }

    @Test
    void anOutStoveKeepsItsFuelUntilLit() {
        Hearth h = new Hearth(COLD_START);
        assertEquals(HearthState.UNLIT, h.status().state());
        assertFalse(h.canLight(), "nothing to light");
        assertFalse(h.light());
        assertTrue(h.addFirewood(3));
        assertEquals(HearthState.UNLIT, h.status().state(), "adding wood does not light it");
        run(h, 600);
        assertEquals(3.0, h.status().fuel(), "cold fuel does not burn");
        assertTrue(h.light());
        assertEquals(HearthState.BURNING, h.status().state());
        assertFalse(h.light(), "already lit");
    }

    @Test
    void anOutHearthCanBeRelit() {
        Hearth h = new Hearth(P);
        run(h, 2000);
        assertEquals(HearthState.OUT, h.status().state());
        assertTrue(h.addFirewood(2));
        assertEquals(HearthState.OUT, h.status().state());
        assertTrue(h.light());
        assertEquals(Heat.STRONG, h.heat());
    }

    @Test
    void noTimeMeansNoBurning() {
        Hearth h = new Hearth(P);
        h.update(0);
        h.update(-3);
        h.update(Double.NaN);
        assertEquals(2.0, h.status().fuel());
    }

    @Test
    void fuelNeverGoesNegativeOrAboveTheMaximum() {
        Random random = new Random(7L);
        for (int run = 0; run < 25; run++) {
            Hearth h = new Hearth(P);
            for (int i = 0; i < 5000; i++) {
                h.update(random.nextDouble() * 10);
                if (random.nextInt(20) == 0) {
                    h.addFirewood(1 + random.nextInt(3));
                }
                if (random.nextInt(40) == 0) {
                    h.light();
                }
                double fuel = h.status().fuel();
                assertTrue(fuel >= 0 && fuel <= P.maxFuel() + 1e-9, "fuel " + fuel);
                assertTrue(h.level() >= 0 && h.level() <= P.maxLevel() + 1e-9);
            }
        }
    }

    @Test
    void sameStepsGiveTheSameFire() {
        Hearth a = new Hearth(P);
        Hearth b = new Hearth(P);
        run(a, 333);
        run(b, 333);
        assertEquals(a.status(), b.status());
    }
}
