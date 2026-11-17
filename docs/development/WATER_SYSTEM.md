# Water System

What exists today, how it works, and how to extend or debug it. This describes the implemented code only.

## Overview

Water is world data first and a picture second. Everything gameplay needs (is this point in water, how deep, is it ice) is answered by plain Java in `com.coldfriend.world` with no engine or mesh dependency. `com.coldfriend.environment` draws the same data.

| Concern | Class | Package |
|---|---|---|
| Water body data | `WaterBody`, `WaterType` | `world` |
| Placement (procedural) | `WaterGenerator` (package-private) | `world` |
| Terrain with channels carved in | `TerrainCarver` (package-private), used by `GeneratedWorld` | `world` |
| World = terrain + bodies + queries | `GeneratedWorld` | `world` |
| Per-entity contact tracking | `WaterDetector`, `WaterContact` | `world` |
| Reusable query result | `WaterSample` | `world` |
| Water surface meshes + material | `WaterMeshBuilder`, `WaterView`, `WaterLighting` | `environment` |
| Terrain mesh | `TerrainMeshBuilder` | `environment` |
| Mesh buffer cleanup | `MeshCleanup` | `environment` |
| Shader | `MatDefs/Water.j3md`, `Shaders/Water.vert`, `Shaders/Water.frag` | resources |
| Inspection tool | `com.coldfriend.dev.WorldPreviewApp` | `dev` |

## What was there before

Nothing. The repository contained only the menu backdrop (a flat box ground, seeded trees, a cabin, snow) and empty `world`/`player`/`survival` package stubs. There was no terrain, no world generation, no player and no physics engine, so there was no water implementation to repair. This system and the minimal terrain it needs were added together.

## How water bodies are represented

A `WaterBody` is a centreline polyline with per-point data:

- `x`, `z`: centreline position, in flow order
- `halfWidth`: half the water width (the radius for a one-point body)
- `surface`: water surface height
- `depth`: depth at the centreline

plus `type` (`RIVER`, `STREAM`, `LAKE`, `POND`), `frozen`, and `iceThickness` (0 when not frozen).

Rivers and streams have many points. Lakes and ponds have exactly one, so a still body is a circle. One representation and one containment test serve every type.

Bodies are immutable. Frozen state is fixed when the world is generated.

Containment: a point is inside a body when its distance to the nearest point of the polyline is at most the interpolated half width there. The boundary is inclusive. This is exact for the data; it does not read the render mesh.

Flow: direction is the unit direction of the nearest segment. Speed comes from the surface slope, from `MIN_FLOW` 0.2 m/s to `MAX_FLOW` 3 m/s (`WaterBody.flowSpeed`). Still water reports zero.

## How rivers are generated

`WaterGenerator.generate(seed, base)` runs in this order:

1. **Lakes first.** The terrain is scanned on a 10 m grid for strict local minima. The lowest ones, at least 60 m apart, become lakes. The water level is the lowest ground found on rings around the lake, minus 0.15 m, so the level never sits above any part of the rim.
2. **Rivers, then streams.** Each starts at the highest of 60 random points that is not near another source or existing water. It then walks downhill:
   - Direction is the downhill gradient (sampled over 6 m) blended with the previous direction (momentum 0.7), which makes it meander.
   - The turn per 3 m step is capped at 0.35 rad (20°). This keeps the turning radius above the river's half width so its ribbon never folds over itself.
   - The surface is `min(previous surface, ground - 0.3)`, so it never rises and stays under the ground at the centreline.
   - Width ramps from start to end values with a sine variation.
3. **Where a river stops.** At the world edge. At the edge of an existing lake or river (it joins it). When it comes back within 8 m of its own earlier path. Or when the ground rises more than 2.5 m above the lowest point reached (a hollow). In a hollow it ends in a pond, unless that pond would overlap existing water. The river's last point takes the pond's level.
4. **Frozen state** is decided last, from a separate random stream (see Determinism).

Important detail: each river is traced over the terrain already carved by the water placed before it (`TerrainCarver`), not the raw noise. That is why rivers follow existing valleys and arrive at lakes at the right level. Placing against raw terrain left rivers hanging above their own banks next to lakes.

## Terrain integration

`BaseTerrain` is fractal value noise from the seed. `TerrainCarver.height(x, z)` returns the ground with channels carved in:

- **Inside water** the ground is a parabolic bed: `surface - depth * (1 - (d/w)^2)`. At the water's edge the bed equals the water level, so there is no vertical step at the shoreline.
- **On the bank** the ground blends from the water level back to the base terrain with a smoothstep. Bank width is `1.5 * halfWidth + 3`, plus 1 m per metre of height difference (capped at 12 m), so a river on a steep slope gets a wider, gentler bank instead of a cliff.
- **Overlapping bodies** blend with weights that fall steeply with distance from each body's water edge (`1 / (1 + d)^4`), so the body you are next to dominates and a distant one at a different elevation cannot drag its shore. Weights are continuous, so there are no seams.
- **Invariant:** inside any body's water the ground is never above that body's own bed, so terrain cannot poke through a water surface.

The same `TerrainCarver` serves the generator and `GeneratedWorld`, so generation and the final terrain agree.

## Determinism

`GeneratedWorld.generate(seed)` gives the same terrain and water for the same seed and `WaterGenerator.GENERATION_VERSION` (currently 1). Bump the version whenever a change alters layout for existing seeds.

Geometry uses one `java.util.Random`; frozen state and ice thickness use a second one. Changing freeze probabilities therefore never moves a river. Rendering code only reads `WaterBody` data and cannot affect generation.

## Rendering

`WaterView` builds one `Geometry` per body, all sharing one `Material` (`MatDefs/Water.j3md`). Meshes are built once. There is no per-frame work in Java: the shader animates from the engine's built-in time.

`WaterMeshBuilder` makes a ribbon (5 vertices across, one cross-section per centreline point) for rivers and streams, and a disc (centre plus two rings of 32) for lakes and ponds. Per-vertex data:

| Attribute | Meaning |
|---|---|
| `TexCoord` | ripple pattern coordinates in metres: across/along for ribbons, world x/z for discs |
| `Color.r` | shoreline fade, 0 at the edge to 1 inside (outer 20 % of the width) |
| `Color.g` | depth 0..1 following the same parabolic profile the terrain uses |
| `Color.b` | flow speed / 3 m/s |
| `Color.a` | 1 if frozen |

Because frozen state and flow live in vertex data, the whole world uses a single material and a single shader.

`Water.frag` does, per pixel: three travelling sine waves for a rippled normal (scaled up by flow speed and scrolled along the flow); a shallow-to-deep colour mix by depth; a Fresnel mix toward sky colour; a sun glint; a pale foam/snow line and alpha fade at the shoreline. Frozen bodies skip the ripples, use an opaque pale ice colour with faint crack lines, and have weaker reflection. The render state is alpha blend, no face culling, no depth write; geometry goes in the transparent bucket.

Lighting: the shader does not read jME light objects. The scene owner passes a `WaterLighting` (sun direction, sun colour, ambient, sky) to `WaterView.setLighting`, which updates uniforms only. `WaterLighting.day()` and `night()` are provided. Call it when your lighting changes; nothing else needs rebuilding.

Cleanup: `WaterView.dispose()` detaches the node and destroys the meshes' direct buffers (`MeshCleanup.release`). Do the same for the terrain geometry. The materials are plain objects with no GPU resources of their own.

## Collision and detection

The project has no physics engine and none was added. Water "collision" is analytic and lives in `GeneratedWorld`:

| Query | Returns |
|---|---|
| `terrainHeight(x, z)` | carved ground height |
| `waterBodyAt(x, z)` | body whose water contains the point, or null |
| `surfaceHeight(x, z)` | surface height, or NaN when not over water |
| `depthAt(x, z)` | surface minus carved bed, 0 when dry |
| `groundHeight(x, z)` | what a walker stands on: the ice top over frozen water, else the terrain |
| `sampleWater(x, z, out)` | fills a reusable `WaterSample` (body, margin, surface, depth, flow) |

These do not allocate. `waterBodyAt`/`surfaceHeight`/`depthAt`/`groundHeight` use an internal scratch object and are not thread-safe; use `sampleWater` with your own `WaterSample` from other threads.

Each query visits only bodies whose bounding box (including banks) contains the point, then tests segments of those bodies. This is linear in the segments of nearby bodies, which is cheap at the current scale (about ten bodies of up to a few dozen points).

`WaterDetector` tracks one entity. Call `update(x, feetY, z)` once per tick; it returns a `WaterContact` and exposes `body()`, `depth()`, `enteredWater()` and `leftWater()`:

- `IN_WATER`: inside unfrozen water at least 0.1 m deep, with feet at or below the surface (+0.05 m).
- `ON_ICE`: inside a frozen body, feet at or below the surface (+0.05 m).
- `DRY`: otherwise, including a shallow wet shoreline and being above the surface.

Irregular rivers are never approximated by one big box: containment follows the centreline polyline, so a bend is tested against the bend.

## Frozen water

`WaterBody.isFrozen()` and `iceThickness()` (metres). `isIceSafe()` is true for ice of at least 10 cm, a rule of thumb you should tune. Generation freezes ponds 90 %, lakes 85 %, streams 60 % and rivers 35 % of the time, with thickness 0.06 to 0.5 m.

Frozen and open water are distinguished by `WaterContact`, by `groundHeight` (ice is walkable at the surface, open water is not), and by the shader.

Not implemented: ice breaking, changing frozen state at runtime, thickness varying over time or space, and flow under ice. A frozen body keeps reporting its flow data; the shader simply does not animate it.

## Adding another water body type

1. Add a constant to `WaterType`, choosing `flowing` (needs two or more points) or not (one point).
2. In `WaterGenerator`, add a placement method and call it from `generate`. Reuse `build(...)` so frozen state comes from the second random stream, and add a case to `freezeChance` (the `switch` has no default, so the compiler will flag it).
3. Keep geometry decisions on the `random` stream only. If the new type changes layout, bump `GENERATION_VERSION`.
4. Place bodies against `ground(x, z)` (carved terrain) so they follow existing water, and end or reject placements that overlap existing water (`nearExisting`).
5. Rendering needs no change unless the type needs a new mesh shape: `WaterMeshBuilder` already draws any one-point body as a disc and any multi-point body as a ribbon.
6. Add the type to the tests' invariants (`GeneratedWorldTest`) and to this document.

## Preview tool

```bash
./gradlew worldPreview -PpreviewArgs="--seed 7 --view river --night"
```

Arguments: `--seed N`, `--view overview|river|stream|lake|ice|open`, `--night`, `--cam x,y,z --look x,y,z`, and `--shot path` (saves `path.png` then exits; the directory must exist). Without `--shot` it is a fly camera (WASD and mouse). It lists the generated bodies in the log. This is the manual visual check; it is a developer tool, not part of the game.

On this repository's machine Gradle's launcher JVM must be a JDK that Gradle 8.14 can run. JDK 25 fails with "Unsupported class file major version 69", so set `JAVA_HOME` to a JDK 21 for Gradle commands.

## Troubleshooting

**No water visible.** Check the log for the body list. Check that the water node is attached and that the camera is inside the world (`BaseTerrain.HALF_EXTENT`, 128 m). Check the log for shader compile errors from `Water.frag`; a failure there is reported when the first frame renders.

**Water looks flat or too bright or dark.** The shader uses only the values in `WaterLighting`. Call `setLighting` with values matching your scene. It does not read jME lights.

**Water floats or is buried.** The world-layer invariants are tested for eight seeds: the bed is below the surface, terrain never rises above a body's own bed inside its water, and the bank within 0.3 m of the edge is within 0.5 m of the local water level. If a new seed shows a defect, reproduce it with `GeneratedWorld.generate(seed)` and the same checks; a mismatch between visuals and queries means the mesh and the data have drifted.

**Spiky or sliver triangles on steep banks.** The terrain mesh is 170 cells over 256 m (about 1.5 m) with flat shading. Narrow channels on steep slopes alias against that grid. Raise `CELLS` in `TerrainMeshBuilder` (cost: vertices grow with the square) or accept it.

**Detection disagrees with what you see.** Detection uses the polyline and half width; the ribbon uses the same numbers, but at sharp bends the ribbon's mitred corners can differ slightly from the true distance test. The 20° turn cap keeps this small.

**Result changed after a generator edit.** Expected if layout changed. Bump `GENERATION_VERSION`.

**Gradle fails with "Unsupported class file major version 69".** Run Gradle on JDK 21, as above.

## Tests

`GeneratedWorldTest` (world layer, no engine): determinism, different seeds differ, every seed produces flowing and still water, bodies stay in the world and surfaces never rise, bed below surface, bank levels, smooth bank profile, exact boundary at the radius, river interpolation, surface and depth queries, frozen versus open height and detection, ice safety, `WaterDetector` enter/leave/ice/shoreline, bad-data rejection.

`WaterViewTest` (engine, no GL context): geometry per body, one shared material, mesh attributes present, dispose detaches and empties, lighting change does not rebuild.

Rendering itself (the shader) is verified by running `worldPreview`; it cannot be unit tested without a GL context.

## Known limitations

- Water level and ice are static. No tides, seasons or ice breaking.
- Frozen state cannot change at runtime without rebuilding that body's mesh.
- Water does not receive shadows (the project has no shadow system), so forest shadows do not darken it. It is tinted by the ambient and sun values it is given.
- Reflections are a Fresnel tint toward a single sky colour, not a reflection of the scene.
- No fog integration beyond the scene's own fog filter, and no refraction.
- Terrain has no skirt at the world edge, so a channel that leaves the world shows as a notch against the sky.
- Terrain mesh resolution (about 1.5 m) limits how narrow a channel can look good; streams are about 2.5 to 3.5 m wide.
- Rivers can curl briefly inside the hollow where they end.
- One lake spacing, count and size set; counts and sizes are constants in `WaterGenerator`, not yet in `config/game.properties`.
- World size is fixed at 256 m by 256 m. There is no streaming or chunking.
- Not integrated into the main game loop: there is no player or gameplay world yet, so `WaterDetector` has no caller outside tests.
