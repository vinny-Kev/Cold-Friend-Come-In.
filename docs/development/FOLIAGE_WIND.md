# Foliage and Wind

Bushes that sway in the wind. This describes the implemented code only.

## Overview

Same split as water: where bushes stand is plain data in `com.coldfriend.world`; drawing and animating them is in `com.coldfriend.environment`. The swaying is done entirely in a vertex shader from engine time, so there is no per-frame Java work.

| Concern | Class |
|---|---|
| Where a bush stands | `BushSpawn` (record), `BushPlacer` (`world`) |
| Bush shape | `BushMeshBuilder` (package-private) |
| Batched rendering, material, cleanup | `FoliageView` |
| Wind values | `Wind` (record) |
| Light values | `SceneLighting` (shared with water) |
| Shader | `MatDefs/Foliage.j3md`, `Shaders/Foliage.vert`, `Shaders/Foliage.frag` |

## Placement

`BushPlacer.place(world)` returns an immutable list of `BushSpawn` (position, ground height, yaw, scale, variant, tint).

- Deterministic for the world's seed and `BushPlacer.PLACER_VERSION`. It uses its own random stream, so placing bushes never changes terrain or water, and every random value is drawn for every attempt so a rejected attempt cannot shift the next one.
- 700 attempts; rejected if within `SHORE_CLEARANCE` (2.5 m) of any water's edge or within `MIN_SPACING` (3 m) of another bush. About 500 to 600 bushes result per world.
- Height is `GeneratedWorld.terrainHeight`, so bushes follow the carved ground.

## Shape

`BushMeshBuilder.template(variant)` builds one of `BushPlacer.VARIANTS` (3) shapes: a clump of 4 to 6 faceted blobs (three rings plus apexes), flat-shaded, standing on y = 0. The same variant index always gives the same shape. Snow is vertex colour on near-horizontal faces, leaves elsewhere.

Each vertex carries:

| Attribute | Meaning |
|---|---|
| `TexCoord.x` | sway weight 0..1: `(height / 1.4 m)^2`, so roots barely move and tips move most |
| `TexCoord.y` | leaf phase 0..1, random per blob |
| `Color` | base colour |
| `Normal` | flat face normal |

## Wind in the shader

`Foliage.vert` offsets each vertex horizontally, in world space:

- A wave travels along the wind direction (`sin(time * 1.3 - along * 0.18)`), so a gust visibly rolls across the bushes instead of everything moving together. A slower envelope makes gusts come and go.
- The push is `strength * (0.6 + 0.4 * wave) + gust * envelope * (0.5 + 0.5 * wave)`.
- A small fast flutter, perpendicular to the wind and offset by each blob's phase, shakes leaf clusters out of step.
- Offset is multiplied by the sway weight and by `MAX_SWAY` (0.35 m at strength 1), and the tips droop slightly as they bend so branches do not stretch.

Because motion is a function of world position and `g_Time`, batched bushes need no per-bush data beyond the vertices.

`Wind` is `(dirX, dirZ, strength, gust)` with presets `calm()`, `breezy()` and `storm()`. `FoliageView.setWind(wind)` writes three uniforms; nothing is rebuilt. Wind is one global value today.

Lighting: the shader does not read jME lights. Pass a `SceneLighting` to `FoliageView.setLighting` (ambient plus one sun term, matching the flat low-poly look). Fog from the scene's fog filter applies as usual.

## Performance

Bushes are baked into one mesh per 64 m square of the world and share one material, so a world is at most 16 draw calls (tested: batches are far fewer than bushes). Culling works per batch. Triangles per bush are about 100, so a world is roughly 50,000. Wind and lighting changes are uniform writes only.

## Cleanup

`FoliageView.dispose()` detaches the node and destroys the meshes' direct buffers. `WorldPreviewApp.destroy()` shows the call.

## Preview and verification

```bash
./gradlew worldPreview -PpreviewArgs="--seed 1 --view bush --wind storm --shot build/shots/a --shot-frame 45"
```

`--wind calm|breezy|storm` (default breezy), `--view bush` (camera at the first bush), `--shot-frame N` (frame to capture). A still screenshot cannot show motion, so motion was verified by capturing two frames of the same view: they differ (storm frame 45 versus 100: 876 differing sampled pixels) and storm differs more from calm (2,315). Feel (speed, amplitude) has not been judged beyond that and should be tuned by running the preview live.

## Tuning

Constants at the top of `Foliage.vert`: `MAX_SWAY`, `DROOP`; wave speed and spatial frequency are the literals in `main`. Presets live in `Wind`. Shape constants are at the top of `BushMeshBuilder`.

## Adding another foliage type

1. Build vertex arrays with the same attributes (sway weight and phase in `TexCoord`, colour in `Color`, normals), weight growing from 0 at the roots.
2. Add placement data and a placer in `world` on its own random stream; keep clear of water with `sampleWater`.
3. Batch it through `FoliageView` (or a sibling) using the same material.
4. Add tests across several seeds and update this document.

## Known limitations

- Sway is visual only: no collision, and walking through a bush does nothing.
- Wind is global and constant in direction. No per-area wind, no shelter behind hills, no link to weather or gameplay yet.
- Bushes do not react to snow load or to the player.
- Free-falling leaves are not implemented.
- The menu backdrop trees (`TreeFactory`) do not sway; they are a separate fixed scene and were not changed.
- Foliage is not shadowed (the project has no shadow system).
- Bushes are not wired into any game state, because there is no gameplay world yet.
