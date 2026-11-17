package com.coldfriend.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WindLoopTest {

    private static final int RATE = 22050;

    @Test
    void lengthMatchesTheRequestedLoop() {
        short[] loop = WindLoop.generate(RATE, 2.0, 1L);

        assertEquals(2 * RATE, loop.length);
    }

    @Test
    void sameSeedGivesTheSameLoop() {
        assertArrayEquals(WindLoop.generate(RATE, 1.0, 7L), WindLoop.generate(RATE, 1.0, 7L));
    }

    @Test
    void loopIsAudibleButNotClipped() {
        short[] loop = WindLoop.generate(RATE, 1.0, 3L);
        int peak = 0;
        for (short s : loop) {
            peak = Math.max(peak, Math.abs(s));
        }

        assertTrue(peak > Short.MAX_VALUE / 4, "too quiet: " + peak);
        assertTrue(peak <= Short.MAX_VALUE * 0.61, "too loud: " + peak);
    }

    @Test
    void windDiesAwayBetweenGusts() {
        short[] loop = WindLoop.generate(RATE, 16.0, 9L);
        int window = RATE / 4;
        double loudest = 0;
        double quietest = Double.MAX_VALUE;
        for (int start = 0; start + window <= loop.length; start += window) {
            double sum = 0;
            for (int i = start; i < start + window; i++) {
                sum += (double) loop[i] * loop[i];
            }
            double rms = Math.sqrt(sum / window);
            loudest = Math.max(loudest, rms);
            quietest = Math.min(quietest, rms);
        }

        assertTrue(quietest < loudest * 0.05, "no lull: quietest " + quietest + " loudest " + loudest);
    }

    @Test
    void joinAtTheLoopEndIsSmooth() {
        short[] loop = WindLoop.generate(RATE, 1.0, 5L);
        int largestStep = 0;
        for (int i = 1; i < loop.length; i++) {
            largestStep = Math.max(largestStep, Math.abs(loop[i] - loop[i - 1]));
        }
        int seamStep = Math.abs(loop[0] - loop[loop.length - 1]);

        assertTrue(seamStep <= largestStep, "seam jump " + seamStep + " is bigger than normal steps " + largestStep);
    }
}
