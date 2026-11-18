package com.coldfriend.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.item.ItemCatalog;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Phase C: the creature's modes, chosen from what it remembers. */
class CreaturePhaseCTest {

    private static final float STEP = 0.05f;
    private static WorldData world;

    @BeforeAll
    static void setUp() {
        world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
    }

    private static WorldSimulation fresh() {
        return new WorldSimulation(world, ItemCatalog.empty());
    }

    private static void run(WorldSimulation sim, double seconds) {
        int steps = (int) Math.round(seconds / STEP);
        for (int i = 0; i < steps; i++) {
            sim.update(STEP, Map.of());
        }
    }

    /** Runs and records every mode the creature is in at each decision (once a second). */
    private static Set<CreatureMode> modesOver(WorldSimulation sim, double seconds) {
        Set<CreatureMode> seen = EnumSet.noneOf(CreatureMode.class);
        int steps = (int) Math.round(seconds / STEP);
        for (int i = 0; i < steps; i++) {
            sim.update(STEP, Map.of());
            if (i % 20 == 0) {
                seen.add(sim.creatureStatus().orElseThrow().mode());
            }
        }
        return seen;
    }

    /** A standable spot at about the given distance from a point, in a direction; null if none is found. */
    private static float[] standableNear(WorldCollision collision, float x, float z, double bearing, double distance) {
        for (double d = distance; d < distance + 15; d += 1) {
            float px = x + (float) (Math.sin(bearing) * d);
            float pz = z + (float) (Math.cos(bearing) * d);
            if (collision.canStandAt(px, pz, 0.4f) && !world.water().isWaterAt(px, pz)) {
                return new float[] {px, pz, world.terrain().heightAt(px, pz)};
            }
        }
        return null;
    }

    private static double distance(float ax, float az, float bx, float bz) {
        return Math.hypot(ax - bx, az - bz);
    }

    @Test
    void freshEvidenceSendsTheCreatureToInvestigate() {
        WorldSimulation sim = fresh();
        CreatureSimulation creature = sim.creature().orElseThrow();
        CreatureStatus start = sim.creatureStatus().orElseThrow();
        float[] spot = standableNear(new WorldCollision(world), start.x(), start.z(), start.heading(), 40);
        Assumptions.assumeTrue(spot != null, "no standable spot to put the sound at");
        creature.memory().note(ObservationKind.SOUND, spot[0], spot[1], sim.minuteOfDay(), 0.9f);

        Set<CreatureMode> modes = modesOver(sim, 20);

        assertTrue(modes.contains(CreatureMode.INVESTIGATE), "modes seen: " + modes);
        CreatureStatus later = sim.creatureStatus().orElseThrow();
        assertTrue(distance(later.x(), later.z(), spot[0], spot[1]) < distance(start.x(), start.z(), spot[0], spot[1]) - 10,
                "the creature should have walked toward the evidence");
    }

    @Test
    void seenButNotWatchedSendsTheCreatureToStalkFromCover() {
        WorldSimulation sim = fresh();
        CreatureSimulation creature = sim.creature().orElseThrow();
        CreatureStatus start = sim.creatureStatus().orElseThrow();
        WorldCollision collision = new WorldCollision(world);
        // The player 20 m ahead of the creature, looking away from it: visible, but not watching it.
        float[] spot = standableNear(collision, start.x(), start.z(), start.heading(), 20);
        Assumptions.assumeTrue(spot != null, "no standable spot for the player");
        sim.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(spot[0], spot[1], start.heading()));

        Set<CreatureMode> modes = modesOver(sim, 10);

        assertTrue(modes.contains(CreatureMode.STALK), "modes seen: " + modes);
        PlayerTrace seen = creature.memory().focus();
        assertTrue(seen != null && seen.hasSeen(), "the creature should remember seeing the player");
        CreatureStatus now = sim.creatureStatus().orElseThrow();
        assertFalse(collision.sightClear(now.targetX(), now.targetZ(), seen.lastSeenX(), seen.lastSeenZ(),
                CreatureParameters.SIGHT_RADIUS), "the stalking spot should be out of sight of where the player was seen");
    }

    @Test
    void beingWatchedSendsTheCreatureToWithdraw() {
        WorldSimulation sim = fresh();
        CreatureStatus start = sim.creatureStatus().orElseThrow();
        WorldCollision collision = new WorldCollision(world);
        // The player 12 m in front of the creature, looking straight at it.
        float[] spot = standableNear(collision, start.x(), start.z(), start.heading(), 12);
        Assumptions.assumeTrue(spot != null, "no standable spot for the player");
        sim.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(spot[0], spot[1], start.heading() + (float) Math.PI));

        Set<CreatureMode> modes = modesOver(sim, 3);
        assertTrue(modes.contains(CreatureMode.WITHDRAW), "modes seen: " + modes);

        run(sim, 20);
        CreatureStatus later = sim.creatureStatus().orElseThrow();
        assertTrue(distance(later.x(), later.z(), spot[0], spot[1]) > 25,
                "the creature should have moved away from the player");
    }

    @Test
    void withoutAnyInformationTheCreatureRoamsOrHoldsAndNeverHunts() {
        WorldSimulation sim = fresh();
        Set<CreatureMode> modes = modesOver(sim, 180);
        assertEquals(EnumSet.of(CreatureMode.ROAM, CreatureMode.HOLD).containsAll(modes), true,
                "a creature that has perceived nothing should only roam or hold, saw " + modes);
    }

    @Test
    void theCreatureKeepsAModeForItsMinimumTime() {
        WorldSimulation sim = fresh();
        CreatureSimulation creature = sim.creature().orElseThrow();
        CreatureStatus start = sim.creatureStatus().orElseThrow();
        float[] spot = standableNear(new WorldCollision(world), start.x(), start.z(), start.heading(), 40);
        Assumptions.assumeTrue(spot != null, "no standable spot to put the sound at");
        creature.memory().note(ObservationKind.SOUND, spot[0], spot[1], sim.minuteOfDay(), 0.9f);

        run(sim, 1.5);
        assertEquals(CreatureMode.INVESTIGATE, sim.creatureStatus().orElseThrow().mode());
        // Within the 15 s commitment the mode must not flip back to roaming, even though the sound will fade.
        for (int i = 0; i < 13; i++) {
            run(sim, 1.0);
            assertEquals(CreatureMode.INVESTIGATE, sim.creatureStatus().orElseThrow().mode(),
                    "the mode changed inside its minimum time at second " + i);
        }
    }

    /**
     * The information barrier for Phase C: with the same memory, the creature's choices do not depend on where a player
     * is when that player is out of the creature's senses.
     */
    @Test
    void thePlayersHiddenPositionDoesNotChangeWhatTheCreatureDoes() {
        WorldSimulation a = fresh();
        WorldSimulation b = fresh();
        CreatureStatus start = a.creatureStatus().orElseThrow();
        float[] evidence = standableNear(new WorldCollision(world), start.x(), start.z(), start.heading(), 40);
        Assumptions.assumeTrue(evidence != null, "no standable spot to put the sound at");
        a.creature().orElseThrow().memory().note(ObservationKind.SOUND, evidence[0], evidence[1], a.minuteOfDay(), 0.9f);
        b.creature().orElseThrow().memory().note(ObservationKind.SOUND, evidence[0], evidence[1], b.minuteOfDay(), 0.9f);
        // Two players, far from the creature and from each other, standing still.
        a.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabin"));
        b.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));
        for (int i = 0; i < 1200; i++) {
            a.update(STEP, Map.of(PlayerId.LOCAL, PlayerInput.NONE));
            b.update(STEP, Map.of(PlayerId.LOCAL, PlayerInput.NONE));
            assertEquals(a.creatureStatus(), b.creatureStatus(), "the creature differed at step " + i);
        }
    }
}
