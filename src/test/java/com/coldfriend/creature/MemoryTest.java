package com.coldfriend.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.player.PlayerId;
import org.junit.jupiter.api.Test;

class MemoryTest {

    private static final double EPS = 1e-6;

    @Test
    void confidenceFallsInAStraightLineToZeroAtTheLifetime() {
        Memory memory = new Memory();
        memory.note(ObservationKind.SIGHT, 0f, 0f, 0.0, 1.0f);
        Observation sight = memory.get(0);
        // Sight lasts 4 game minutes.
        assertEquals(1.0, Memory.confidenceAt(sight, 0.0), EPS);
        assertEquals(0.5, Memory.confidenceAt(sight, 2.0), EPS);
        assertEquals(0.25, Memory.confidenceAt(sight, 3.0), EPS);
        assertEquals(0.0, Memory.confidenceAt(sight, 4.0), EPS);
        assertEquals(0.0, Memory.confidenceAt(sight, 9.0), EPS, "never negative");
    }

    @Test
    void expiredObservationsAreForgotten() {
        Memory memory = new Memory();
        memory.note(ObservationKind.SOUND, 0f, 0f, 0.0, 0.8f);
        memory.forgetExpired(2.9);
        assertEquals(1, memory.size(), "sound lasts 3 minutes");
        memory.forgetExpired(3.0);
        assertEquals(0, memory.size());
    }

    @Test
    void theSameKindNearbyMergesAndRefreshesTheClock() {
        Memory memory = new Memory();
        memory.note(ObservationKind.SOUND, 0f, 0f, 0.0, 0.5f);
        // By minute 1 the first sound has decayed to 1/3 of its strength, so a report of 0.2 is weaker than it.
        memory.note(ObservationKind.SOUND, 3f, 0f, 1.0, 0.2f);
        assertEquals(1, memory.size());
        Observation kept = memory.get(0);
        assertEquals(0f, kept.x(), 0f, "the weaker report does not move it");
        assertEquals(1.0, kept.minute(), EPS, "but it is fresher");
        assertEquals(0.5f, kept.startConfidence(), 0f);
    }

    @Test
    void aStrongerReportReplacesTheOldOne() {
        Memory memory = new Memory();
        memory.note(ObservationKind.SOUND, 0f, 0f, 0.0, 0.4f);
        memory.note(ObservationKind.SOUND, 5f, 0f, 1.0, 0.8f);
        assertEquals(1, memory.size());
        assertEquals(5f, memory.get(0).x(), 0f);
        assertEquals(0.8f, memory.get(0).startConfidence(), 0f);
    }

    @Test
    void differentKindsDoNotMerge() {
        Memory memory = new Memory();
        memory.note(ObservationKind.SOUND, 0f, 0f, 0.0, 0.5f);
        memory.note(ObservationKind.CHANGED, 0f, 0f, 0.0, 0.5f);
        assertEquals(2, memory.size());
    }

    @Test
    void farApartSoundsStayApart() {
        Memory memory = new Memory();
        memory.note(ObservationKind.SOUND, 0f, 0f, 0.0, 0.5f);
        memory.note(ObservationKind.SOUND, 20f, 0f, 0.0, 0.5f);
        assertEquals(2, memory.size());
    }

    @Test
    void theLeastConfidentObservationIsEvictedWhenFull() {
        Memory memory = new Memory();
        memory.note(ObservationKind.SOUND, 0f, 0f, 0.0, 0.3f);
        for (int i = 1; i < CreatureParameters.MEMORY_CAPACITY; i++) {
            memory.note(ObservationKind.CHANGED, i * 100f, 0f, 0.0, 0.5f);
        }
        assertEquals(CreatureParameters.MEMORY_CAPACITY, memory.size());
        memory.note(ObservationKind.CHANGED, 9_000f, 0f, 0.0, 0.5f);
        assertEquals(CreatureParameters.MEMORY_CAPACITY, memory.size());
        for (int i = 0; i < memory.size(); i++) {
            assertFalse(memory.get(i).kind() == ObservationKind.SOUND, "the weakest sound was evicted");
        }
    }

    @Test
    void aSightingIsRememberedWithTheWhenAndTheWhere() {
        Memory memory = new Memory();
        PlayerTrace trace = memory.trace(PlayerId.LOCAL);
        assertFalse(trace.hasSeen());
        assertTrue(Double.isNaN(trace.watchedMinute()));

        memory.sawPlayer(PlayerId.LOCAL, 12f, -4f, 7.5);
        memory.believedWatched(PlayerId.LOCAL, 8.0);

        assertTrue(trace.hasSeen());
        assertEquals(12f, trace.lastSeenX(), 0f);
        assertEquals(-4f, trace.lastSeenZ(), 0f);
        assertEquals(7.5, trace.lastSeenMinute(), EPS);
        assertEquals(8.0, trace.watchedMinute(), EPS);
    }
}
