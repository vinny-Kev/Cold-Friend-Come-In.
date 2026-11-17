package com.coldfriend.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.interaction.Interactable;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerState;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.Footprint;
import com.coldfriend.world.model.WorldData;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CabinTest {

    private static final float FRAME = 1f / 60f;
    private static final PlayerInput WALK_FORWARD = new PlayerInput(1f, 0f, 0f, 0f, false);
    private static WorldData world;

    @BeforeAll
    static void generate() {
        world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
    }

    private static void walk(WorldSimulation simulation, float seconds) {
        for (float t = 0f; t < seconds; t += FRAME) {
            simulation.update(FRAME, Map.of(PlayerId.LOCAL, WALK_FORWARD));
        }
    }

    /** Walks forward until the player is outside (or inside) the cabin, for at most six seconds. */
    private static void walkUntil(WorldSimulation simulation, PlayerState player, boolean outside) {
        for (float t = 0f; t < 6f && outsideCabin(player) != outside; t += FRAME) {
            simulation.update(FRAME, Map.of(PlayerId.LOCAL, WALK_FORWARD));
        }
        // Take a few more steps so a player stuck on the doorstep would show.
        walk(simulation, 0.1f);
    }

    private static PlayerState insideCabin(WorldSimulation simulation) {
        return simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabin"));
    }

    private static boolean outsideCabin(PlayerState player) {
        Footprint f = world.cabin().footprint();
        return !f.contains(player.x(), player.z(), 0.2f);
    }

    private static void useDoor(WorldSimulation simulation) {
        Optional<Interactable> target = simulation.interactableFor(PlayerId.LOCAL);
        assertTrue(target.isPresent(), "no door within reach");
        assertTrue(simulation.interact(new InteractionRequest(PlayerId.LOCAL, target.get().id())).success());
    }

    @Test
    void playerStartsInsideTheCabinWithTheDoorShut() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = insideCabin(simulation);

        assertTrue(world.cabin().footprint().contains(player.x(), player.z(), -CabinLayout.WALL_THICKNESS));
        assertFalse(simulation.isCabinDoorOpen());
    }

    @Test
    void aShutDoorKeepsThePlayerInside() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = insideCabin(simulation);

        walk(simulation, 4f);

        assertFalse(outsideCabin(player), "player walked through the shut door");
    }

    @Test
    void anOpenDoorLetsThePlayerOutAndBackIn() {
        WorldSimulation simulation = new WorldSimulation(world);
        PlayerState player = insideCabin(simulation);
        walk(simulation, 1.5f);
        useDoor(simulation);
        assertTrue(simulation.isCabinDoorOpen());

        walkUntil(simulation, player, true);
        assertTrue(outsideCabin(player), "player could not leave through the open door");

        // Turn round and walk back in.
        simulation.update(FRAME, Map.of(PlayerId.LOCAL, new PlayerInput(0f, 0f, (float) Math.PI, 0f, false)));
        walkUntil(simulation, player, false);
        assertFalse(outsideCabin(player), "player could not come back in");
    }

    @Test
    void theDoorOnlyWorksFromCloseBy() {
        WorldSimulation simulation = new WorldSimulation(world);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));

        assertTrue(simulation.interactableFor(PlayerId.LOCAL).isEmpty());
        assertFalse(simulation.interact(new InteractionRequest(PlayerId.LOCAL, WorldSimulation.CABIN_DOOR_ID)).success());
        assertFalse(simulation.isCabinDoorOpen());
    }

    @Test
    void usingTheDoorTwiceShutsItAgain() {
        WorldSimulation simulation = new WorldSimulation(world);
        insideCabin(simulation);
        walk(simulation, 1.5f);

        useDoor(simulation);
        useDoor(simulation);

        assertFalse(simulation.isCabinDoorOpen());
    }

    @Test
    void furnitureAndWallsDoNotBlockTheWayFromBedToDoor() {
        // The doorway, the spawn spot and the middle of the room must all be free of furniture.
        Footprint f = world.cabin().footprint();
        for (Footprint item : CabinLayout.furniture(world.cabin())) {
            float[] doorway = CabinLayout.point(world.cabin(), 0f, f.halfDepth() - 0.5f);
            float[] spawn = CabinLayout.point(world.cabin(), 0f, -0.8f);
            assertFalse(item.contains(doorway[0], doorway[1], 0.4f));
            assertFalse(item.contains(spawn[0], spawn[1], 0.4f));
        }
        assertEquals(5, CabinLayout.walls(world.cabin()).size());
    }
}
