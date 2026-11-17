package com.coldfriend.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.assets.PropKind;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.Placement;
import com.coldfriend.world.model.WorldData;
import com.coldfriend.world.query.GridNavigation;
import com.coldfriend.world.query.TerrainRules;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerMovementTest {

    private static final float FRAME = 1f / 60f;
    private static final float TOLERANCE = 0.05f;
    private static WorldData world;

    @BeforeAll
    static void generate() {
        world = WorldGenerator.generate(new WorldSeed(101L), WorldGenParameters.DEFAULT);
    }

    private static PlayerState spawn(WorldSimulation simulation, SpawnPoints.Spawn at) {
        return simulation.addPlayer(PlayerId.LOCAL, at);
    }

    private static void run(WorldSimulation simulation, PlayerInput input, float seconds) {
        for (float t = 0f; t < seconds; t += FRAME) {
            simulation.update(FRAME, Map.of(PlayerId.LOCAL, input));
        }
    }

    @Test
    void forwardMovesTheWayThePlayerFaces() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = spawn(simulation, SpawnPoints.at(world, "cabindoor"));
        float startX = player.x();
        float startZ = player.z();
        float yaw = player.yaw();

        run(simulation, new PlayerInput(1f, 0f, 0f, 0f, false), 0.5f);

        float moved = (float) Math.hypot(player.x() - startX, player.z() - startZ);
        assertTrue(moved > 1f, "player did not move");
        assertEquals(Math.sin(yaw), (player.x() - startX) / moved, TOLERANCE);
        assertEquals(Math.cos(yaw), (player.z() - startZ) / moved, TOLERANCE);
    }

    @Test
    void strafingRightMovesToThePlayersRight() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = spawn(simulation, SpawnPoints.at(world, "cabindoor"));
        float startX = player.x();
        float startZ = player.z();
        float yaw = player.yaw();

        run(simulation, new PlayerInput(0f, 1f, 0f, 0f, false), 0.5f);

        // The player's right is (-cos yaw, sin yaw) when facing (sin yaw, cos yaw).
        float moved = (float) Math.hypot(player.x() - startX, player.z() - startZ);
        assertEquals(-Math.cos(yaw), (player.x() - startX) / moved, TOLERANCE);
        assertEquals(Math.sin(yaw), (player.z() - startZ) / moved, TOLERANCE);
    }

    @Test
    void turningRightLowersTheHeadingAndLookIsLimited() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = spawn(simulation, SpawnPoints.at(world, "cabindoor"));
        float yaw = player.yaw();

        run(simulation, new PlayerInput(0f, 0f, 0.1f, 10f, false), FRAME);

        assertEquals(yaw - 0.1f, player.yaw(), 0.0001f);
        assertTrue(player.pitch() <= 1.5f && player.pitch() > 1f);
    }

    @Test
    void aLongFrameMovesNoFurtherThanTheCapAllows() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = spawn(simulation, SpawnPoints.at(world, "cabindoor"));
        float startX = player.x();
        float startZ = player.z();

        // One half-second frame, sprinting straight ahead on open ground. Without the cap this would be 2.8 m.
        simulation.update(0.5f, Map.of(PlayerId.LOCAL, new PlayerInput(1f, 0f, 0f, 0f, true)));

        float moved = (float) Math.hypot(player.x() - startX, player.z() - startZ);
        assertTrue(moved > 0.1f, "player did not move");
        assertTrue(moved <= 5.6f * PlayerMovement.MAX_STEP_SECONDS + TOLERANCE, "one long frame moved " + moved + " m");
    }

    @Test
    void sprintingIsFasterThanWalking() {
        float walked = distanceTravelled(false);
        float ran = distanceTravelled(true);

        assertTrue(ran > walked * 1.3f);
    }

    @Test
    void playerStandsOnTheGround() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = spawn(simulation, SpawnPoints.at(world, "village"));

        run(simulation, new PlayerInput(1f, 0f, 0f, 0f, false), 1f);

        assertEquals(world.terrain().heightAt(player.x(), player.z()), player.y(), 0.0001f);
    }

    @Test
    void aTreeBlocksThePlayer() {
        Placement tree = world.placements().stream().filter(p -> p.kind() == PropKind.CONIFER).findFirst()
                .orElseThrow();
        WorldSimulation simulation = new WorldSimulation(world);
        // Start 3 m west of the trunk and walk straight toward it.
        PlayerState player = spawn(simulation, new SpawnPoints.Spawn(tree.x() - 3f, tree.z(),
                (float) (Math.PI / 2.0)));

        run(simulation, new PlayerInput(1f, 0f, 0f, 0f, false), 3f);

        float distance = (float) Math.hypot(player.x() - tree.x(), player.z() - tree.z());
        assertTrue(distance >= tree.colliderRadius() + PlayerMovement.BODY_RADIUS - 0.01f,
                "player walked into the trunk");
    }

    @Test
    void buildingsAreSolid() {
        var f = world.cabin().footprint();
        WorldSimulation simulation = new WorldSimulation(world);
        float[] outside = f.pointInFront(f.halfDepth() + 3f);
        PlayerState player = spawn(simulation, new SpawnPoints.Spawn(outside[0], outside[1], f.yaw() + (float) Math.PI));

        run(simulation, new PlayerInput(1f, 0f, 0f, 0f, false), 3f);

        assertFalse(f.contains(player.x(), player.z(), 0f), "player walked through a wall");
    }

    @Test
    void theWalkOnTheTrailFromCabinToVillageNeverLeavesWalkableGround() {
        GridNavigation navigation = GridNavigation.of(world, 4f);
        var reachable = navigation.reachableFrom(world.cabin().doorX(), world.cabin().doorZ());

        assertTrue(reachable.contains(world.village().x(), world.village().z()));
        assertTrue(TerrainRules.walkable(world.terrain(), world.water(), world.village().x(), world.village().z()));
    }

    private static float distanceTravelled(boolean sprint) {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = spawn(simulation, SpawnPoints.at(world, "village"));
        float x = player.x();
        float z = player.z();
        run(simulation, new PlayerInput(1f, 0f, 0f, 0f, sprint), 1f);
        return (float) Math.hypot(player.x() - x, player.z() - z);
    }
}
