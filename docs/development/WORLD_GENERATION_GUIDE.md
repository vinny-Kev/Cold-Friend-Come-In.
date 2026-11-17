# World Generation Guide

**Status: Implemented, with open quality questions.** The pipeline runs and is covered by tests (`WorldGeneratorTest`, `PlacementTest`, `CabinTest`). Whether the worlds look right is a judgement the owner has not yet made. Check the generated maps (see the testing guide) before treating any stage as final.

## The guarantee

A **seed**, a **generation version** and a **parameter set** together identify exactly one world. The same three inputs give the same world on any machine, at any frame rate, in any order of calls. Changing one of them changes the world, and that is intended.

- **Seed:** `world.seed` in `config\game.properties`. A whole number is used as it is. Any other text is hashed, so a word always gives the same world. An empty value means a random seed each time (`WorldSeed.random`).
- **Generation version:** `GenerationVersion.CURRENT` (currently 1). Bump it whenever a change would alter the world a seed produces. Older saved worlds would then need the old version.
- **Parameters:** `WorldGenParameters.DEFAULT` holds every tunable value. The world records `fingerprint()`, so a changed parameter is visible in the log.

## Deterministic randomness

All randomness comes from `RandomStreams`. Each stage gets its own stream, derived from:

- the world seed,
- the generation version,
- the attempt number, and
- the stage name (for example `RandomStreams.TERRAIN`).

Because each stage has its own stream, a change inside one stage does not shift the random numbers another stage uses. This is what keeps river placement stable when only vegetation changes.

`RandomStreams.random(stage)` returns a `SplittableRandom` for that stage. Use `seedFor(stage)` if you need a raw 64-bit seed. Use a new stage name for any new stage, never reuse an existing one.

Noise (`Noise2D`) and hashing (`Mixer`) use only integer maths and basic float arithmetic, so results do not depend on the platform.

## The pipeline

`WorldGenerator.generate(seed, parameters)` runs `attemptGenerate` up to 8 times. Each attempt runs these stages in order:

| Order | Stage | What it decides |
|---|---|---|
| 1 | `Geography.derive` | The large-scale character of the seed: which way the land falls, how mountainous, how wet, how wooded. Later stages read it. |
| 2 | `TerrainStage` | Height field: overall tilt, mountain ridges, rolling hills, hollows and small unevenness, bent by a warp noise. |
| 3 | `HydrologyStage` | Rivers: water routed downhill (`FlowNetwork`), busiest paths become rivers, smoothed and cut into the terrain. |
| 4 | `RegionStage` | Environmental regions from height, steepness, distance to water and two noises. Also a forest density that is blurred across region borders. |
| 5 | `VegetationStage` | Trees, rocks, bushes, fallen logs and clutter, on a jittered grid, weighted by forest density and grouped into groves. |
| 6 | `StructureStage` | The cabin and the village (`CabinSiting`, `VillageSiting`), the trail between them (`PathFinder`), levelled building sites and fords (`TerrainShaping`), and water-collection points (`ResourceStage`). |
| 7 | `settle` | Removes scenery that overlaps building sites, the village square and paths (`Exclusions`). |

The result is a `WorldData`. `WorldValidator.validate` then checks it against the rules every world must meet. If an attempt fails, the next attempt number is used. The attempt number is part of the world's identity, so the retry is still deterministic.

## Terrain, rivers and water

- The terrain is a `HeightField`: a grid with a fixed cell size (`cellSize` in the parameters).
- `FlowNetwork` fills hollows so every cell drains to the map edge. Water is then routed to one lower neighbour per cell.
- Rivers are the busiest flow paths. Their width grows with the area they drain. `WaterMap` answers "how deep is the water here".
- `TerrainRules` (used by both generation and gameplay) decides where a person can walk: slope limits and water depth.

## Biomes and regional variation

"Biome" is not a separate system here. Regions (`RegionStage`) and the seed's `Geography` provide regional variation: a high dry ridge, a wet valley, or a dense wood. Colours come from `world.render.TerrainColors`. If you add a biome, add a region rule in `RegionStage` and a colour rule in `TerrainColors`.

## Vegetation

`VegetationStage` gives each kind (trees, rocks, bushes, logs, clutter) its own random stream. Candidates are accepted by forest density. Groves come from a cluster noise, so trees are not evenly sprinkled. Tuning values are in `PlacementParameters`. Changing them changes the world, so they belong in `WorldGenParameters`, not in code constants.

## Villages and cabins

- `CabinSiting` scans for flat, dry ground away from the edge. It ranks sites by how close they come to a target distance from water and a target forest density, both drawn from the seed.
- `VillageSiting` finds a place for the village, then lays out its store and homes around a central square.
- `StructureStage` joins the cabin and the village with a trail, levels the sites, and adds fords where the trail crosses water.

## Resources

`ResourceStage` finds gentle, dry riverbank ground that a person can walk to. It picks the closest such point to the cabin, a second one well away from it, and the closest to the village. Only water collection is placed here so far.

## World validation

`WorldValidator` checks a generated world. A world that fails is rejected and regenerated with the next attempt. Add a rule there when a bug shows a world that breaks an assumption (for example, a building cut off from every path).

## How to add a generation feature

1. Pick a stage name and add a constant to `RandomStreams` (for example `public static final String FENCES = "fences";`). Never reuse a name.
2. Put any new tunable values in `PlacementParameters` or `WorldGenParameters`, not in code.
3. Write the stage as a static method that takes the terrain and earlier results and returns its own result.
4. Add the stage to `attemptGenerate` in the right order. Later stages may depend on it; earlier stages must not depend on it.
5. If the feature changes existing worlds, bump `GenerationVersion.CURRENT`. Update the golden values tests (`GoldenValues`) only when you mean to change the worlds.
6. Add a test (see `TESTING_GUIDE.md`) and generate the map image for several seeds to check it by eye.

## Testing multiple seeds

- `TestWorlds.standard(seed)` generates a world once per seed and caches it for the test run.
- `WorldMapRenderer` (test-only) draws a top-down map. Use it to check worlds by eye.
- Generate at least five different seeds when you change a stage, and look at all of them, not just one.

## Known gaps

- The owner has not yet approved the look of the generated worlds.
- Biomes are regions with colour rules, not a full biome system.
- Only water collection is placed as resources. Other resources (food, wood piles and so on) are not placed.
- There is no save or load of a generated world. A world is regenerated from its seed each time.
- The generation version has only ever been `1`. Nothing has yet been generated under an older version.

## Verified in this session

- The game generated a world at launch with the seed in the log, took about 780 ms, and entered play (see `GETTING_STARTED.md`).
- `WorldGeneratorTest`, `PlacementTest` and `CabinTest` pass (23 tests). The determinism guarantee is tested by those suites, not re-proven here.

## The standalone `GeneratedWorld` generator

A second, smaller generator lives in `com.coldfriend.world` (`GeneratedWorld.generate(seed)`). It is separate from the pipeline described above: it has its own terrain noise, its own rivers, streams, lakes and ponds (`WaterGenerator`), and its own bush placement (`BushPlacer`). It is not used by `WorldGenerator`, `WorldScene` or the game, and the two do not share data. Its own version constants are `WaterGenerator.GENERATION_VERSION` and `BushPlacer.PLACER_VERSION`.

Its rules mirror the guarantee above: the same seed and version give the same world; layout and cosmetic decisions use separate random streams (water geometry versus frozen state; bush placement on its own stream); rendering never feeds back into generation.

Details: [WATER_SYSTEM.md](WATER_SYSTEM.md) (rivers carved over already-carved terrain, ice, queries) and [FOLIAGE_WIND.md](FOLIAGE_WIND.md) (bushes and wind sway). Preview it with `./gradlew worldPreview -PpreviewArgs="--seed 7 --view river"`.
