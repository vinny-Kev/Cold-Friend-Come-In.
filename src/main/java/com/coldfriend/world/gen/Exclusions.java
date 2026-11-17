package com.coldfriend.world.gen;

import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.WorldPath.PathPoint;
import java.util.ArrayList;
import java.util.List;

/** Ground that must stay clear of scenery: building sites, the village square and paths. */
final class Exclusions {

    private record Rect(Footprint footprint, float margin) {
    }

    private record Circle(float x, float z, float radius) {
    }

    private record Corridor(List<PathPoint> points, float radius) {
    }

    private final List<Rect> rects = new ArrayList<>();
    private final List<Circle> circles = new ArrayList<>();
    private final List<Corridor> corridors = new ArrayList<>();

    void addRect(Footprint footprint, float margin) {
        rects.add(new Rect(footprint, margin));
    }

    void addCircle(float x, float z, float radius) {
        circles.add(new Circle(x, z, radius));
    }

    void addCorridor(List<PathPoint> points, float radius) {
        corridors.add(new Corridor(points, radius));
    }

    /** @return true if a circle of the given radius at this spot touches any excluded ground */
    boolean blocks(float x, float z, float radius) {
        for (Rect rect : rects) {
            if (rect.footprint().contains(x, z, rect.margin() + radius)) {
                return true;
            }
        }
        for (Circle circle : circles) {
            float dx = x - circle.x();
            float dz = z - circle.z();
            float limit = circle.radius() + radius;
            if (dx * dx + dz * dz < limit * limit) {
                return true;
            }
        }
        for (Corridor corridor : corridors) {
            if (distanceToPath(corridor.points(), x, z) < corridor.radius() + radius) {
                return true;
            }
        }
        return false;
    }

    /** @return the distance from a point to a polyline; infinity for an empty one */
    static float distanceToPath(List<PathPoint> points, float x, float z) {
        float best = Float.POSITIVE_INFINITY;
        for (int k = 0; k + 1 < points.size(); k++) {
            PathPoint a = points.get(k);
            PathPoint b = points.get(k + 1);
            best = Math.min(best, Maths.segmentDistance(x, z, a.x(), a.z(), b.x(), b.z()));
        }
        return best;
    }
}
