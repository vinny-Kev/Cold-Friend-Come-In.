package com.coldfriend.world.model;

import java.util.List;

/**
 * A walkable route across the ground.
 *
 * @param id stable identifier
 * @param kind what the path joins
 * @param points the centre line, in order
 * @param width path width in metres
 */
public record WorldPath(String id, PathKind kind, List<PathPoint> points, float width) {

    /** Defensive copy so the path cannot change after generation. */
    public WorldPath {
        points = List.copyOf(points);
    }

    /**
     * One point on a path.
     *
     * @param x world x in metres
     * @param z world z in metres
     */
    public record PathPoint(float x, float z) {
    }
}
