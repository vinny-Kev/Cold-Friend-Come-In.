package com.coldfriend.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StepCounterTest {

    @Test
    void noStepUntilOneStrideIsWalked() {
        StepCounter counter = new StepCounter(2f);

        assertFalse(counter.advance(0.5f));
        assertFalse(counter.advance(0.5f));
        assertTrue(counter.advance(1f));
    }

    @Test
    void leftoverDistanceCarriesIntoTheNextStep() {
        StepCounter counter = new StepCounter(2f);

        assertTrue(counter.advance(2.5f));
        // 0.5 m is left over, so 0.4 m more is not yet a full 2 m stride.
        assertFalse(counter.advance(0.4f));
        assertTrue(counter.advance(1.1f));
    }

    @Test
    void negativeDistanceIsIgnored() {
        StepCounter counter = new StepCounter(2f);

        assertFalse(counter.advance(-5f));
        assertFalse(counter.advance(1f));
    }

    @Test
    void resetForgetsDistanceAlreadyWalked() {
        StepCounter counter = new StepCounter(2f);
        counter.advance(1.9f);

        counter.reset();

        assertFalse(counter.advance(1f));
    }

    @Test
    void strideMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new StepCounter(0f));
        assertEquals(true, new StepCounter(0.1f).advance(0.2f));
    }
}
