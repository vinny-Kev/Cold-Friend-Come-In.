# Testing Guide

## Framework and setup

- **JUnit Jupiter 5.11.4**, pinned in `gradle.properties`. Set up in `build.gradle` with `useJUnitPlatform()`.
- Run all tests: `gradlew.bat test`. Failures print in the console with full stack traces. Reports are in `build\reports\tests\test\index.html`.
- Run one class: `gradlew.bat test --tests com.coldfriend.world.gen.WorldGeneratorTest`.
- Run one method: `gradlew.bat test --tests "com.coldfriend.ui.MenuModelTest.clickingDisabledItemRunsNothing"`.

At the time of writing, `gradlew.bat build` passes with **54 tests in 9 classes**:

| Class | Tests | What it covers |
|---|---|---|
| `core.GameConfigTest` | 4 | Config keys, missing and malformed values |
| `core.GameSettingsTest` | 6 | Settings clamping, restart rules, `with...` copies |
| `core.ResolutionTest` | 3 | Resolution parsing |
| `core.SettingsStoreTest` | 3 | Saving and loading settings, bad values, no leftover temp file |
| `player.PlayerMovementTest` | 8 | Walking, sprinting, wading, sliding along obstacles |
| `ui.MenuModelTest` | 7 | Menu selection, wrapping, disabled rows, activation |
| `world.CabinTest` | 6 | Cabin layout and doorway |
| `world.gen.PlacementTest` | 8 | Scenery placement rules |
| `world.gen.WorldGeneratorTest` | 9 | Generation across seeds, validation, reproducibility |

## Test organisation

- Tests mirror the package of the code they cover.
- Helpers that are only for tests sit beside the tests:
  - `TestWorlds` generates each test world once per seed and caches it. Use it rather than generating in every test, since generation takes about a second.
  - `GoldenValues` holds expected values that pin a world down (see below).
  - `WorldMapRenderer` draws a top-down map for checking worlds by eye. It is not a test.

## What to unit test

Test the logic that can break without anyone noticing:

- parsing and persistence (config, settings, seeds, resolutions),
- rules (menu navigation, walkability, collision, placement),
- generation invariants,
- movement.

Do not unit test rendering. Check rendering by running the game.

## Unit testing deterministic generation

Generation must be reproducible. A test should generate the same world twice and compare:

```java
@Test
void sameSeedGivesSameTerrain() {
    WorldData first = TestWorlds.standard(404L);
    WorldData second = WorldGenerator.generate(new WorldSeed(404L), WorldGenParameters.DEFAULT);
    assertEquals(TestWorlds.terrainHash(first), TestWorlds.terrainHash(second));
}
```

`TestWorlds.terrainHash` gives a stable number for a world's heights, so you can compare two worlds in one line.

## Seed reproducibility and golden values

`GoldenValues` pins known outputs for known seeds. When you change a generation stage on purpose, the golden values change, and the test tells you. Update `GoldenValues` only when the change is intended, and bump `GenerationVersion.CURRENT` if the change affects existing worlds.

If a golden value changes and you did not mean to change the world, you have found an unintended side effect. Look for a shared random stream or a change to a shared helper.

## Generation invariants

Check rules that every valid world must meet, for example:

- the cabin and the village exist, and are reachable from each other,
- no scenery overlaps a building site or a path,
- water and terrain agree (no river drawn on a ridge top).

`WorldValidator` already checks these at generation time. Tests check the same rules across several seeds, so a failing seed is caught before the game reaches it.

## Testing multiple seeds

Use a range of seeds, not one:

```java
@ParameterizedTest
@ValueSource(longs = {1L, 2L, 7L, 404L, 9001L})
void validWorldForEachSeed(long seed) {
    WorldData world = TestWorlds.standard(seed);
    assertTrue(WorldValidator.validate(world).isEmpty());
}
```

`@ParameterizedTest` needs the `junit-jupiter-params` artifact, which the `junit-jupiter` aggregate already includes.

When you change a stage, generate the map for at least five seeds and look at them. The numbers can pass while the map looks wrong.

## Testing gameplay independently of rendering

`PlayerMovement` and `WorldCollision` take a `WorldData`, not a scene. Build a small world (or use `TestWorlds`) and call `step` directly:

```java
// PlayerInput is (forward, strafe, turn, look, sprint); PlayerMovement.step advances one player by dt seconds.
PlayerState player = new PlayerState(PlayerId.LOCAL, x, y, z, 0f);
movement.step(player, new PlayerInput(1f, 0f, 0f, 0f, false), 0.5f);
assertTrue(player.z() != z);
```

Keep new gameplay in classes that do not use the engine. Then it can be tested without a window.

## Manual visual testing

Automated tests cannot judge how the game looks. Use these checks:

1. **Startup:** `gradlew.bat run`. The menu appears, and the log shows `Main menu ready`.
2. **World:** start a new game, or set `dev.autostart=true` with `dev.spawn=cabinview` for a fixed view. Look for missing scenery, black objects and pink textures.
3. **Seeds:** change `world.seed` and check several worlds. Note anything that looks wrong and keep the seed, so you can reproduce it.
4. **Asset showcase:** `gradlew.bat showcase` places the models side by side at their real sizes. Use it when you add or change a model. It is temporary and can be removed.
5. **Screenshots:** set `dev.screenshot` to a path on your machine to save a screenshot and exit. Do not commit that path.

Record manual results in the commit message or a note, since nothing else records them.

## Adding a test

1. Pick the package of the code under test.
2. Name the class `<Thing>Test`.
3. Test one behaviour per method, with a name that says what it checks.
4. Use `assertEquals`, `assertTrue` and similar from `org.junit.jupiter.api.Assertions`.
5. Run `gradlew.bat test --tests <class>` until it passes, then run the full `gradlew.bat build`.

## Known gaps

- No tests for `GameApp`, the screens or rendering. These need the engine and are checked by running the game.
- The determinism and invariant tests run on a few seeds only. Widen the set before a release.
- The `dev` package has no tests, because it is a temporary tool.
