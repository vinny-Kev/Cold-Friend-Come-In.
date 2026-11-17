package com.coldfriend.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayOptionsTest {

    @Test
    void namedTimesAreStartTimesNotLightingModes() {
        assertEquals(12 * 60, PlayOptions.parseStartTime("day").getAsInt());
        assertEquals(18 * 60, PlayOptions.parseStartTime(" Dusk ").getAsInt());
        assertEquals(22 * 60, PlayOptions.parseStartTime("NIGHT").getAsInt());
    }

    @Test
    void clockTimesAreAccepted() {
        assertEquals(5 * 60 + 30, PlayOptions.parseStartTime("05:30").getAsInt());
        assertEquals(0, PlayOptions.parseStartTime("0:00").getAsInt());
        assertEquals(23 * 60 + 59, PlayOptions.parseStartTime("23:59").getAsInt());
    }

    @Test
    void anythingElseIsRejected() {
        assertTrue(PlayOptions.parseStartTime("noon").isEmpty());
        assertTrue(PlayOptions.parseStartTime("24:00").isEmpty());
        assertTrue(PlayOptions.parseStartTime("12:60").isEmpty());
        assertTrue(PlayOptions.parseStartTime("12").isEmpty());
        assertTrue(PlayOptions.parseStartTime("a:b").isEmpty());
    }

    @Test
    void aNormalGameStartsAtSeven() {
        assertEquals(7 * 60, PlayOptions.NORMAL.startMinute());
    }
}
