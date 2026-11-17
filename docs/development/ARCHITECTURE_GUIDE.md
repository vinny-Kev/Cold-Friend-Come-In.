# Architecture Guide (developer notes)

Companion to [../ARCHITECTURE.md](../ARCHITECTURE.md), which holds the stack, subsystem table and lifecycle. This file covers how the world and water code is structured and why.

## World and water packages

```
com.coldfriend.world        pure Java, no engine types
    BaseTerrain             seeded height noise
    WaterType, WaterBody    water data (immutable)
    WaterGenerator          placement (package-private)
    TerrainCarver           ground with channels cut in (package-private)
    GeneratedWorld          terrain + bodies + queries; the public face
    WaterSample             reusable query result
    BushSpawn, BushPlacer   where bushes stand (own random stream)
    WaterDetector,          per-entity contact tracking
    WaterContact

com.coldfriend.environment  engine rendering of that data
    TerrainMeshBuilder      flat-shaded terrain mesh
    WaterMeshBuilder        ribbon / disc meshes per body (package-private)
    WaterView               node + one shared material + dispose()
    FoliageView             batched swaying bushes: node + one shared material + dispose()
    BushMeshBuilder         bush shape and per-vertex sway data (package-private)
    Wind                    wind values for the foliage shader
    SceneLighting           light values handed to the shaders (water, foliage)
    MeshCleanup             frees direct buffers

com.coldfriend.dev          developer tools, not the game
    WorldPreviewApp         fly camera and screenshots of a generated world

src/main/resources          MatDefs/Water.j3md + Shaders/Water.*, MatDefs/Foliage.j3md + Shaders/Foliage.*
```

## Dependency direction

```
dev -> environment -> world
dev -> world
environment -> world (reads WaterBody and GeneratedWorld; world never imports environment)
world -> (JDK only)
```

`world` has no jME imports. That is what makes it testable without an engine and keeps gameplay code independent of rendering.

## Separation of concerns

| Concern | Where | Rule |
|---|---|---|
| World data | `WaterBody`, `GeneratedWorld` | immutable, no engine types |
| Procedural generation | `WaterGenerator`, `BaseTerrain` | seed in, data out; deterministic |
| Rendering | `environment`, shader | reads data only; cannot change layout |
| Physics and detection | `GeneratedWorld` queries, `WaterDetector` | analytic; independent of meshes |
| Future interactions (drinking, collecting, ice breaking) | not implemented | build on `WaterContact` and `WaterBody` queries; do not read meshes |

There is deliberately no `WaterManager`. `GeneratedWorld` owns data and queries; `WaterView` owns render resources; `WaterDetector` owns per-entity state. Each has one job.

## Design decisions

**Analytic queries instead of physics objects.** The project has no physics library and a dependency was not approved. Water is long, thin and curved, which suits distance-to-polyline tests better than collision boxes. Queries run in microseconds, allocate nothing, and cannot drift from the visuals because both read the same `WaterBody` points. If a physics engine is added later, build static colliders from the terrain and ice tops and keep these queries for water state.

**One representation for every body.** Rivers, streams, lakes and ponds are all "polyline plus per-point width". A lake is a one-point polyline. This removes per-type branches from containment, carving and rendering.

**One shared carve function.** `TerrainCarver` is used by both the generator (to trace rivers over existing water) and `GeneratedWorld` (final terrain). Two copies would eventually disagree.

**Frozen state and flow in vertex data.** The world needs one material and one shader instead of one per state. The cost is that changing a body's frozen state needs its mesh rebuilt.

**Shader animates from engine time.** No per-frame Java work and no uniform updates, so there is nothing to allocate or schedule. Simulation state (water level, ice) is separate and static.

**Lighting passed in, not read.** The shader cannot see jME lights, so the scene owner gives `WaterView` and `FoliageView` a `SceneLighting`. This keeps the water correct at night and in interiors without the shader depending on scene setup.

See [FOLIAGE_WIND.md](FOLIAGE_WIND.md) for how bushes and wind work.

## Resource lifecycle

- `WaterView.dispose()` and `FoliageView.dispose()` detach their nodes and destroys its meshes' direct buffers. Call it when the world is destroyed.
- Terrain geometry from `TerrainMeshBuilder.build` is the caller's to release with `MeshCleanup.release(geometry.getMesh())`.
- `WorldPreviewApp.destroy()` shows both calls.
- Engine `Material` objects hold no GPU resources of their own.

## Conventions followed

Four-space indentation, named constants, Javadoc on public API, `-Xlint:all -Werror`, no `System.out` in game code, JUnit 5 tests in matching packages. Interfaces were not added; the existing rule (interfaces only at genuine subsystem boundaries) still holds.

## Not done

- No integration into `GameApp` or any game state: there is no gameplay world yet.
- Generation constants live in code, not `config/game.properties`.
- No chunking or streaming; the world is one 256 m tile.
