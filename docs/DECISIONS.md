# Decisions

Each entry: the decision, why, and what was considered. Entries marked **Open** need owner approval.

## D-001: Modular monolith, packages as subsystems

**Decision:** one Gradle module; subsystems are packages under `com.coldfriend`.
**Why:** a single desktop game with one developer. Separate modules add build overhead without a benefit yet.
**Alternatives:** multi-module Gradle build. Revisit if a subsystem needs independent release or tooling.

## D-002: jMonkeyEngine 3.9.0-stable with its own state and input model

**Decision:** screens are `BaseAppState` subclasses managed by the engine's `AppStateManager`. Input goes through jME's `InputManager`, wrapped by `InputController`.
**Why:** it is the engine's own lifecycle mechanism, so it needs no extra framework.
**Alternatives:** a custom screen stack (rejected: duplicates engine code); Lemur or Nifty GUI for menus (rejected: a new dependency, and the menu is small).

## D-003: Menus drawn with engine text, no GUI library

**Decision:** `MenuView` draws `BitmapText` rows and hit-tests them itself. Navigation logic lives in `MenuModel`, which has no engine dependency.
**Why:** the menus are a list of text items. A GUI library is not justified by this milestone, and the model is unit testable.
**Alternatives:** Lemur, Nifty GUI (both need owner approval; neither is needed yet). Revisit when there are real panels, inventory grids or text input.

## D-004: Settings stored as Java `.properties`, not JSON or YAML

**Decision:** `config/game.properties` (tuning, versioned) and `config/settings.properties` (player settings, git-ignored), both `java.util.Properties`.
**Why:** the brief asks for JSON or YAML. Java has no built-in JSON or YAML parser, and both need a library. `.properties` needs none and covers flat key/value configuration.
**Alternatives:** Gson (JSON), SnakeYAML (YAML), Jackson. Each needs owner approval.
**Open:** if you want JSON or YAML, approve one library and the files will move to it.

## D-005: Logging with SLF4J API and slf4j-simple backend

**Decision:** code logs through `org.slf4j`. Backend is `slf4j-simple`, configured in `src/main/resources/simplelogger.properties`.
**Why:** SLF4J is in the approved stack. The API alone prints nothing, so a backend is needed. slf4j-simple is the smallest one and has no configuration files beyond one properties file.
**Alternatives:** Logback (more features, more configuration; not needed yet).

## D-006: Distance fog via `jme3-effects` 3.9.0-stable

**Decision:** the menu scene uses jME's `FogFilter` (in `BackdropState`), fading distant geometry into the sky colour. Dependency `org.jmonkeyengine:jme3-effects:${jmeVersion}` added to `build.gradle`.
**Why:** the owner approved the dependency. It is the same engine version as the other jME modules.
**Status:** approved and added. Build and unit tests pass; the game starts with no errors. The visual effect has not been checked in a screenshot.
**Tuning:** `FOG_DENSITY = 1.0` (the Normal level) and `FOG_DISTANCE = 120` are constants in `BackdropState`. The player scales density with the Fog setting (Off, Light, Normal, Heavy).

## D-007: Volume settings stored, not applied

**Decision:** master, music and sound-effect volumes are read, changed and saved, but no audio is played.
**Why:** audio is not part of this milestone. Applying volumes now would need a mixing design that the owner has not approved.
**Alternatives:** wire volumes to jME audio nodes now (rejected: no audio exists to apply them to).

## D-008: Display changes apply by restarting the display

**Decision:** changing resolution, fullscreen or vsync stores the new settings, calls `setSettings` with fresh `AppSettings`, then `restart()`.
**Why:** jME's supported way to change window configuration at run time.
**Status:** the code path is written and reviewed. It has **not** been exercised at runtime, because the synthetic keyboard input used for testing did not reach the game (see the verification notes in the README).

## D-009: Procedural placeholder scene; no external assets yet

**Decision:** the menu backdrop is generated in code from boxes and faceted cones. No model files are loaded.
**Why:** the brief asks for placeholders and no external assets in this milestone.
**Note:** the only texture used is jME's built-in `Common/Textures/dot.png` for snowflakes.

## D-010: Assets folder registered with a file locator

**Decision:** `GameApp` registers `assets/` as a file locator. The folder tree is in the repo with `.gitkeep` files.
**Why:** the brief defines this structure, and external assets will live there.

## D-011: glTF and FBX loading via `jme3-plugins` 3.9.0-stable

**Decision:** `org.jmonkeyengine:jme3-plugins:${jmeVersion}` added to `build.gradle`. It provides the glTF, GLB and FBX loaders.
**Why:** the owner approved it. The launch log showed these loaders missing, and the asset packs in `assets/models/` are glTF files.
**Status:** the build and 23 unit tests pass, and the game starts. Startup no longer warns about the glTF, GLB, FBX or Ogre loaders. The OGG audio loader warning remains and belongs to the audio milestone. No model has been loaded into the scene yet.

## D-012: Seeded world generation: own noise, own heightfield, no new dependencies

**Decision:** the world is generated by a staged pipeline in `com.coldfriend.world.gen` with no engine types. Each stage draws from its own random stream, derived by hashing the seed, the generation version, the attempt number and the stage name. Noise is written in the project (integer hashing, basic float arithmetic), so a seed gives the same world on every machine. Terrain is a bounded 1536 m heightfield on a 2 m grid. Rivers come from filling hollows, routing water downhill on a coarse grid, picking the busiest paths, smoothing and carving them into the terrain.
**Why:** same seed and version must give the same world, including for a future multiplayer client. A custom heightfield also keeps chunked meshes and loading open, and needs no `jme3-terrain` or physics dependency.
**Parameters:** all generation values live in `WorldGenParameters.DEFAULT`, not in `game.properties`, because editing them would silently change what a seed produces. The world records the seed, `GenerationVersion`, attempt number and a parameter fingerprint.
**Versioning rule:** change what a seed produces only together with a `GenerationVersion` bump, and update the pinned hash in `GoldenValues`.

## D-013: Scenery, cabin, village and paths are generated data, with stable ids

**Decision:** `WorldData` also holds scenery (`Placement`, by `PropKind`, never by file name), the `Cabin`, the `Village` (buildings, named locations), `WorldPath`s and named `WorldLocation`s such as water access. Stages run in this order: terrain, rivers, regions, vegetation, structures, then scenery is cleared from building sites and paths. The cabin and village are ranked against targets drawn from the seed (distance to water, forest density, distance apart), which is what makes their surroundings differ between seeds.
**Reachability:** paths come from A* search that may cross deep water at a high cost; a ford (shallow bed) is cut where a path crosses. `GridNavigation` then checks, with the same `TerrainRules` the player will use, that the village, every doorstep and every water point can be walked to from the cabin door. A world that fails is regenerated with the next attempt number.
**Dependency to know about:** region tuning can move the cabin and village, because cabin siting reads forest density. Tree and prop placement never moves rivers, cabin or village (tested).

## D-014: Presentation reads simulation state; scenery drawn with instancing; custom collision

**Decision:** `WorldSimulation` (authoritative state: players, later doors, NPCs, creature) advances only from `PlayerInput` records keyed by `PlayerId`. `WorldPlayState` turns keys and mouse into input, runs the simulation, and moves the camera to follow the player. It owns its input mappings (`GameplayInput`) and removes them when disabled. `PlayerMovement` and `WorldCollision` use no engine types.
**Rendering:** terrain and water are meshes in 64 m chunks. Scenery is grouped per chunk into `InstancedNode`s (hardware instancing) and attached only near the viewer. Models are chosen by `PropKind` through `PropCatalog`; the files are listed in `assets/models/catalog.properties`, and a kind with no model falls back to procedural geometry.
**Collision:** height sampling plus the shared `TerrainRules` (slope, wading depth), round colliders for trees and rocks, rectangles for buildings. No physics dependency.
**Why:** keeps gameplay decisions out of rendering and camera code (multiplayer), keeps draw calls low, and needs no new dependency.
**Assets:** models come from the owner's KayKit Forest Nature Pack and Quaternius Stylized Nature MegaKit (both CC0), imported into `assets/models/environment/` with their licences.

## D-015: Cabin parts are data; the door is simulation state; pause keeps the world loaded

**Decision:** `CabinLayout` describes the cabin's walls, doorway, door and furniture as boxes in the cabin's own directions. Collision (`WorldCollision`) and drawing (`CabinView`) both read it, so they cannot disagree. Whether the door is open lives in `WorldSimulation`; the view only animates toward it. Using things goes through `InteractionRequest` and `Interactable`, so a future server can validate it.
**Pause:** `PauseState` is a menu over the world. `WorldPlayState` stays loaded but stops updating and releases its input mappings, so the cursor is free. Settings opened from the pause menu return to it.
**Development screenshots:** `dev.screenshot` saves the game's own frame through the engine's screenshot state. Desktop screen capture and synthetic mouse or keyboard input are not used, because they can capture other windows and interfere with the person using the machine.

## D-016: Real window openings and depth-write-off glass; items as definitions plus instances

**Window root cause:** windows were opaque `Unshaded` boxes laid on walls that were themselves single solid boxes. There was nothing to see through even if the glass had been transparent.
**Fix:** walls are built from pieces around real openings (`RenderParts.wallWithOpenings`), with opaque frames and two glass sheets facing opposite ways. Glass is lit, alpha blended, in the transparent bucket, with depth writing off so it cannot hide what is behind it. Village homes are now hollow shells so their windows show an interior.
**Items:** `ItemDefinition` (static data from `config/items.properties`) is separate from `ItemStack` and `WorldItem` (quantities and positions). Inventories, world items and placed objects all refer to items by `ItemId`. No item manager class: `Inventory` holds quantities, `ItemActions` does transfers, `PlacementValidator` decides where items may go, and `WorldSimulation` owns the state and exposes the requests.
**Placement:** the preview is presentation; the simulation validates again on confirm. Rules are an ordered list of small `PlacementRule`s so a new rule is one method.
**Missing assets:** only `stone` has a model. Other items draw as a magenta cube until models are approved. No icons exist; the inventory draws a coloured square with the item's first letter.
**Development aids:** `dev.time` (day, dusk, night), `dev.scenario` (inventory, drop, placement) and more `dev.spawn` values let screenshots show these without sending keyboard or mouse input.

## D-027: The simulation advances in fixed 20 Hz steps; excess frame time is dropped

**Decision:** `WorldSimulation.update(frameSeconds, inputs)` gives the frame time to `FixedStep`, which counts whole nanoseconds and runs whole steps of 0.05 s. At most five steps run per update; time owed beyond that is dropped and counted. Mouse look is applied once per frame, outside the steps. Pausing stops calling `update`; resuming calls `resetFrameTiming()`. The camera blends between the last two player positions.
**Why:** gameplay (movement, clock, fire, needs, and later the creature) must not depend on the frame rate, and a future server needs one tick rate. Dropping excess time avoids the spiral where a slow frame makes the next ones slower.
**Alternatives:** keep frame time and scale every rate (rejected: tuning would depend on the machine); catch up without a cap (rejected: bursts after a stall); a separate simulation thread (rejected: not needed).
**Status:** implemented in Milestone 04 Phase A, with tests. Supersedes the per-frame movement described in D-014.

## D-028: Game time lives in a `time` package; the simulation owns the only clock

**Decision:** `time.WorldClock` counts simulation steps and gives the day, the minute of the day and the `DayPhase`. One in-game day is 20 real minutes; dawn 05:30, day 07:00, dusk 17:00, night 19:30; new games start at 07:00 on day 1 (`ClockParameters.DEFAULT`). Lighting follows the clock. `dev.time` now sets the start time, not a lighting mode.
**Why:** the clock must be the authoritative source of game time. It sits in its own package, not in `survival` as the plan proposed, so that `world` → `survival` → `time` has no cycle and the creature can use it later.
**Status:** implemented; the day length is the proposed default and still an open owner decision (see the roadmap).

## D-029: Survival is four needs with health falling only while a need is critical

**Decision:** each player has warmth, nourishment, hydration and health (`survival.Survival`), from 0 to 100. Warmth moves toward a target set by inside or outside, the time of day, wading and the hearth. Nourishment and hydration fall per in-game day. Health falls only while one or more of the others is below its critical level, faster with two; every need passes a warning level first. Values are in `SurvivalParameters.DEFAULT`. No stamina, sickness, sleep or weather.
**Why:** the minimal set that gives decisions (the fire, the river, the cold) without chores, and death that is always preceded by a visible warning.
**Status:** implemented. The rates are the design's proposals and need playtesting; outside at night is currently harsh (critical warmth in about 25 real seconds).

## D-030: The hearth is simulation state; drawing reads it

**Decision:** `survival.Hearth` holds the stove's fuel (fuel-hours) and state (unlit, burning, embers, out). Level is fuel × 0.15, capped at 0.9; a lit fire burns its level per hour (at least 0.1). Embers below 0.15; out 30 game minutes after the fuel is gone. A new world starts lit with 3 fuel-hours. E at the stove adds one firewood, or lights a cold stove with matches and one branch. The room light and the stove's glowing door in `CabinView` follow the level.
**Why:** the fire is the central survival mechanic and must be authoritative and testable without rendering.
**Deviations from the design:** the box of matches is not used up (the item is one box; counting matches would need item durability); start fuel is 3, not 2, because 2 sits exactly on the strong-heat threshold.
**Status:** implemented with tests.

## D-031: Interactables are identified by `InteractableRef`, built on `EntityId`

**Decision:** `InteractableRef(kind, number)` replaces string ids. Fixed parts have constants (`CABIN_DOOR`, `CABIN_HEARTH`); items use `InteractableRef.item(EntityId)`. `InteractionRequest` carries a ref. The old `"item:<id>"` strings and `WorldSimulation.CABIN_DOOR_ID` are gone.
**Why:** strings had to be parsed back into numbers and could drift from presentation. A record gives correct equality and hashing, and items keep their single identity, `EntityId`.
**Status:** implemented with tests.

## D-032: Gameplay tuning lives in `*Parameters.DEFAULT` records

**Decision:** clock, survival and hearth values are in `ClockParameters.DEFAULT`, `SurvivalParameters.DEFAULT` and `HearthParameters.DEFAULT`. `config/game.properties` keeps display, menu and `dev.*` keys only. Item data stays in `config/items.properties` (a new `water_container_empty` item was added there).
**Why:** these values are game balance; editing them in a file would silently change behaviour, the same reason D-012 keeps generation parameters in code. Tests refer to them by name.
**Status:** implemented. Revisit if the owner wants to tune without rebuilding.

## D-033: Item use is a simulation request keyed by item id; F uses the selected item

**Decision:** `WorldSimulation.useItem(player, slot)` handles eating (`ration`), drinking (`water_container`, which becomes `water_container_empty`) and filling an empty container at open water (at the feet or 1.2 m ahead). Each action checks first and changes nothing if it fails. The F key sends the request; the HUD shows `[F]` with the item's `useVerb`.
**Why:** the player needs a way to eat and drink, and the transfer must be all or nothing. Filling anywhere there is water is simpler than only at `WATER_ACCESS` points, which the design proposed.
**Status:** implemented with tests.

## D-034: One canonical decision log

**Decision:** this file, `docs/DECISIONS.md`, is the decision log. `docs/development/DECISIONS.md` is a summary table whose IDs D-012 to D-026 mean different things from the entries here; it is kept for reference and no longer receives new decisions. New decisions start at D-027 here so no ID means two things.
**Why:** two logs with clashing numbers made references ambiguous.
**Status:** done. Merging the summary table's rows into this file is left for a later clean-up.
