package com.coldfriend.creature;

import com.coldfriend.time.DayPhase;
import com.coldfriend.world.model.WaterMap;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.model.WorldPath.PathPoint;
import com.coldfriend.world.query.TerrainRules;
import com.coldfriend.world.query.WorldCollision;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Chooses what the creature does. Once a second it scores each mode from what it remembers (its memory, its own
 * position, the world and the time of day), keeps the current mode unless another is clearly better and the current
 * one has run its minimum time, and picks a place to go for that mode.
 *
 * <p>The brain never reads the player's state. Everything it knows about the player is in {@link Memory}: where they
 * were seen, when the creature believed it was watched, and the sounds and traces it heard. A player far away, or one
 * the creature never perceived, therefore cannot change what it does. A test enforces this.
 */
final class CreatureBrain {

    private static final int MODES = CreatureMode.values().length;

    private final WorldData world;
    private final WorldCollision collision;
    private final Territory territory;
    private final CreatureNavigator navigator;
    private final SplittableRandom random;
    private final double[] scores = new double[MODES];
    private CreatureMode mode = CreatureMode.ROAM;
    private double modeSeconds = Double.NEGATIVE_INFINITY;
    private double withdrawSeconds = Double.NEGATIVE_INFINITY;
    private boolean hasTarget;
    private float targetX;
    private float targetZ;
    private double targetSeconds;

    CreatureBrain(WorldData world, WorldCollision collision, Territory territory, CreatureNavigator navigator,
            SplittableRandom random) {
        this.world = world;
        this.collision = collision;
        this.territory = territory;
        this.navigator = navigator;
        this.random = random;
    }

    /**
     * One decision.
     *
     * @param creature the creature, whose mode this sets
     * @param memory what the creature remembers
     * @param phase the time of day
     * @param minute game time, in minutes
     */
    void think(CreatureState creature, Memory memory, DayPhase phase, double minute) {
        double now = minute * CreatureParameters.REAL_SECONDS_PER_GAME_MINUTE;
        scoreModes(creature, memory, phase, now);
        CreatureMode best = best();
        if (best != mode) {
            boolean watched = scores[CreatureMode.WITHDRAW.ordinal()] > 0;
            boolean committed = now - modeSeconds < commitSeconds(mode);
            boolean clearlyBetter = scores[best.ordinal()] > scores[mode.ordinal()] * CreatureParameters.HYSTERESIS;
            if ((watched && best == CreatureMode.WITHDRAW) || (!committed && clearlyBetter)) {
                switchTo(creature, best, now);
            }
        }
        if (hasTarget && mode == CreatureMode.ROAM && now - targetSeconds >= CreatureParameters.WANDER_TIMEOUT_SECONDS) {
            hasTarget = false;
        }
        if (hasTarget && navigator.blocked()) {
            // The route is stuck: search the same target again. If that fails, drop the target and decide afresh.
            if (!routeTo(creature, targetX, targetZ, mode == CreatureMode.ROAM)) {
                hasTarget = false;
            }
        } else if (mode == CreatureMode.ROAM && (!hasTarget || navigator.arrived())) {
            hasTarget = false;
            roam(creature, now);
        } else if (!hasTarget && mode != CreatureMode.HOLD && mode != CreatureMode.ROAM) {
            chooseTarget(creature, memory, now);
        }
    }

    /** @return the spot the creature is walking to, x, if it has one */
    float targetX() {
        return targetX;
    }

    /** @return the spot the creature is walking to, z, if it has one */
    float targetZ() {
        return targetZ;
    }

    /** Fills {@link #scores} from what the creature knows. Scores are in 0 to 1 and never negative. */
    private void scoreModes(CreatureState creature, Memory memory, DayPhase phase, double now) {
        PlayerTrace focus = memory.focus();
        double secondsPerMinute = CreatureParameters.REAL_SECONDS_PER_GAME_MINUTE;
        double nowMinute = now / secondsPerMinute;
        double sightAge = focus != null && focus.hasSeen() ? nowMinute - focus.lastSeenMinute() : Double.POSITIVE_INFINITY;
        double watchedAge = focus != null && !Double.isNaN(focus.watchedMinute())
                ? (nowMinute - focus.watchedMinute()) * secondsPerMinute : Double.POSITIVE_INFINITY;
        boolean watched = watchedAge <= CreatureParameters.WATCHED_WINDOW;
        boolean cooling = now - withdrawSeconds < CreatureParameters.WITHDRAW_COOLDOWN;
        double sightFresh = Math.max(0.0, 1.0 - sightAge / CreatureParameters.SIGHT_LIFETIME);
        boolean cabinInterest = focus != null && focus.hasSeen() && sightFresh > 0
                && distance(focus.lastSeenX(), focus.lastSeenZ(), cabinX(), cabinZ()) <= CreatureParameters.CABIN_INTEREST_RANGE;
        double evidence = strongestEvidence(memory, nowMinute);
        double night = switch (phase) {
            case NIGHT -> 1.0;
            case DAWN, DUSK -> 0.5;
            case DAY -> 0.2;
        };
        scores[CreatureMode.ROAM.ordinal()] = 0.35;
        scores[CreatureMode.HOLD.ordinal()] = 0.2;
        scores[CreatureMode.INVESTIGATE.ordinal()] = !watched && evidence >= CreatureParameters.EVIDENCE_THRESHOLD
                ? 0.1 + 0.8 * evidence : 0.0;
        scores[CreatureMode.STALK.ordinal()] = !watched && !cooling && sightFresh > 0 ? 0.7 * sightFresh : 0.0;
        scores[CreatureMode.OBSERVE.ordinal()] = !cooling && cabinInterest ? 0.5 * night : 0.0;
        scores[CreatureMode.CIRCLE.ordinal()] = !cooling && cabinInterest ? 0.4 : 0.0;
        scores[CreatureMode.WITHDRAW.ordinal()] = watched ? 1.0 : 0.0;
    }

    private CreatureMode best() {
        CreatureMode best = CreatureMode.ROAM;
        for (CreatureMode candidate : CreatureMode.values()) {
            if (scores[candidate.ordinal()] > scores[best.ordinal()]) {
                best = candidate;
            }
        }
        return best;
    }

    /** The strongest sound, track or change the creature still remembers, from 0 to 1. */
    private static double strongestEvidence(Memory memory, double minute) {
        double best = 0.0;
        for (int i = 0; i < memory.size(); i++) {
            Observation observation = memory.get(i);
            if (observation.kind() != ObservationKind.SIGHT) {
                best = Math.max(best, Memory.confidenceAt(observation, minute));
            }
        }
        return best;
    }

    private void switchTo(CreatureState creature, CreatureMode next, double now) {
        mode = next;
        modeSeconds = now;
        hasTarget = false;
        creature.setMode(next);
        if (next == CreatureMode.WITHDRAW) {
            withdrawSeconds = now;
        }
    }

    private static double commitSeconds(CreatureMode mode) {
        return switch (mode) {
            case ROAM -> CreatureParameters.COMMIT_ROAM;
            case INVESTIGATE -> CreatureParameters.COMMIT_INVESTIGATE;
            case OBSERVE -> CreatureParameters.COMMIT_OBSERVE;
            case STALK -> CreatureParameters.COMMIT_STALK;
            case CIRCLE -> CreatureParameters.COMMIT_CIRCLE;
            case HOLD -> CreatureParameters.COMMIT_HOLD;
            case WITHDRAW -> CreatureParameters.COMMIT_WITHDRAW;
        };
    }

    /** Picks a random spot to roam to. Roaming keeps away from the cabin and the village. */
    private void roam(CreatureState creature, double now) {
        for (int attempt = 0; attempt < CreatureParameters.WANDER_ATTEMPTS && !hasTarget; attempt++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double span = CreatureParameters.WANDER_MAX_DISTANCE - CreatureParameters.WANDER_MIN_DISTANCE;
            double distance = CreatureParameters.WANDER_MIN_DISTANCE + random.nextDouble() * span;
            float x = creature.x() + (float) (Math.sin(angle) * distance);
            float z = creature.z() + (float) (Math.cos(angle) * distance);
            if (territory.standable(x, z) && territory.targetAllowed(x, z) && routeTo(creature, x, z, true)) {
                targetX = x;
                targetZ = z;
                targetSeconds = now;
                hasTarget = true;
            }
        }
    }

    /** Picks a target for a mode other than roaming or holding. Gives up quietly if none is found. */
    private void chooseTarget(CreatureState creature, Memory memory, double now) {
        PlayerTrace focus = memory.focus();
        boolean seen = focus != null && focus.hasSeen();
        for (int attempt = 0; attempt < CreatureParameters.TARGET_ATTEMPTS && !hasTarget; attempt++) {
            double bearing = random.nextDouble() * 2.0 * Math.PI;
            float[] spot = switch (mode) {
                case INVESTIGATE -> evidenceSpot(memory, now / CreatureParameters.REAL_SECONDS_PER_GAME_MINUTE);
                case STALK -> seen ? around(focus.lastSeenX(), focus.lastSeenZ(), CreatureParameters.STALK_DISTANCE,
                        bearing) : null;
                case OBSERVE -> cabinSpot((float) (random.nextDouble() * (CreatureParameters.OBSERVE_MAX_CABIN
                        - CreatureParameters.OBSERVE_MIN_CABIN) + CreatureParameters.OBSERVE_MIN_CABIN), bearing);
                case CIRCLE -> cabinSpot(CreatureParameters.CIRCLE_RADIUS, bearing);
                case WITHDRAW -> withdrawSpot(creature, focus, bearing);
                default -> null;
            };
            if (spot == null) {
                continue;
            }
            float x = spot[0];
            float z = spot[1];
            if (!territory.standable(x, z) || !suitable(x, z, seen, focus)) {
                continue;
            }
            if (routeTo(creature, x, z, false)) {
                targetX = x;
                targetZ = z;
                targetSeconds = now;
                hasTarget = true;
            }
        }
    }

    /**
     * Whether a spot suits the current mode. Investigating needs nothing. Stalking, circling and withdrawing need cover
     * from where the player was last seen. Observing needs a view of the cabin and cover from the player.
     */
    private boolean suitable(float x, float z, boolean seen, PlayerTrace focus) {
        boolean hiddenFromPlayer = !seen || !collision.sightClear(x, z, focus.lastSeenX(), focus.lastSeenZ(),
                CreatureParameters.SIGHT_RADIUS);
        return switch (mode) {
            case INVESTIGATE -> true;
            case WITHDRAW -> true;
            case STALK, CIRCLE -> hiddenFromPlayer;
            case OBSERVE -> hiddenFromPlayer && collision.sightClear(x, z, cabinX(), cabinZ(),
                    CreatureParameters.SIGHT_RADIUS);
            default -> false;
        };
    }

    private float[] evidenceSpot(Memory memory, double minute) {
        float bestX = 0f;
        float bestZ = 0f;
        float best = 0f;
        for (int i = 0; i < memory.size(); i++) {
            Observation observation = memory.get(i);
            float confidence = Memory.confidenceAt(observation, minute);
            if (observation.kind() != ObservationKind.SIGHT && confidence > best
                    && confidence >= CreatureParameters.EVIDENCE_THRESHOLD) {
                best = confidence;
                bestX = observation.x();
                bestZ = observation.z();
            }
        }
        return best > 0f ? new float[] {bestX, bestZ} : null;
    }

    /** A spot at a distance from the cabin, in a direction. */
    private float[] cabinSpot(float distance, double bearing) {
        return around(cabinX(), cabinZ(), distance, bearing);
    }

    /** A spot away from where the player was last seen, past the creature: out of sight, moving off. */
    private float[] withdrawSpot(CreatureState creature, PlayerTrace focus, double bearing) {
        float fromX = focus != null && focus.hasSeen() ? focus.lastSeenX() : creature.x();
        float fromZ = focus != null && focus.hasSeen() ? focus.lastSeenZ() : creature.z();
        float dx = creature.x() - fromX;
        float dz = creature.z() - fromZ;
        double away = dx == 0f && dz == 0f ? bearing : Math.atan2(dx, dz) + (bearing - Math.PI) * 0.3;
        return around(fromX, fromZ, CreatureParameters.WITHDRAW_DISTANCE, away);
    }

    private static float[] around(float x, float z, float distance, double bearing) {
        return new float[] {x + (float) (Math.sin(bearing) * distance), z + (float) (Math.cos(bearing) * distance)};
    }

    /** Searches a route and follows it. Roaming routes must keep away from the cabin and the village. */
    private boolean routeTo(CreatureState creature, float x, float z, boolean roaming) {
        List<PathPoint> route = navigator.search(creature.x(), creature.z(), x, z);
        if (route.isEmpty() || (roaming && !territory.routeAllowed(route))) {
            return false;
        }
        navigator.follow(route);
        return true;
    }

    private float cabinX() {
        return world.cabin().footprint().x();
    }

    private float cabinZ() {
        return world.cabin().footprint().z();
    }

    private static float distance(float ax, float az, float bx, float bz) {
        float dx = ax - bx;
        float dz = az - bz;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}
