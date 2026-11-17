package com.coldfriend.creature;

import com.coldfriend.player.PlayerId;
import java.util.HashMap;
import java.util.Map;

/**
 * What the creature has noticed, and when. Only {@link Perception} adds to it; the creature's decisions only read it.
 *
 * <p>Confidence falls in a straight line with age: {@code start * (1 - age / lifetime)}, reaching zero at the
 * lifetime. An observation that is refreshed (seen again nearby) restarts its clock. Two observations of the same
 * kind within {@link CreatureParameters#MERGE_RADIUS} become one. Wrong information is kept until it decays, not
 * deleted, so the creature can be fooled.
 */
public final class Memory {

    private final Observation[] slots = new Observation[CreatureParameters.MEMORY_CAPACITY];
    private int count;
    private final Map<PlayerId, PlayerTrace> players = new HashMap<>();

    /**
     * Notes something noticed now.
     *
     * @param kind what was noticed
     * @param x approximate x, in metres
     * @param z approximate z, in metres
     * @param minute game time, in minutes
     * @param confidence how sure, from 0 to 1, when first made
     */
    public void note(ObservationKind kind, float x, float z, double minute, float confidence) {
        for (int i = 0; i < count; i++) {
            Observation old = slots[i];
            if (old.kind() == kind && distance(old.x(), old.z(), x, z) <= CreatureParameters.MERGE_RADIUS) {
                boolean better = confidence >= confidenceAt(old, minute);
                slots[i] = better ? new Observation(kind, x, z, minute, confidence)
                        : new Observation(kind, old.x(), old.z(), minute, old.startConfidence());
                return;
            }
        }
        if (count < slots.length) {
            slots[count++] = new Observation(kind, x, z, minute, confidence);
            return;
        }
        int weakest = 0;
        for (int i = 1; i < count; i++) {
            if (confidenceAt(slots[i], minute) < confidenceAt(slots[weakest], minute)) {
                weakest = i;
            }
        }
        slots[weakest] = new Observation(kind, x, z, minute, confidence);
    }

    /**
     * Records that the creature saw the player.
     *
     * @param player who was seen
     * @param x where, x
     * @param z where, z
     * @param minute game time
     */
    void sawPlayer(PlayerId player, float x, float z, double minute) {
        trace(player).sawAt(x, z, minute);
    }

    /**
     * Records that the creature believed the player was watching it.
     *
     * @param player who was watching
     * @param minute game time
     */
    void believedWatched(PlayerId player, double minute) {
        trace(player).watchedAt(minute);
    }

    /** @return what the creature remembers about a player; never null (empty if it never saw them) */
    public PlayerTrace trace(PlayerId player) {
        return players.computeIfAbsent(player, id -> new PlayerTrace());
    }

    /**
     * Drops observations that have decayed to nothing. Called on each brain tick.
     *
     * @param minute game time now
     */
    public void forgetExpired(double minute) {
        int kept = 0;
        for (int i = 0; i < count; i++) {
            if (confidenceAt(slots[i], minute) > 0f) {
                slots[kept++] = slots[i];
            }
        }
        for (int i = kept; i < count; i++) {
            slots[i] = null;
        }
        count = kept;
    }

    /** @return how many observations are held */
    public int size() {
        return count;
    }

    /**
     * @param index which observation, from 0 to {@link #size()} minus one
     * @return that observation
     */
    public Observation get(int index) {
        return slots[index];
    }

    /**
     * @param observation one of the held observations
     * @param minute game time now
     * @return how sure the creature is of it now, from 0 to its start confidence
     */
    public static float confidenceAt(Observation observation, double minute) {
        double age = minute - observation.minute();
        double fraction = 1.0 - age / observation.kind().lifetime();
        return (float) (observation.startConfidence() * Math.max(0.0, Math.min(1.0, fraction)));
    }

    private static float distance(float ax, float az, float bx, float bz) {
        float dx = ax - bx;
        float dz = az - bz;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}
