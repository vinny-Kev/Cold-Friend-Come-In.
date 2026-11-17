package com.coldfriend.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.entity.EntityId;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.gen.RandomStreams;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CreatureSimulationTest {

    private static final float STEP = 0.05f;
    private static final long SEED = 404L;

    private static WorldData generate(long seed) {
        return WorldGenerator.generate(new WorldSeed(seed), WorldGenParameters.DEFAULT);
    }

    private static double distance(float ax, float az, float bx, float bz) {
        return Math.hypot(ax - bx, az - bz);
    }

    @Test
    void aCreatureStartsFarFromTheCabinOutsideTheVillageForManySeeds() {
        for (long seed = 1; seed <= 10; seed++) {
            WorldData world = generate(seed);
            WorldCollision collision = new WorldCollision(world);
            Optional<CreatureSimulation> found = CreatureSimulation.spawn(world, collision, CreatureSimulation.ID,
                    new RandomStreams(world.seed(), world.generationVersion(), 0));
            assertTrue(found.isPresent(), "seed " + seed + " has no spot to start a creature");
            CreatureStatus status = found.get().status();
            Footprint cabin = world.cabin().footprint();
            assertTrue(distance(status.x(), status.z(), cabin.x(), cabin.z()) >= CreatureParameters.SPAWN_MIN_DISTANCE,
                    "seed " + seed + " starts too close to the cabin");
            assertTrue(distance(status.x(), status.z(), world.village().x(), world.village().z())
                    >= world.village().radius() + CreatureParameters.VILLAGE_KEEP_OUT_TARGET,
                    "seed " + seed + " starts in the village");
            assertTrue(collision.canStandAt(status.x(), status.z(), 0.4f), "seed " + seed + " starts inside a tree");
        }
    }

    @Test
    void theSameSeedStartsTheCreatureInTheSamePlace() {
        WorldData world = generate(SEED);
        CreatureStatus first = CreatureSimulation.spawn(world, new WorldCollision(world), CreatureSimulation.ID,
                new RandomStreams(world.seed(), world.generationVersion(), 0)).orElseThrow().status();
        CreatureStatus second = CreatureSimulation.spawn(world, new WorldCollision(world), CreatureSimulation.ID,
                new RandomStreams(world.seed(), world.generationVersion(), 0)).orElseThrow().status();
        assertEquals(first.x(), second.x(), 0f);
        assertEquals(first.z(), second.z(), 0f);
    }

    @Test
    void aWanderingCreatureMovesSlowlyAndNeverIntoBuildings() {
        WorldData world = generate(SEED);
        WorldSimulation simulation = new WorldSimulation(world, ItemCatalog.empty());
        CreatureStatus start = simulation.creatureStatus().orElseThrow();
        float lastX = start.x();
        float lastZ = start.z();
        double travelled = 0;
        double closestToCabin = Double.MAX_VALUE;
        Footprint cabin = world.cabin().footprint();
        float speedLimit = CreatureParameters.WANDER_SPEED * STEP * 1.01f + 1e-4f;
        // Five game minutes, no players.
        for (int i = 0; i < 6000; i++) {
            simulation.update(STEP, Map.of());
            CreatureStatus now = simulation.creatureStatus().orElseThrow();
            float stepDistance = (float) distance(now.x(), now.z(), lastX, lastZ);
            assertTrue(stepDistance <= speedLimit, "moved " + stepDistance + " m in one step");
            travelled += stepDistance;
            closestToCabin = Math.min(closestToCabin, distance(now.x(), now.z(), cabin.x(), cabin.z()));
            if (i % 50 == 0) {
                for (Footprint building : world.footprints()) {
                    assertFalse(building.contains(now.x(), now.z(), 0f), "inside a building at step " + i);
                }
            }
            lastX = now.x();
            lastZ = now.z();
        }
        assertTrue(travelled > 40, "the creature should wander, it travelled " + travelled + " m");
        assertTrue(closestToCabin >= CreatureParameters.CABIN_KEEP_OUT_ROUTE * 0.8,
                "the creature came within " + closestToCabin + " m of the cabin");
    }

    /**
     * Measures the cost of the creature's steps and checks it against a generous version of the budget (0.5 ms per
     * step on average, asserted at 1.5 ms so a slow machine does not fail it). The measured figures are printed.
     */
    @Test
    void theCreatureStaysWithinItsTimeBudget() {
        WorldData world = generate(SEED);
        WorldSimulation simulation = new WorldSimulation(world, ItemCatalog.empty());
        long firstStep = System.nanoTime();
        simulation.update(STEP, Map.of());
        long firstNanos = System.nanoTime() - firstStep;
        int steps = 4000;
        long start = System.nanoTime();
        for (int i = 0; i < steps; i++) {
            simulation.update(STEP, Map.of());
        }
        double averageMillis = (System.nanoTime() - start) / 1e6 / steps;
        System.out.printf("creature: first step %.2f ms (includes building the search grid), average %.4f ms per step%n",
                firstNanos / 1e6, averageMillis);
        assertTrue(averageMillis < 1.5, "average step took " + averageMillis + " ms");
    }

    @Test
    void aCreatureWithNobodyNearbyRemembersNothing() {
        WorldData world = generate(SEED);
        WorldSimulation simulation = new WorldSimulation(world, ItemCatalog.empty());
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabin"));
        for (int i = 0; i < 2400; i++) {
            simulation.update(STEP, Map.of(PlayerId.LOCAL, PlayerInput.NONE));
        }
        assertEquals(0, simulation.creatureStatus().orElseThrow().observations());
    }

    /**
     * The information barrier: the creature's behaviour must not depend on where the player is when the player is
     * out of its senses. Two worlds with the same seed, the player far away in different places and standing still,
     * must give exactly the same creature, step for step.
     */
    @Test
    void theCreatureDoesNotKnowWhereAPlayerIsWhenItCannotSenseThem() {
        WorldData world = generate(SEED);
        WorldSimulation probe = new WorldSimulation(world, ItemCatalog.empty());
        CreatureStatus creature = probe.creatureStatus().orElseThrow();
        float[] farA = farAway(world, creature, 0);
        float[] farB = farAway(world, creature, Math.PI);

        WorldSimulation a = new WorldSimulation(world, ItemCatalog.empty());
        WorldSimulation b = new WorldSimulation(world, ItemCatalog.empty());
        a.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(farA[0], farA[2], 0f));
        b.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(farB[0], farB[2], 0f));
        for (int i = 0; i < 2400; i++) {
            a.update(STEP, Map.of(PlayerId.LOCAL, PlayerInput.NONE));
            b.update(STEP, Map.of(PlayerId.LOCAL, PlayerInput.NONE));
            assertEquals(a.creatureStatus(), b.creatureStatus(), "the creature differed at step " + i);
        }
        assertEquals(0, a.creatureStatus().orElseThrow().observations());
    }

    /** A spot at least 200 m from the creature, in the given direction, that the player could stand on. */
    private static float[] farAway(WorldData world, CreatureStatus creature, double bearing) {
        for (double distance = 200; distance < 900; distance += 20) {
            float x = creature.x() + (float) (Math.sin(bearing) * distance);
            float z = creature.z() + (float) (Math.cos(bearing) * distance);
            boolean inside = x > 50 && z > 50 && x < world.sizeMeters() - 50 && z < world.sizeMeters() - 50;
            if (inside && new WorldCollision(world).canStandAt(x, z, 0.4f)) {
                return new float[] {x, world.terrain().heightAt(x, z), z};
            }
        }
        throw new IllegalStateException("no far spot in this direction");
    }

    /**
     * Architecture: the creature's decisions and memory never import the player's state. Only perception may read it,
     * and the simulation hands the players to perception. Also no engine types in the creature package.
     */
    @Test
    void theCreaturePackageKeepsDecisionsAwayFromThePlayerAndTheEngine() throws IOException {
        Path dir = Path.of("src", "main", "java", "com", "coldfriend", "creature");
        List<String> allowedToReadPlayers = List.of("Perception.java", "CreatureSimulation.java");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file);
                assertFalse(text.contains("com.jme3"), file + " uses the engine");
                if (!allowedToReadPlayers.contains(file.getFileName().toString())) {
                    assertFalse(text.contains("PlayerState"), file + " reads the player's state");
                }
            }
        }
    }

    /** Positive control: a player the creature can see is remembered, and one far away is not. */
    @Test
    void theCreatureRemembersAPlayerItCanSeeAndNotOneItCannot() {
        WorldData world = generate(SEED);
        WorldSimulation near = new WorldSimulation(world, ItemCatalog.empty());
        CreatureStatus creature = near.creatureStatus().orElseThrow();
        // Ten metres straight ahead of the creature, which faces its own heading.
        float x = creature.x() + (float) Math.sin(creature.heading()) * 10f;
        float z = creature.z() + (float) Math.cos(creature.heading()) * 10f;
        if (world.footprints().stream().noneMatch(f -> f.contains(x, z, 0.5f))) {
            near.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(x, z, creature.heading() + (float) Math.PI));
            // Sight runs every fourth step, so give it one full look.
            for (int i = 0; i < CreatureParameters.PERCEPTION_EVERY_STEPS; i++) {
                near.update(STEP, Map.of(PlayerId.LOCAL, PlayerInput.NONE));
            }
            assertTrue(near.creature().orElseThrow().memory().trace(PlayerId.LOCAL).hasSeen(),
                    "a player ten metres in front should be seen");
        }
        WorldSimulation far = new WorldSimulation(world, ItemCatalog.empty());
        float[] away = farAway(world, creature, 0);
        far.addPlayer(PlayerId.LOCAL, new SpawnPoints.Spawn(away[0], away[2], 0f));
        far.update(STEP, Map.of(PlayerId.LOCAL, PlayerInput.NONE));
        assertFalse(far.creature().orElseThrow().memory().trace(PlayerId.LOCAL).hasSeen());
    }
}
