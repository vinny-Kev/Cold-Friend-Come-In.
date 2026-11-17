# World Generation Guide

How the world is generated today. Only terrain and water exist; forests, buildings and creatures are not generated yet (the menu backdrop uses its own separate, fixed layout).

## Entry point

```java
GeneratedWorld world = GeneratedWorld.generate(seed); // com.coldfriend.world
```

Same seed (and same `WaterGenerator.GENERATION_VERSION`) always gives the same world. The world is 256 m by 256 m, centred on the origin (`BaseTerrain.HALF_EXTENT` is 128).

## Pipeline

1. **Base terrain** (`BaseTerrain`): fractal value noise, 4 octaves, frequency 1/70 per metre, amplitude 60 m before normalising. Hashing uses the seed, so there are no shared random streams and queries can be made in any order.
2. **Water placement** (`WaterGenerator`): lakes, then rivers, then streams, with end-ponds where a river stops in a hollow. Each river is traced over the terrain already carved by earlier water. Details are in [WATER_SYSTEM.md](WATER_SYSTEM.md).
3. **Carved terrain** (`TerrainCarver`): the final ground height with every channel cut in. `GeneratedWorld.terrainHeight` is this function.
4. **Bushes** (`BushPlacer`): placed on the finished world on their own random stream, clear of water. See [FOLIAGE_WIND.md](FOLIAGE_WIND.md).
5. **Meshes** (`environment`, rendering only): `TerrainMeshBuilder`, `WaterView` and `FoliageView` read the finished world. Rendering never feeds back into generation.

## Determinism rules

- Everything that decides layout must derive from the seed. Use `new Random(mixedSeed)` created in the generator, never `Math.random()`, time or iteration order of unordered collections.
- Keep layout and cosmetic decisions on separate random streams. Water uses one for geometry and one for frozen state, so adding or changing a cosmetic roll cannot move a river. Follow that pattern for new features.
- Generate in a fixed order. Order matters: lakes are placed first so rivers can stop at them. Changing order changes every world.
- Draw a fixed number of random values per feature whether or not it is placed, so skipping one feature does not shift the values used by the next.
- Bump `WaterGenerator.GENERATION_VERSION` when a change alters existing seeds. Saves and tests that depend on a layout should record the version.
- Rendering changes (shader, meshes, colours, mesh resolution) must not change anything in `world`. If a visual fix needs a data change, treat it as a generation change.

## Adding a new generated feature

1. Put pure data and queries in `com.coldfriend.world`, with no engine types. Put meshes and materials in `environment`.
2. Decide its random stream, its place in the order, and whether it reshapes terrain. If it does, route that through `TerrainCarver` (or an equivalent single height function) so generation and queries agree.
3. Make placement see what is already there. Water uses `ground(x, z)` (carved height) and `nearExisting(...)`.
4. Add it to `GeneratedWorld` as data plus queries.
5. Add tests across several seeds: same seed identical, different seeds differ, invariants hold for every seed.
6. Check it visually with `worldPreview` on at least three seeds, day and night.

## Checking generation

- Unit tests: `./gradlew test` (see `GeneratedWorldTest`).
- Visual: `./gradlew worldPreview -PpreviewArgs="--seed 7 --view overview"`. Views and flags are listed in WATER_SYSTEM.md.
- Eight seeds are exercised by the tests: 1, 2, 7, 42, 71017, 123456789, -5, 99.

## Current limits

Fixed 256 m world, no chunking, no biomes, and generation constants are in code rather than `config/game.properties`.
