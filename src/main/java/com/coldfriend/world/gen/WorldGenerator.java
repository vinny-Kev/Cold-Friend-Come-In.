package com.coldfriend.world.gen;

import com.coldfriend.world.model.HeightField;
import com.coldfriend.world.model.Placement;
import com.coldfriend.world.model.RegionMap;
import com.coldfriend.world.model.WorldData;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the world-generation pipeline. Every stage draws from its own random stream and the stages run in a
 * fixed order, so a seed, generation version and parameter set always give the same world, whatever the
 * machine, frame rate or rendering order.
 */
public final class WorldGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(WorldGenerator.class);
    private static final int MAX_ATTEMPTS = 8;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final float MIN_SCENERY_RADIUS = 0.3f;

    private WorldGenerator() {
    }

    /**
     * Generates a world. If an attempt cannot place the cabin and village, or the world fails validation, the
     * generator tries again with the next attempt number, which is part of the world's identity, so the
     * result is still the same every time.
     *
     * @param seed the world seed
     * @param parameters the generation parameters
     * @return the generated world
     * @throws IllegalStateException if no attempt passes validation
     */
    public static WorldData generate(WorldSeed seed, WorldGenParameters parameters) {
        long started = System.nanoTime();
        LOG.info("Generating world: seed={} generationVersion={} parameters={}",
                seed, GenerationVersion.CURRENT, parameters.fingerprint());
        List<String> problems = List.of();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                WorldData world = attemptGenerate(seed, parameters, attempt);
                problems = WorldValidator.validate(world);
                if (problems.isEmpty()) {
                    LOG.info("World ready: seed={} attempt={} trees and props={} in {} ms", seed, attempt,
                            world.placements().size(), (System.nanoTime() - started) / NANOS_PER_MILLI);
                    return world;
                }
            } catch (IllegalStateException e) {
                problems = List.of(e.getMessage());
            }
            LOG.warn("World attempt {} rejected: {}", attempt, problems);
        }
        throw new IllegalStateException("No valid world for seed " + seed + ": " + problems);
    }

    private static WorldData attemptGenerate(WorldSeed seed, WorldGenParameters p, int attempt) {
        RandomStreams streams = new RandomStreams(seed.value(), GenerationVersion.CURRENT, attempt);
        Geography geography = Geography.derive(streams, p);
        HeightField terrain = TerrainStage.generate(p, geography, streams);
        HydrologyStage.Result hydrology = HydrologyStage.generate(terrain, geography, p, streams);
        RegionMap regions = RegionStage.generate(terrain, hydrology.water(), geography, p, streams);
        List<Placement> scenery = VegetationStage.generate(terrain, hydrology.water(), regions, p, streams);
        StructureStage.Plan plan = StructureStage.generate(terrain, hydrology.water(), regions,
                hydrology.rivers(), p, streams);
        return new WorldData(seed.value(), GenerationVersion.CURRENT, attempt, p.fingerprint(), terrain,
                hydrology.rivers(), hydrology.water(), regions, settle(scenery, plan.exclusions(), terrain),
                plan.cabin(), plan.village(), plan.paths(), plan.locations());
    }

    /** Removes scenery from cleared ground, then stands what is left on the final terrain. */
    private static List<Placement> settle(List<Placement> scenery, Exclusions cleared, HeightField terrain) {
        List<Placement> kept = new ArrayList<>(scenery.size());
        for (Placement placement : scenery) {
            float radius = Math.max(MIN_SCENERY_RADIUS, placement.colliderRadius());
            if (!cleared.blocks(placement.x(), placement.z(), radius)) {
                kept.add(placement.withY(terrain.heightAt(placement.x(), placement.z())));
            }
        }
        return kept;
    }
}
