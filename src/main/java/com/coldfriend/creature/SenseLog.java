package com.coldfriend.creature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The sense events since the creature last listened. The simulation adds to it between and during steps; the
 * creature reads it on its next step; the simulation clears it at the end of that step. Bounded, so a burst of
 * actions cannot grow it without limit: events past the capacity are counted and dropped.
 */
public final class SenseLog {

    /** Most events kept between two steps. */
    public static final int CAPACITY = 16;

    private final List<SenseEvent> events = new ArrayList<>(CAPACITY);
    private long dropped;
    private long raised;

    /** @param event what happened; dropped and counted if the log is full */
    public void add(SenseEvent event) {
        raised++;
        if (events.size() < CAPACITY) {
            events.add(event);
        } else {
            dropped++;
        }
    }

    /** @return the events waiting to be heard, oldest first; read-only */
    public List<SenseEvent> events() {
        return Collections.unmodifiableList(events);
    }

    /** Forgets every event. The simulation calls this at the end of each step. */
    public void clear() {
        events.clear();
    }

    /** @return how many events have ever been added, kept or dropped */
    public long raised() {
        return raised;
    }

    /** @return how many events were dropped because the log was full */
    public long dropped() {
        return dropped;
    }
}
