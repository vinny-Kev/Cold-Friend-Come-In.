package com.coldfriend.survival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.time.DayPhase;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SurvivalTest {

    private static final SurvivalParameters P = SurvivalParameters.DEFAULT;
    /** A step that divides whole minutes evenly, so test times are exact; the game uses 0.06. */
    private static final double STEP = 0.05;

    /** Runs whole fixed steps, as the simulation does. */
    private static void run(Survival s, double minutes, boolean inside, boolean wading, DayPhase phase, Heat heat) {
        long steps = Math.round(minutes / STEP);
        for (long i = 0; i < steps; i++) {
            s.update(STEP, inside, wading, phase, heat);
        }
    }

    @Test
    void startsAtTheConfiguredValues() {
        SurvivalStatus s = new Survival(P).status();
        assertEquals(80, s.warmth());
        assertEquals(100, s.nourishment());
        assertEquals(100, s.hydration());
        assertEquals(100, s.health());
        assertFalse(s.dead());
        assertFalse(s.healthFalling());
    }

    @Test
    void warmthTargetsFollowTheDesignTable() {
        Survival s = new Survival(P);
        assertEquals(85, s.warmthTarget(true, false, DayPhase.NIGHT, Heat.STRONG));
        assertEquals(55, s.warmthTarget(true, false, DayPhase.NIGHT, Heat.LOW));
        assertEquals(25, s.warmthTarget(true, false, DayPhase.DAY, Heat.NONE));
        assertEquals(40, s.warmthTarget(false, false, DayPhase.DAY, Heat.STRONG));
        assertEquals(25, s.warmthTarget(false, false, DayPhase.DAWN, Heat.NONE));
        assertEquals(25, s.warmthTarget(false, false, DayPhase.DUSK, Heat.NONE));
        assertEquals(12, s.warmthTarget(false, false, DayPhase.NIGHT, Heat.NONE));
        assertEquals(15, s.warmthTarget(false, true, DayPhase.DAY, Heat.NONE));
        assertEquals(0, s.warmthTarget(false, true, DayPhase.NIGHT, Heat.NONE), "clamped at 0");
    }

    @Test
    void warmthRisesFourAndFallsTwoPerMinuteAndStopsAtTheTarget() {
        Survival s = new Survival(P);
        run(s, 10, false, false, DayPhase.DAY, Heat.NONE);
        assertEquals(60, s.status().warmth(), 0.01, "80 falls 2 per minute for 10 minutes");
        run(s, 3, true, false, DayPhase.DAY, Heat.STRONG);
        assertEquals(72, s.status().warmth(), 0.01, "60 rises 4 per minute for 3 minutes");
        run(s, 60, true, false, DayPhase.DAY, Heat.STRONG);
        assertEquals(85, s.status().warmth(), 1e-9, "stops at the target");
    }

    @Test
    void nourishmentAndHydrationFallByTheirDailyAmounts() {
        Survival s = new Survival(P);
        run(s, 1440, true, false, DayPhase.DAY, Heat.STRONG);
        assertEquals(30, s.status().nourishment(), 0.05);
        assertEquals(10, s.status().hydration(), 0.05);
    }

    @Test
    void healthHoldsWhileNothingIsCritical() {
        Survival s = new Survival(P);
        run(s, 600, true, false, DayPhase.DAY, Heat.LOW);
        assertEquals(100, s.status().health(), 1e-9);
        assertFalse(s.status().healthFalling());
    }

    @Test
    void oneCriticalNeedCostsOneHealthPerMinute() {
        Survival s = new Survival(P);
        // Outside at night warmth falls 2 per minute from 80 toward 12, passing the critical 20 at minute 30.
        run(s, 29, false, false, DayPhase.NIGHT, Heat.NONE);
        assertEquals(100, s.status().health(), 1e-6);
        assertTrue(s.status().low(Need.WARMTH), "the warning shows before health is touched");
        run(s, 11, false, false, DayPhase.NIGHT, Heat.NONE);
        assertTrue(s.status().critical(Need.WARMTH));
        assertTrue(s.status().healthFalling());
        assertEquals(90, s.status().health(), 0.2);
    }

    @Test
    void severalCriticalNeedsCostMore() {
        Survival s = new Survival(P);
        // A day by the fire without drinking: hydration falls to 10 and becomes critical just after.
        run(s, 1442, true, false, DayPhase.DAY, Heat.STRONG);
        assertTrue(s.status().critical(Need.HYDRATION));
        double before = s.status().health();
        s.update(1.0, true, false, DayPhase.DAY, Heat.STRONG);
        assertEquals(1.0, before - s.status().health(), 1e-9, "hydration alone is critical");
        // Then out into the night until warmth is critical too.
        run(s, 34, false, false, DayPhase.NIGHT, Heat.NONE);
        assertTrue(s.status().critical(Need.WARMTH));
        before = s.status().health();
        s.update(1.0, false, false, DayPhase.NIGHT, Heat.NONE);
        assertEquals(4.0, before - s.status().health(), 1e-9, "two critical needs");
    }

    @Test
    void aWarningComesBeforeEveryCriticalLevel() {
        SurvivalStatus s = new Survival(P).status();
        assertTrue(P.warmthLow() > P.warmthCritical());
        assertTrue(P.nourishmentLow() > P.nourishmentCritical());
        assertTrue(P.hydrationLow() > P.hydrationCritical());
        assertFalse(s.low(Need.WARMTH));
        // From full, a critical need costs one health per minute, so death takes at least 100 game minutes.
        assertTrue(P.healthLossSeveral() * 1 < 100, "no single minute can kill");
    }

    @Test
    void healthRecoversWhenAllNeedsAreComfortable() {
        Survival s = new Survival(P);
        run(s, 40, false, false, DayPhase.NIGHT, Heat.NONE);
        double hurt = s.status().health();
        assertTrue(hurt < 100);
        run(s, 20, true, false, DayPhase.NIGHT, Heat.STRONG);
        assertTrue(s.status().health() > hurt, "health recovers by a strong fire with food and water");
    }

    @Test
    void deathIsFinalAndNamesTheCause() {
        Survival s = new Survival(P);
        run(s, 200, false, false, DayPhase.NIGHT, Heat.NONE);
        SurvivalStatus status = s.status();
        assertTrue(status.dead());
        assertEquals(Need.WARMTH, status.causeOfDeath().orElseThrow());
        assertEquals(0, status.health());
        assertFalse(s.eat(45));
        assertFalse(s.drink(30));
        run(s, 100, true, false, DayPhase.DAY, Heat.STRONG);
        assertEquals(0, s.status().health(), "a dead player does not recover");
    }

    @Test
    void eatingAndDrinkingRestoreUpToTheMaximum() {
        Survival s = new Survival(P);
        run(s, 1440, true, false, DayPhase.DAY, Heat.STRONG);
        assertTrue(s.eat(P.rationNourishment()));
        assertEquals(75, s.status().nourishment(), 0.05);
        assertTrue(s.eat(P.rationNourishment()));
        assertEquals(100, s.status().nourishment());
        assertTrue(s.drink(P.drinkHydration()));
        assertEquals(40, s.status().hydration(), 0.05);
    }

    @Test
    void noTimeMeansNoChange() {
        Survival s = new Survival(P);
        s.update(0, false, true, DayPhase.NIGHT, Heat.NONE);
        s.update(-5, false, true, DayPhase.NIGHT, Heat.NONE);
        s.update(Double.NaN, false, true, DayPhase.NIGHT, Heat.NONE);
        assertEquals(new Survival(P).status(), s.status());
    }

    @Test
    void valuesStayInBoundsUnderRandomConditions() {
        Random random = new Random(20261118L);
        DayPhase[] phases = DayPhase.values();
        Heat[] heats = Heat.values();
        for (int run = 0; run < 25; run++) {
            Survival s = new Survival(P);
            for (int i = 0; i < 2000; i++) {
                s.update(random.nextDouble() * 5, random.nextBoolean(), random.nextBoolean(),
                        phases[random.nextInt(phases.length)], heats[random.nextInt(heats.length)]);
                if (random.nextInt(50) == 0) {
                    s.eat(45);
                    s.drink(30);
                }
                SurvivalStatus status = s.status();
                for (Need need : Need.values()) {
                    double v = status.value(need);
                    assertTrue(v >= 0 && v <= 100, need + " out of bounds: " + v);
                }
            }
        }
    }

    @Test
    void sameStepsGiveTheSameResult() {
        Survival a = new Survival(P);
        Survival b = new Survival(P);
        run(a, 500, false, true, DayPhase.DUSK, Heat.NONE);
        run(b, 500, false, true, DayPhase.DUSK, Heat.NONE);
        assertEquals(a.status(), b.status());
    }
}
