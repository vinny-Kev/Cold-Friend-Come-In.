package com.coldfriend.world.gen;

import com.coldfriend.world.model.WorldData;
import java.util.HashMap;
import java.util.Map;

/** Generates each test world once and shares it between tests. */
final class TestWorlds {

    private static final Map<Long, WorldData> CACHE = new HashMap<>();

    private TestWorlds() {
    }

    static synchronized WorldData standard(long seed) {
        return CACHE.computeIfAbsent(seed,
                s -> WorldGenerator.generate(new WorldSeed(s), WorldGenParameters.DEFAULT));
    }

    /** @return a stable hash of the terrain heights, for comparing worlds */
    static long terrainHash(WorldData world) {
        long hash = 0;
        int vertices = world.terrain().vertices();
        for (int j = 0; j < vertices; j++) {
            for (int i = 0; i < vertices; i++) {
                hash = Mixer.mix(hash ^ Float.floatToIntBits(world.terrain().get(i, j)));
            }
        }
        return hash;
    }
}
