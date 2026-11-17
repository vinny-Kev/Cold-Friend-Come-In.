package com.coldfriend.world.gen;

import com.coldfriend.world.model.River;
import com.coldfriend.world.model.River.RiverPoint;
import java.util.List;

/** Exact distance from a point to the nearest water, over any distance. Slow per query; used for siting only. */
final class RiverProximity {

    private final List<River> rivers;

    RiverProximity(List<River> rivers) {
        this.rivers = rivers;
    }

    /** @return the distance to the nearest water's edge in metres; 0 if the point is in a river */
    float edgeDistance(float x, float z) {
        float best = Float.POSITIVE_INFINITY;
        for (River river : rivers) {
            List<RiverPoint> points = river.points();
            for (int k = 0; k + 1 < points.size(); k++) {
                RiverPoint a = points.get(k);
                RiverPoint b = points.get(k + 1);
                float d = Maths.segmentDistance(x, z, a.x(), a.z(), b.x(), b.z());
                best = Math.min(best, d - Math.max(a.halfWidth(), b.halfWidth()));
            }
        }
        return Math.max(0f, best);
    }
}
