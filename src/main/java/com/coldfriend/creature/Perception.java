package com.coldfriend.creature;

import com.coldfriend.player.PlayerState;
import com.coldfriend.time.DayPhase;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;
import java.util.List;
import java.util.SplittableRandom;

/**
 * How the creature senses the world: sight and hearing. This is the only class in the creature package that reads the
 * player's state. It turns what it senses into {@link Observation}s in {@link Memory}, and returns the sight facts.
 * The creature's decisions never see the player's state, only what is in memory (see the information barrier test).
 */
public final class Perception {

    private static final float DEG = (float) (Math.PI / 180.0);

    private final HeightField terrain;
    private final WorldCollision collision;
    private final List<Footprint> walls;
    private final SplittableRandom jitter;

    /**
     * @param world the generated world (ground and buildings)
     * @param collision scenery and buildings that block sight
     * @param jitter random stream for the error in heard positions
     */
    public Perception(WorldData world, WorldCollision collision, SplittableRandom jitter) {
        this.terrain = world.terrain();
        this.collision = collision;
        this.walls = world.footprints();
        this.jitter = jitter;
    }

    /**
     * Looks at one player: records what the creature sees, and whether it thinks it is being watched.
     *
     * @param creature the creature doing the looking; its heading decides its cone
     * @param player the player being looked at
     * @param phase the time of day, which sets the ranges
     * @param memory where observations are recorded
     * @param minute game time, in minutes
     * @return the sight facts
     */
    public Sighting look(CreatureState creature, PlayerState player, DayPhase phase, Memory memory, double minute) {
        float creatureEye = terrain.heightAt(creature.x(), creature.z()) + CreatureParameters.CREATURE_EYE_HEIGHT;
        float playerEye = player.y() + CreatureParameters.PLAYER_EYE_HEIGHT;
        float dx = player.x() - creature.x();
        float dy = playerEye - creatureEye;
        float dz = player.z() - creature.z();
        float ground = (float) Math.sqrt(dx * dx + dz * dz);
        float distance = (float) Math.sqrt(ground * ground + dy * dy);

        boolean blocked = blocked(creature.x(), creature.z(), creatureEye, player.x(), player.z(), playerEye);
        boolean creatureSees = !blocked && distance <= creatureRange(phase)
                && inCone(creature.heading(), 0f, dx, dy, dz, ground, CreatureParameters.CREATURE_HALF_ANGLE,
                        CreatureParameters.CREATURE_HALF_ELEVATION);
        // The player's view of the creature: the same geometry, seen from the player's eyes looking the other way.
        boolean playerSees = !blocked && distance <= playerRange(phase)
                && inCone(player.yaw(), player.pitch(), -dx, -dy, -dz, ground, CreatureParameters.PLAYER_HALF_ANGLE,
                        CreatureParameters.PLAYER_HALF_ELEVATION);
        boolean believesWatched = creatureSees && distance <= CreatureParameters.WATCHED_RANGE && playerSees;

        if (creatureSees) {
            memory.note(ObservationKind.SIGHT, player.x(), player.z(), minute, CreatureParameters.SIGHT_CONFIDENCE);
            memory.sawPlayer(player.id(), player.x(), player.z(), minute);
        }
        if (believesWatched) {
            memory.believedWatched(player.id(), minute);
        }
        return new Sighting(playerSees, creatureSees, blocked, believesWatched);
    }

    /**
     * Hears everything that happened since the last step. Sound gives only an approximate place, and walls cut it.
     * A footstep the creature can see on the ground also leaves a track observation.
     *
     * @param creature the listening creature
     * @param senses what the player did, this step
     * @param phase the time of day, which sets how far it can see footprints
     * @param memory where observations are recorded
     */
    public void hear(CreatureState creature, List<SenseEvent> senses, DayPhase phase, Memory memory) {
        for (SenseEvent sense : senses) {
            float dx = sense.x() - creature.x();
            float dz = sense.z() - creature.z();
            float distance = (float) Math.sqrt(dx * dx + dz * dz);
            if (distance <= sense.kind().loudness()) {
                float loudness = sense.kind().loudness() * (float) Math.pow(CreatureParameters.WALL_FACTOR,
                        wallsBetween(sense.x(), sense.z(), creature.x(), creature.z()));
                if (distance <= loudness) {
                    heard(sense, distance, loudness, memory);
                }
            }
            if (isFootstep(sense) && distance <= creatureRange(phase) && collision.sightClear(creature.x(),
                    creature.z(), sense.x(), sense.z(), CreatureParameters.SIGHT_RADIUS)
                    && inCone(creature.heading(), 0f, dx, 0f, dz, distance, CreatureParameters.CREATURE_HALF_ANGLE,
                            CreatureParameters.CREATURE_HALF_ELEVATION)) {
                memory.note(ObservationKind.TRACK, sense.x(), sense.z(), sense.minute(),
                        CreatureParameters.TRACK_CONFIDENCE);
            }
        }
    }

    private void heard(SenseEvent sense, float distance, float loudness, Memory memory) {
        float error = CreatureParameters.HEARING_JITTER;
        float x = sense.x() + (float) ((jitter.nextDouble() * 2 - 1) * error);
        float z = sense.z() + (float) ((jitter.nextDouble() * 2 - 1) * error);
        switch (sense.kind()) {
            case DROP, PLACE, PICK_UP -> memory.note(ObservationKind.CHANGED, x, z, sense.minute(),
                    CreatureParameters.CHANGED_CONFIDENCE);
            default -> {
                float confidence = clamp(1f - distance / loudness, CreatureParameters.HEARING_FLOOR,
                        CreatureParameters.HEARING_CAP);
                memory.note(ObservationKind.SOUND, x, z, sense.minute(), confidence);
            }
        }
    }

    private static boolean isFootstep(SenseEvent sense) {
        return sense.kind() == SenseKind.FOOTSTEP_WALK || sense.kind() == SenseKind.FOOTSTEP_SPRINT;
    }

    /** @return true if the line from one eye to the other is cut by scenery or by the ground */
    private boolean blocked(float ax, float az, float ay, float bx, float bz, float by) {
        return !collision.sightClear(ax, az, bx, bz, CreatureParameters.SIGHT_RADIUS)
                || terrainBlocks(ax, az, ay, bx, bz, by);
    }

    /** Samples the ground along a sight line; a hill between the two eyes cuts the view. */
    private boolean terrainBlocks(float ax, float az, float ay, float bx, float bz, float by) {
        float dx = bx - ax;
        float dz = bz - az;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        int steps = Math.min(CreatureParameters.TERRAIN_MAX_SAMPLES,
                Math.max(1, (int) Math.ceil(length / CreatureParameters.TERRAIN_SAMPLE_STEP)));
        for (int k = 1; k < steps; k++) {
            float t = (float) k / steps;
            float x = ax + dx * t;
            float z = az + dz * t;
            float rayHeight = ay + (by - ay) * t;
            if (terrain.heightAt(x, z) > rayHeight + CreatureParameters.TERRAIN_MARGIN) {
                return true;
            }
        }
        return false;
    }

    /** @return how many distinct solid buildings the straight line between two points passes through */
    private int wallsBetween(float ax, float az, float bx, float bz) {
        float dx = bx - ax;
        float dz = bz - az;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(length / CreatureParameters.WALL_SAMPLE_STEP));
        boolean[] crossed = new boolean[walls.size()];
        for (int k = 0; k <= steps; k++) {
            float t = (float) k / steps;
            float x = ax + dx * t;
            float z = az + dz * t;
            for (int w = 0; w < walls.size(); w++) {
                if (walls.get(w).contains(x, z, 0f)) {
                    crossed[w] = true;
                }
            }
        }
        int count = 0;
        for (boolean hit : crossed) {
            if (hit) {
                count++;
            }
        }
        return count;
    }

    /**
     * Whether a point lies inside a cone: within {@code halfAngle} degrees of a heading (left or right) and within
     * {@code halfElevation} degrees of a level line.
     *
     * @param yaw the heading the cone faces; 0 is +z, positive turns toward +x
     * @param pitch the level the cone faces; positive looks up
     * @param dx point offset, x
     * @param dy point offset, height
     * @param dz point offset, z
     * @param ground horizontal distance to the point
     */
    static boolean inCone(float yaw, float pitch, float dx, float dy, float dz, float ground, float halfAngle,
            float halfElevation) {
        if (ground <= 0f) {
            return false;
        }
        float bearing = (float) Math.atan2(dx, dz);
        float turn = Math.abs(wrap(bearing - yaw));
        float elevation = (float) Math.atan2(dy, ground);
        return turn <= halfAngle * DEG && Math.abs(elevation - pitch) <= halfElevation * DEG;
    }

    /** Wraps an angle into the range minus pi to pi. */
    private static float wrap(float angle) {
        float a = angle;
        while (a > Math.PI) {
            a -= 2f * (float) Math.PI;
        }
        while (a < -Math.PI) {
            a += 2f * (float) Math.PI;
        }
        return a;
    }

    static float creatureRange(DayPhase phase) {
        return switch (phase) {
            case DAY -> CreatureParameters.CREATURE_RANGE_DAY;
            case DAWN, DUSK -> CreatureParameters.CREATURE_RANGE_TWILIGHT;
            case NIGHT -> CreatureParameters.CREATURE_RANGE_NIGHT;
        };
    }

    static float playerRange(DayPhase phase) {
        return switch (phase) {
            case DAY -> CreatureParameters.PLAYER_RANGE_DAY;
            case DAWN, DUSK -> CreatureParameters.PLAYER_RANGE_TWILIGHT;
            case NIGHT -> CreatureParameters.PLAYER_RANGE_NIGHT;
        };
    }

    private static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }
}
