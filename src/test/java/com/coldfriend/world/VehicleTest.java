package com.coldfriend.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.entity.EntityId;
import com.coldfriend.interaction.InteractableRef;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerState;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.WorldCollision;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VehicleTest {

    private static final float STEP = 0.05f;
    private static WorldData world;

    @BeforeAll
    static void setUp() {
        world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
    }

    /** A car on open ground, facing +z, with 12 m of clear ground ahead, so driving tests are not stopped by scenery. */
    private static Vehicle startCar(WorldCollision collision) {
        for (float x = 200f; x < world.sizeMeters() - 200f; x += 10f) {
            for (float z = 200f; z < world.sizeMeters() - 200f; z += 10f) {
                ParkedCar spot = new ParkedCar(x, z, 0f);
                if (clear(collision, spot, 0f) && clear(collision, spot, 12f)) {
                    return new Vehicle(world, collision, spot, 3_000_000_000L);
                }
            }
        }
        throw new IllegalStateException("no open ground in the test world");
    }

    private static boolean clear(WorldCollision collision, ParkedCar spot, float ahead) {
        float[] c = ParkedCar.centres(spot.x(), spot.z() + ahead, 0f);
        for (int i = 0; i < c.length; i += 2) {
            if (!collision.canStandAt(c[i], c[i + 1], ParkedCar.RADIUS)) {
                return false;
            }
        }
        return true;
    }

    private static void drive(Vehicle car, float throttle, float steer, double seconds) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            car.drive(throttle, steer, STEP);
        }
    }

    @Test
    void drivingForwardMovesTheCarAlongItsHeading() {
        WorldCollision collision = new WorldCollision(world);
        Vehicle car = startCar(collision);
        float startX = car.x();
        float startZ = car.z();

        drive(car, 1f, 0f, 3);

        double moved = Math.hypot(car.x() - startX, car.z() - startZ);
        assertTrue(moved > 5, "the car should have gone a good way, went " + moved + " m");
        assertEquals(Math.sin(car.yaw()), (car.x() - startX) / moved, 0.01);
        assertEquals(Math.cos(car.yaw()), (car.z() - startZ) / moved, 0.01);
    }

    @Test
    void theCarStopsAtSomethingSolid() {
        WorldCollision collision = new WorldCollision(world);
        Vehicle car = startCar(collision);
        // A tree-sized obstacle 6 m ahead of the car's centre.
        float ahead = 6f;
        float wallX = car.x() + (float) Math.sin(car.yaw()) * ahead;
        float wallZ = car.z() + (float) Math.cos(car.yaw()) * ahead;
        collision.addObstacle(new EntityId(5L), wallX, wallZ, 1f);

        drive(car, 1f, 0f, 6);

        assertEquals(0f, car.speed(), 0f, "the car should have stopped at the obstacle");
        assertTrue(Math.hypot(car.x() - wallX, car.z() - wallZ) > 1.5,
                "the car should not have driven into the obstacle");
    }

    @Test
    void steeringRightTurnsTheCarRight() {
        WorldCollision collision = new WorldCollision(world);
        Vehicle car = startCar(collision);
        float startYaw = car.yaw();

        drive(car, 1f, 1f, 1);

        assertTrue(car.yaw() < startYaw, "steering right should lower the heading, as for a player");
    }

    @Test
    void aPlayerGetsInDrivesAndGetsOut() {
        WorldSimulation simulation = new WorldSimulation(world, ItemCatalog.empty());
        Vehicle car = simulation.vehicle().orElseThrow();
        PlayerState player = simulation.addPlayer(PlayerId.LOCAL,
                new SpawnPoints.Spawn(car.x() + 1f, car.z(), car.yaw()));
        PlayerInput forward = new PlayerInput(1f, 0f, 0f, 0f, false);

        assertTrue(simulation.interact(new InteractionRequest(PlayerId.LOCAL, InteractableRef.VEHICLE)).success());
        assertTrue(simulation.isDriving());

        float startX = car.x();
        float startZ = car.z();
        for (int i = 0; i < 40; i++) {
            simulation.update(STEP, Map.of(PlayerId.LOCAL, forward));
        }
        assertTrue(Math.hypot(car.x() - startX, car.z() - startZ) > 3, "the car should have moved");
        assertEquals(car.x(), player.x(), 0.001f, "the player rides with the car");
        assertEquals(car.z(), player.z(), 0.001f);

        assertTrue(simulation.interact(new InteractionRequest(PlayerId.LOCAL, InteractableRef.VEHICLE)).success());
        assertFalse(simulation.isDriving());
        assertTrue(Math.hypot(player.x() - car.x(), player.z() - car.z()) >= 2.0, "the player stands beside the car");
    }
}
