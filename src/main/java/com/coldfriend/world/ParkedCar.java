package com.coldfriend.world;

import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.TerrainRules;
import com.coldfriend.world.query.WorldCollision;
import java.util.Optional;

/**
 * A car parked beside the cabin, facing the same way as the cabin. Its body is solid: it is three round obstacles
 * along its length, so a person cannot walk through it. The spot is chosen from the cabin's own layout, so it is the
 * same for the same seed. It cannot be driven yet.
 *
 * @param x world x of the car's centre, in metres
 * @param z world z of the car's centre, in metres
 * @param yaw the direction the car's length runs in; the cabin's yaw
 */
public record ParkedCar(float x, float z, float yaw) {

    /** Half the car's length and width, in metres. */
    public static final float HALF_LENGTH = 2.1f;
    public static final float HALF_WIDTH = 0.9f;
    /** Radius of each round obstacle that makes up the car's body, in metres. */
    public static final float RADIUS = 1.1f;
    /** Gap between the cabin wall and the car, in metres. */
    private static final float SIDE_GAP = 2.0f;
    /** Spots along the cabin wall to try, forward of the cabin's centre, in metres. */
    private static final float[] FORWARD_OFFSETS = {0f, -2f, 2f, -4f, 4f};

    /** @return the centres of the round obstacles that make up the car, as x, z pairs flattened to one array */
    public float[] obstacleCentres() {
        return new float[] {x - (float) Math.sin(yaw) * (HALF_LENGTH - RADIUS), z - (float) Math.cos(yaw) * (HALF_LENGTH - RADIUS),
            x, z, x + (float) Math.sin(yaw) * (HALF_LENGTH - RADIUS), z + (float) Math.cos(yaw) * (HALF_LENGTH - RADIUS)};
    }

    /**
     * Finds a spot beside the cabin where the car fits on dry, walkable ground, clear of buildings and trees.
     *
     * @return the spot, or empty if no spot near the cabin is clear
     */
    public static Optional<ParkedCar> find(WorldData world, WorldCollision collision) {
        Footprint cabin = world.cabin().footprint();
        for (float side : new float[] {cabin.halfWidth() + SIDE_GAP, -(cabin.halfWidth() + SIDE_GAP)}) {
            for (float forward : FORWARD_OFFSETS) {
                float[] centre = cabin.toWorld(side, forward);
                ParkedCar car = new ParkedCar(centre[0], centre[1], cabin.yaw());
                if (car.fits(world, collision)) {
                    return Optional.of(car);
                }
            }
        }
        return Optional.empty();
    }

    private boolean fits(WorldData world, WorldCollision collision) {
        WaterMap water = world.water();
        float[] centres = obstacleCentres();
        for (int i = 0; i < centres.length; i += 2) {
            float cx = centres[i];
            float cz = centres[i + 1];
            if (!TerrainRules.walkable(world.terrain(), water, cx, cz) || water.depthAt(world.terrain(), cx, cz) > 0.05f
                    || !collision.canStandAt(cx, cz, RADIUS)) {
                return false;
            }
        }
        return true;
    }
}
