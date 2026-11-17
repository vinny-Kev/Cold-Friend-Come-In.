package com.coldfriend.world.items;

import com.coldfriend.item.ItemCatalog;
import com.coldfriend.item.TestItems;
import com.coldfriend.player.PlayerId;
import com.coldfriend.player.PlayerInput;
import com.coldfriend.player.PlayerState;
import com.coldfriend.world.SpawnPoints;
import com.coldfriend.world.WorldSimulation;
import com.coldfriend.world.gen.WorldGenParameters;
import com.coldfriend.world.gen.WorldGenerator;
import com.coldfriend.world.gen.WorldSeed;
import com.coldfriend.world.model.CabinLayout;
import com.coldfriend.world.model.WorldData;
import java.util.Map;

/** Builds simulations of one shared test world, so item tests do not each generate a world. */
final class SimFixture {

    static final float FRAME = 1f / 60f;
    private static WorldData world;

    private SimFixture() {
    }

    static synchronized WorldData world() {
        if (world == null) {
            world = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
        }
        return world;
    }

    static ItemCatalog catalog() {
        return TestItems.catalog();
    }

    /** @return a new simulation with the test catalogue and no players */
    static WorldSimulation simulation() {
        return new WorldSimulation(world(), catalog());
    }

    /** @return the local player placed outside the cabin door, looking along the trail */
    static PlayerState outside(WorldSimulation simulation) {
        return simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world(), "cabindoor"));
    }

    /** @return the local player placed inside the cabin, facing the door */
    static PlayerState inside(WorldSimulation simulation) {
        return simulation.addPlayer(PlayerId.LOCAL, SpawnPoints.at(world(), "cabin"));
    }

    /** @return a player at a spot given in the cabin's own directions, facing the door */
    static PlayerState insideAt(WorldSimulation simulation, PlayerId id, float side, float forward) {
        float[] spot = CabinLayout.point(world().cabin(), side, forward);
        return simulation.addPlayer(id, new SpawnPoints.Spawn(spot[0], spot[1], world().cabin().footprint().yaw()));
    }

    /** Looks down by the given angle and advances one frame, so the player can aim at things near their feet. */
    static void lookDown(WorldSimulation simulation, float radians) {
        simulation.update(FRAME, Map.of(PlayerId.LOCAL, new PlayerInput(0f, 0f, 0f, -radians, false)));
    }

    static void walkForward(WorldSimulation simulation, float seconds) {
        for (float t = 0f; t < seconds; t += FRAME) {
            simulation.update(FRAME, Map.of(PlayerId.LOCAL, new PlayerInput(1f, 0f, 0f, 0f, false)));
        }
    }
}
