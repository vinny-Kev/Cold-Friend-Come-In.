# Architecture Guide

This guide describes the code as it is now. Each area is labelled **Implemented**, **Partial** or **Planned**. Where a claim is not verified by running the game, it says so.

For a short summary, see `docs/ARCHITECTURE.md`. That summary is older and does not yet cover the world, player or pause code.

## Package tree

```
com.coldfriend
├── Main                  entry point, loads config, starts GameApp          Implemented
├── core                  GameApp, config, settings, display, Intensity     Implemented
├── state                 screens: backdrop, main menu, settings, pause,    Implemented
│                         world play, inventory; ScreenNavigator, LightingProfile, PlayOptions
├── input                 InputController (menus), GameplayInput (world)    Implemented
├── ui                    MenuModel (logic), MenuView, MenuItem,            Implemented
│                         InventoryView, ShadowText
├── environment           the menu's procedural backdrop                    Implemented
├── assets                ModelCatalog, PropCatalog, PropKind, PropModel    Partial
├── item                  ItemId, ItemDefinition, ItemStack, ItemCatalog    Implemented
├── inventory             Inventory (slots, stacks, moves)                  Implemented
├── entity                EntityId (stable ids for things in the world)     Implemented
├── world
│   ├── gen               world generation pipeline (see WORLD_GENERATION)  Implemented
│   ├── model             WorldData and the data records it holds           Implemented
│   ├── query             TerrainRules, WorldCollision, GridNavigation      Implemented
│   ├── items             WorldItem(s), ItemActions, PlacementValidator     Implemented
│   ├── render            WorldScene, TerrainMeshes, CabinView, VillageView, Implemented
│   │                     WorldItemsView, PlacementPreview, RenderParts
│   ├── WorldSimulation   authoritative world state                         Implemented
│   └── SpawnPoints       player start positions                            Implemented
├── player                PlayerState, PlayerMovement, PlayerInput, PlayerId Partial
├── interaction           Interactable, InteractableKind (DOOR, ITEM),      Implemented
│                         InteractionRequest, ActionResult
├── dev                   AssetShowcase (temporary developer tool)          Temporary
└── audio (see docs/audio), creature, narrative, survival                   audio partial, rest planned
```

Tests mirror this layout under `src/test/java/com/coldfriend/`.

## Application lifecycle

1. `Main.main` reads `config\game.properties` (fails fast on a missing or bad key) and `config\settings.properties` (defaults if missing). It creates `GameApp` and starts it.
2. `GameApp.simpleInitApp` registers the `assets` folder with the engine, sets up the input controller, creates the screen states, and attaches them. The main menu is the only screen enabled.
3. If `dev.autostart` is set, `GameApp` starts a world straight away.
4. Each frame, the engine calls `update` on every enabled state. `GameApp.simpleUpdate` runs the development screenshot timer, if one is set.
5. `GameApp.destroy` removes input mappings and stops the engine.

## Screens and navigation

Screens are jME `BaseAppState` objects. Enabling and disabling them switches screens. `GameApp` is the only class that knows every screen. Screens reach the application through the `ScreenNavigator` interface, so they never hold a `GameApp` reference.

| State | Class | Purpose | Enabled when |
|---|---|---|---|
| Backdrop | `BackdropState` | Winter scene behind the menus, with fog | Menus and pause |
| Main menu | `MainMenuState` | Title, New Game, Settings, Quit | Start, and after leaving a world |
| Settings | `SettingsState` | Edit and apply settings | From the menu or pause |
| Pause | `PauseState` | Resume, Settings, Quit to Menu | Esc in a world |
| World play | `WorldPlayState` | First-person view of a generated world, HUD, placement mode | A world is running |
| Inventory | `InventoryState` | The 24-slot inventory screen; stops the game while open | I or Tab in a world |

Menu screens share `MenuScreenState`, which handles drawing a `MenuModel`, mouse hover and click, and re-layout on window resize.

`ScreenNavigator` is the interface screens use: `showMainMenu`, `showSettings`, `startNewGame`, `pauseGame`, `resumeGame`, `openInventory`, `closeInventory`, `beginPlacement`, `leaveWorld`, `saveAndApplySettings`, `quit`, `currentSettings`.

Only one of pause, inventory and placement owns input at a time: pause and inventory both stop the world (`WorldPlayState.setPaused(true)`, which releases gameplay input and shows the cursor) and take menu input through `InputController`; placement mode is part of `WorldPlayState` and uses gameplay input.

## Input

- **Menus:** `InputController` owns the key and mouse mappings. A screen calls `bind(owner, handler)` to receive `GameAction`s, and `release(owner)` when it closes. Ownership means a closing screen cannot clear a handler that a newer screen has set.
- **World:** `GameplayInput` owns its own mappings. It turns keys and mouse into `PlayerInput` and one-off requests (interact, pause, inventory, drop, place, rotate, confirm, cancel, number keys, mouse wheel), and removes every mapping in `detach()` when the player leaves or pauses.

## Configuration

- `config\game.properties` (tuning and developer keys) is read once at start-up. Values are read through `GameConfig`, which fails with a message naming the missing or malformed key.
- `config\settings.properties` (player settings) is written by `SettingsStore` when Apply is chosen. Invalid values fall back to defaults; the file is written through a temporary file so a crash cannot leave it half-written.
- `GameSettings` is an immutable record; changes produce a new copy with `with...` methods.

## World model and generation

- `world.gen.WorldGenerator` runs the generation stages in order and returns `WorldData`. See `WORLD_GENERATION_GUIDE.md`.
- `WorldData` (in `world.model`) is the generated world. It never changes after generation. It holds the terrain height field, rivers and water, regions, placements (trees, rocks and so on), the cabin, the village, paths and locations.
- `world.query` answers questions about the world: `TerrainRules` (where a person can walk, the same rules used by generation), `WorldCollision` (where a person can stand and slide along obstacles), and `GridNavigation`.

## World state and presentation

- `WorldSimulation` owns the authoritative state of one world: its players, their `Inventory`s, the cabin door, and the `WorldItems` lying or placed in the world. It moves players from their inputs, validates every interaction, drop, pick-up and placement, and knows nothing about rendering or devices, so it could later run on a server.
- **Items:** `ItemDefinition`/`ItemCatalog` (what an item is) are static data; `ItemStack`/`WorldItem` (how many, where) are instances. The `Inventory` holds stacks per `PlayerId`. `ItemActions` performs the transfers and `PlacementValidator` decides where items may go. There is deliberately no single item manager. See ITEM_SYSTEM.md, INVENTORY_SYSTEM.md, WORLD_PLACEMENT.md.
- `WorldPlayState` is the presentation layer. It reads input, hands it to the simulation, moves the camera to follow the player, and handles the fog, prompts, HUD, the placement preview and dev scenarios. It does not decide gameplay: placing sends the previewed spot to `WorldSimulation.placeItem`, which checks it again.
- `world.render.WorldScene` draws the terrain and water in chunks and batches scenery with hardware instancing, so thousands of trees cost few draw calls. Scenery chunks attach only near the viewer.
- `CabinView` and `VillageView` draw the cabin and village. Walls are built with real window openings and transparent glass (`RenderParts`). The door's open state comes from the simulation.
- `WorldItemsView` is shown the simulation's world-item list each frame and adds, moves or removes models to match. Item state is never stored in the scene graph.

## Assets

- `ModelCatalog` reads `assets\models\catalog.properties`. It loads each model listed under a `PropKind`, gives it a simple lit material that keeps its texture, and falls back to procedural geometry when a kind has no model.
- `PropCatalog` is the interface. Gameplay and rendering ask for a `PropKind` and a variant number, or an item **model key**, and get a `PropModel`. Only `ModelCatalog` knows file names. Items with no model get a magenta placeholder cube. See `ASSET_PIPELINE.md`.

## Player

- `PlayerState` is authoritative position and heading. It is owned by the simulation, and the camera follows it.
- `PlayerMovement` moves a player each frame: walking, sprinting, wading, sliding along obstacles and following the ground. Its rules are covered by `PlayerMovementTest`.

## Dependencies between packages

```
Main -> core -> state -> ui, input, environment, world (via WorldPlayState), assets
state -> core only through ScreenNavigator
world.gen -> world.model                (generation produces data)
world.query -> world.model              (queries read data)
world.render -> world.model, assets     (drawing reads data)
player -> world.model, world.query      (movement reads the world)
world (WorldSimulation) -> player, inventory, item, world.items, world.query
world.items -> item, inventory, player, interaction, world.model, world.query
inventory -> item                         (no engine, no screens)
state (InventoryState) -> ui, world (the simulation), inventory
```

Rules the code relies on:

- `world.gen` must not depend on rendering or on `world.render`.
- `ui` has no game rules. `MenuModel` has no engine dependency.
- Gameplay code refers to `PropKind`, not model files.

## Where to look when changing behaviour

| To change | Look at |
|---|---|
| Menu items or screen flow | `MainMenuState`, `PauseState`, `GameApp` navigation methods |
| Settings and their effect | `GameSettings`, `SettingsState`, `GameApp.saveAndApplySettings` |
| Player movement or speed | `PlayerMovement` and its constants |
| Walkability or collision | `TerrainRules`, `WorldCollision` |
| Terrain shape | `world.gen.TerrainStage`, `TerrainShaping` |
| Rivers | `HydrologyStage`, `FlowNetwork` |
| Trees, rocks, bushes | `VegetationStage`, then `assets\models\catalog.properties` for appearance |
| Cabin or village placement | `StructureStage`, `CabinSiting`, `VillageSiting` |
| How the world looks on screen | `world.render` package, `WorldPlayState` lighting and fog |
| Which models are used | `assets\models\catalog.properties`, `ModelCatalog` |
| Items, stack limits, starting kit | `config\items.properties` (ITEM_SYSTEM.md) |
| Inventory rules | `Inventory` (INVENTORY_SYSTEM.md) |
| Where items may be dropped or placed | `PlacementValidator` (WORLD_PLACEMENT.md) |
| Inventory screen | `InventoryState`, `InventoryView` |
| Window glass | `RenderParts.glass`, `RenderParts.wallWithOpenings` |

## Implemented vs planned

| Area | Status |
|---|---|
| Menus, settings, pause, backdrop | Implemented |
| World generation (terrain, rivers, regions, vegetation, cabin, village) | Implemented. Quality of the generated worlds has not been reviewed by the owner. |
| World rendering | Implemented |
| First-person player movement | Implemented, with unit tests |
| Interaction | The cabin door and world items go through one `Interactable` / `InteractionRequest` path in the simulation. Logic is unit tested; keyboard and mouse use in the real game window has not been exercised by hand. |
| Items, inventory, dropping, picking up, placing | Implemented in the simulation with tests; inventory screen, placement preview and item drawing verified by in-game screenshots. Most item models are placeholders. |
| Window glass | Implemented (transparent, two-sided), verified by in-game screenshots |
| Survival, narrative, creature | Planned (placeholder packages only) |
| Audio | Planned (placeholder package; volumes are stored but not applied) |
| Multiplayer | Planned as a later extension. Not started. |

## Standalone terrain, water and foliage module

**Status: Implemented, not yet wired into the game.** A second, self-contained world model sits next to the `gen`/`model` pipeline above. It was built first and does not use `WorldData`, `HeightField` or `WaterMap`, and nothing in `gen`, `model` or `render` uses it. Both currently coexist; reconciling them (one terrain, one water model) is open work.

```
com.coldfriend.world        pure Java, no engine types
    BaseTerrain             seeded height noise (256 m world)
    WaterType, WaterBody    water data (rivers, streams, lakes, ponds, ice), immutable
    WaterGenerator          placement (package-private)
    TerrainCarver           ground with channels cut in (package-private)
    GeneratedWorld          terrain + bodies + queries; the public face
    WaterSample             reusable query result
    WaterDetector,          per-entity contact tracking (dry / on ice / in water)
    WaterContact
    BushSpawn, BushPlacer   where bushes stand (own random stream)

com.coldfriend.environment  engine rendering of that data
    TerrainMeshBuilder      flat-shaded terrain mesh
    WaterMeshBuilder        ribbon / disc meshes per body (package-private)
    WaterView               node + one shared material + dispose()
    FoliageView             batched swaying bushes: node + one shared material + dispose()
    BushMeshBuilder         bush shape and per-vertex sway data (package-private)
    Wind                    wind values for the foliage shader
    SceneLighting           light values handed to the shaders (water, foliage)
    MeshCleanup             frees direct buffers

com.coldfriend.dev          WorldPreviewApp: fly camera and screenshots of a GeneratedWorld
                            (./gradlew worldPreview)

src/main/resources          MatDefs/Water.j3md + Shaders/Water.*, MatDefs/Foliage.j3md + Shaders/Foliage.*
```

Dependency direction: `dev -> environment -> world`; `world` imports only the JDK. That keeps the module testable without an engine.

Design decisions, in short:

- **Analytic queries instead of physics objects.** Water is long, thin and curved, so containment is distance to a centreline polyline. Queries allocate nothing and cannot drift from the visuals because both read the same `WaterBody` points.
- **One representation for every body.** A lake is a one-point polyline; rivers, streams and ponds use the same containment, carving and rendering code.
- **One shared carve function.** `TerrainCarver` serves both the generator (rivers are traced over already-carved ground) and `GeneratedWorld`.
- **Shaders animate from engine time**, so there is no per-frame Java work. Frozen state and flow live in vertex data, so the world needs one water material.
- **Lighting is passed in, not read.** The shaders cannot see jME lights, so the scene owner gives `WaterView` and `FoliageView` a `SceneLighting`.

Resource lifecycle: `WaterView.dispose()` and `FoliageView.dispose()` detach their nodes and destroy the meshes' direct buffers; terrain geometry from `TerrainMeshBuilder.build` is released with `MeshCleanup.release(geometry.getMesh())`.

Details: [WATER_SYSTEM.md](WATER_SYSTEM.md) and [FOLIAGE_WIND.md](FOLIAGE_WIND.md).
