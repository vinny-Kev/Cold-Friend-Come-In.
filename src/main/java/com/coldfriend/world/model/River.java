package com.coldfriend.world.model;

import java.util.List;

/**
 * One river reach, running downstream from its first point to its last. Reaches join at confluences, where
 * the last point of one reach is also a point of the reach it flows into.
 *
 * @param points the centre line, upstream to downstream
 */
public record River(List<RiverPoint> points) {

    /** Defensive copy so the river cannot change after generation. */
    public River {
        points = List.copyOf(points);
    }

    /**
     * One point on a river's centre line.
     *
     * @param x world x in metres
     * @param z world z in metres
     * @param level water surface height in metres; never rises downstream
     * @param halfWidth half the water width in metres
     */
    public record RiverPoint(float x, float z, float level, float halfWidth) {
    }

    /** @return the downstream end of the reach */
    public RiverPoint mouth() {
        return points.get(points.size() - 1);
    }

    /** @return the upstream end of the reach */
    public RiverPoint source() {
        return points.get(0);
    }
}
