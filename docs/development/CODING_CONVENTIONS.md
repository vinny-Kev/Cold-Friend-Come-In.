# Coding Conventions

These conventions match the code as it is. Follow them so the codebase stays consistent. The pragmatic rule is: a modular Java game, not a service architecture. Add structure only when a second use appears.

## Language and build

- **Java 21.** The Gradle toolchain enforces it. Use records for immutable values (`Resolution`, `GameSettings`, `Intensity`, `WorldSeed`).
- **Compiler flags:** `-Xlint:all,-this-escape -Werror`. Warnings fail the build. Fix the warning. Do not suppress it unless there is a clear reason, and then say why in a comment.
- **Dependencies** are pinned in `gradle.properties`. Do not add or upgrade one without owner approval.

## Naming

- Types: `PascalCase`. Methods and fields: `camelCase`. Constants: `UPPER_SNAKE_CASE`.
- Packages: lowercase, one word where possible (`world.gen`, `world.render`).
- Screens end in `State` (`MainMenuState`). Factories end in `Factory` (`TreeFactory`). Generation stages end in `Stage` (`TerrainStage`).
- Test classes end in `Test` and sit in the same package as the code under test.

## Formatting

- Four spaces. No tabs.
- UTF-8 source files.
- Explicit imports only. No wildcard imports (`import java.util.*;` is not allowed).
- One top-level type per source file.
- Keep methods short and focused. If a class holds several unrelated jobs, split it by job, not by line count.

## Constants and tuning

- No magic numbers. Put named constants at the top of the class.
- Values that a designer or player might tune belong in configuration:
  - game tuning in `config\game.properties` (read through `GameConfig`),
  - world values in `WorldGenParameters` or `PlacementParameters` (they change the world, so they are part of its identity).

## Error handling

- Handle exceptions where they can be handled. Log and fall back if the game can keep running (for example, a model that fails to load falls back to procedural geometry).
- Fail fast with a clear message where configuration is wrong. `GameConfig` throws `IllegalStateException` naming the key.
- Never swallow an exception silently. Log it, or rethrow it.
- Do not throw checked exceptions out of a screen's lifecycle method. Catch and log.

## Logging

- Use **SLF4J only**: `private static final Logger LOG = LoggerFactory.getLogger(Foo.class);`.
- Never use `System.out` or `System.err`.
- Use placeholders, not string concatenation: `LOG.info("Saved settings to {}", file)`.
- Log at `INFO` for start-up and major events, `WARN` for recoverable problems, `ERROR` with the exception for failures.

## Configuration

- Read keys through `GameConfig`. Do not parse properties files directly in gameplay code.
- Player settings are written only by `SettingsStore`.
- Developer keys (`dev.*`) are for testing. Do not rely on them in game logic.

## Resource lifecycle

- Create engine objects in `initialize` or `simpleInitApp`, not in constructors. The constructors of `GameApp` and the states run before the engine is ready.
- Attach to the scene in `onEnable`, detach in `onDisable`.
- Release input bindings and mappings on the same path that created them.
- Null out references in `cleanup`.

## Dependency boundaries

- `state` depends on `core` only through `ScreenNavigator`. Screens never hold a `GameApp`.
- `world.gen` depends on nothing in rendering or the engine. Keep it that way, so generation can be tested without a window.
- `world.query` and `player` do not depend on the engine.
- `ui` contains no game rules. `MenuModel` has no engine dependency.
- Gameplay refers to `PropKind` and `Interactable` kinds, never to file names or formats.

These boundaries are what make the tests possible. See `TESTING_GUIDE.md`.

## Documentation in code

- Public types and methods that other packages use carry a short Javadoc comment.
- Say **why**, not **what**, when a comment is needed. A comment that restates the code is noise.
- Package-level comments (`package-info.java`) say what the package is for, and whether it is planned.

## Assets

- Asset files live under `assets\`. See `ASSET_PIPELINE.md`.
- Keep each third-party pack in its own folder, with its licence file beside it.
- Record every asset in `ASSET_MANIFEST.md` when you add it.

## Adding a new subsystem without unnecessary abstraction

1. Create a package under `com.coldfriend` with a `package-info.java` describing its job.
2. Put its state in plain classes. Add an interface only when a second implementation is real (`PropCatalog` has one; that is enough).
3. Connect it to the rest of the game through `WorldSimulation` (for gameplay) or a screen (for presentation).
4. Add a test for its logic before wiring it to the engine.
5. Update `ARCHITECTURE_GUIDE.md` with the new package.

Do not add a framework, a dependency-injection container or an event bus. The project has not needed them, and each one would need owner approval.

## Testing conventions

- JUnit 5. Tests live in `src/test/java`, in the same package as the code.
- Test pure logic: models, parsing, persistence, generation, movement rules.
- Rendering is checked by running the game, not by unit tests.
- A test should fail when the behaviour it covers breaks. Do not test getters.
