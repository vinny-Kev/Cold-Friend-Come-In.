package com.coldfriend.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ParkedCarTest {

    @Test
    void theCarParksBesideTheCabinOnStandableGroundForManySeeds() {
        for (long seed = 1; seed <= 8; seed++) {
            WorldData world = WorldGenerator.generate(new WorldSeed(seed), WorldGenParameters.DEFAULT);
            WorldCollision collision = new WorldCollision(world);
            Optional<ParkedCar> found = ParkedCar.find(world, collision);
            assertTrue(found.isPresent(), "seed " + seed + " has no clear spot for the car");
            ParkedCar car = found.get();
            double distance = Math.hypot(car.x() - world.cabin().doorX(), car.z() - world.cabin().doorZ());
            assertTrue(distance < 12, "seed " + seed + " parks the car " + distance + " m from the door");
            float[] centres = car.obstacleCentres();
            for (int i = 0; i < centres.length; i += 2) {
                assertTrue(collision.canStandAt(centres[i], centres[i + 1], ParkedCar.RADIUS),
                        "seed " + seed + " puts part of the car on something solid");
            }
        }
    }

    @Test
    void theSameSeedParksTheCarInTheSamePlace() {
        WorldData world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
        ParkedCar first = ParkedCar.find(world, new WorldCollision(world)).orElseThrow();
        ParkedCar second = ParkedCar.find(world, new WorldCollision(world)).orElseThrow();
        assertEquals(first, second);
    }

    @Test
    void theCarsBodyIsSolidOnce() {
        WorldData world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
        WorldCollision collision = new WorldCollision(world);
        ParkedCar car = ParkedCar.find(world, collision).orElseThrow();
        float[] centres = car.obstacleCentres();
        collision.addObstacle(new com.coldfriend.entity.EntityId(1L), centres[2], centres[3], ParkedCar.RADIUS);
        assertTrue(collision.obstructed(centres[2], centres[3], 0.4f), "a person cannot stand in the car");
    }
}
