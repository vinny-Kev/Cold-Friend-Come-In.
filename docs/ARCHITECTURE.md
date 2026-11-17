# Architecture

Cold Friend, Come In is a single desktop application built as a modular monolith. Subsystems are packages under `com.coldfriend`, with one-way dependencies.

## Stack

| Concern | Choice | Version |
|---|---|---|
| Language | Java | 21 (toolchain) |
| Engine | jMonkeyEngine: jme3-core, jme3-desktop, jme3-lwjgl3, jme3-effects, jme3-plugins | 3.9.0-stable |
| Logging | SLF4J API with slf4j-simple backend | 2.0.17 |
| Tests | JUnit Jupiter | 5.11.4 |
| Build | Gradle, Groovy DSL, wrapper | 8.14.3 |

Versions are pinned in `gradle.properties`.

## Subsystems

| Package | Responsibility | Status |
|---|---|---|
| `com.coldfriend` (`Main`) | Process entry point: loads config, starts the app | Implemented |
| `core` | Application root (`GameApp`), config, settings model and persistence, display mapping | Implemented |
| `state` | Screen states: menu backdrop, main menu, settings; navigation interface | Implemented |
| `input` | Central mapping from keys and mouse to `GameAction`, input ownership | Implemented |
| `ui` | Menu model (pure logic) and menu view (text rendering, hit testing) | Implemented |
| `environment` | Procedural menu scene: ground, trees, cabin, snow, camera sway (fog is applied by `state/BackdropState`) | Implemented |
| `world.gen` | Seeded generation pipeline: seed, streams, noise, terrain, rivers, regions, vegetation, structures, validation | Implemented |
| `world.model` | Generated world data with no engine types: terrain, rivers, regions, scenery, cabin, village, paths, locations | Implemented |
| `world.query` | Walkability rules, collision, grid navigation | Implemented |
| `world.render` | Terrain, water, instanced scenery, cabin and village drawing | Implemented |
| `world` | `WorldSimulation` (authoritative state), spawn points | Implemented |
| `player` | Player identity, state, input and movement, no engine types | Implemented |
| `interaction` | Interactables and interaction requests | Implemented (door only) |
| `assets` | Prop kinds, prop catalogue, model loading | Implemented |
| `audio` | Playback and mixing | Planned |
| `survival`, `creature`, `inventory`, `narrative` | Gameplay subsystems | Planned (package descriptors only) |

## Dependency direction

```
Main -> core -> state -> ui, input, environment
                  |
                  +-> core (GameSettings, Resolution via ScreenNavigator)
environment -> (engine only)
ui -> (engine font and nodes only)
input -> (engine input only)
core/config, core/settings -> (no engine dependency)
```

Rules:

- `state` depends on `core` only through the `ScreenNavigator` interface. States never hold a reference to `GameApp`.
- `ui` contains no game-specific rules. `MenuModel` has no engine dependency and is unit tested.
- `environment` knows nothing about settings or screens. It receives a `BackdropSettings` record.
- Gameplay subsystems (planned) will depend on `assets` and `world` interfaces, not on engine nodes where avoidable.

## Lifecycle

1. `Main` loads `config/game.properties` (fail fast if missing or malformed) and `config/settings.properties` (defaults if missing).
2. `GameApp.simpleInitApp` registers the `assets` folder, attaches `InputController`, then attaches three engine states:
   `BackdropState`, `MainMenuState` and `SettingsState`. Only the main menu is enabled.
3. Screens switch by enabling and disabling states. The disabling state releases input first, so ownership is never cleared by a state that has already been replaced.
4. Applying display settings stores the new settings, then calls `setSettings` and `restart()` on the engine.
5. `GameApp.destroy` detaches input mappings. Engine state cleanup runs on shutdown.

## Input

`InputController` owns the key and mouse mappings. Screens call `bind(owner, handler)` to receive `GameAction`s and `release(owner)` when they close. Mouse picking uses screen coordinates from the engine and `MenuView.itemAt`.

## Configuration

- `config/game.properties`: tuning and defaults, read at start-up. Versioned.
- `config/settings.properties`: player settings, written by the game. Git-ignored.

Format: Java `.properties`. See `docs/DECISIONS.md` for why JSON or YAML were not used yet.

## Assets

`assets/` holds externally sourced assets, in folders for models, textures, materials, audio, music, environments and UI. Empty folders are kept with `.gitkeep`. The menu scene uses only engine built-ins and procedural geometry.

## Known gaps

- No audio playback, so volume settings are stored but not applied.
- Restarting the display on resolution, fullscreen or vsync change has been designed but must be checked on the target machine (see README verification notes).

## Layers and multiplayer

1. `WorldData` (generated, immutable, no engine types): the same for the same seed, generation version and parameters.
2. `WorldSimulation` (authoritative mutable state): players keyed by `PlayerId`, the cabin door, later NPCs, items and the creature. It advances only from `PlayerInput` and `InteractionRequest` values.
3. Presentation (`state`, `world.render`, `input`): reads the other two layers, draws, reads devices. It never decides gameplay.

Future NPCs, the shapeshifter and other players belong in layer 2, keyed by stable ids. Appearance (what is drawn) is separate from identity and behaviour, so a disguise is an appearance assigned to an entity.
