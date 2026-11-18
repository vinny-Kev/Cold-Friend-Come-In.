package com.coldfriend.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coldfriend.interaction.Interactable;
import com.coldfriend.interaction.InteractionRequest;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerState;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.WorldData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SenseEventsTest {

    private static final float FRAME = 1f / 60f;
    private static WorldData world;
    private static ItemCatalog items;

    @BeforeAll
    static void setUp() throws IOException {
        world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
        items = ItemCatalog.load(Path.of("config", "items.properties"));
    }

    @Test
    void theLogKeepsItsCapacityAndCountsTheRest() {
        SenseLog log = new SenseLog();
        for (int i = 0; i < SenseLog.CAPACITY + 4; i++) {
            log.add(new SenseEvent(SenseKind.DROP, i, 0f, 0.0));
        }
        assertEquals(SenseLog.CAPACITY, log.events().size());
        assertEquals(4, log.dropped());
        assertEquals(SenseLog.CAPACITY + 4, log.raised());
        log.clear();
        assertTrue(log.events().isEmpty());
        assertEquals(SenseLog.CAPACITY + 4, log.raised(), "clearing does not forget how many were raised");
    }

    @Test
    void standingStillMakesNoFootsteps() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));
        long before = simulation.sensesRaised();
        for (int i = 0; i < 600; i++) {
            simulation.update(FRAME, Map.of(PlayerId.LOCAL, PlayerInput.NONE));
        }
        assertEquals(before, simulation.sensesRaised());
    }

    @Test
    void walkingMakesAFootstepEveryTwoAndAHalfMetres() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        PlayerState player = simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));
        long before = simulation.sensesRaised();
        double travelled = 0;
        float lastX = player.x();
        float lastZ = player.z();
        // Walk straight on; stop well before any building could block the way.
        for (int i = 0; i < 300 && travelled < 12; i++) {
            simulation.update(FRAME, Map.of(PlayerId.LOCAL, new PlayerInput(1f, 0f, 0f, 0f, false)));
            travelled += Math.hypot(player.x() - lastX, player.z() - lastZ);
            lastX = player.x();
            lastZ = player.z();
        }
        long footsteps = simulation.sensesRaised() - before;
        assertTrue(travelled > 5, "the player should have walked a few metres, walked " + travelled);
        assertTrue(Math.abs(footsteps - travelled / 2.5) <= 1.0,
                footsteps + " footsteps for " + travelled + " m");
    }

    @Test
    void openingTheDoorIsHeard() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "cabindoor"));
        Interactable door = simulation.interactableFor(PlayerId.LOCAL).orElseThrow();
        long before = simulation.sensesRaised();

        assertTrue(simulation.interact(new InteractionRequest(PlayerId.LOCAL, door.ref())).success());

        assertEquals(before + 1, simulation.sensesRaised());
    }

    @Test
    void droppingAndPlacingAreHeard() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));
        long before = simulation.sensesRaised();

        assertTrue(simulation.dropItem(PlayerId.LOCAL, 0, 1).success());
        assertEquals(before + 1, simulation.sensesRaised());
    }

    @Test
    void aFailedDropIsNotHeard() {
        WorldSimulation simulation = new WorldSimulation(world, items);
        simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world, "village"));
        long before = simulation.sensesRaised();

        // The last slot is empty at the start, so there is nothing to drop.
        assertTrue(!simulation.dropItem(PlayerId.LOCAL, 23, 1).success());
        assertEquals(before, simulation.sensesRaised());
    }
}
