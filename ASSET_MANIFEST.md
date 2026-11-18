# Asset Manifest

Every third-party asset in the project, with its source and license evidence. Add a row for each approved asset.

**Current status:** the game can load glTF files (`jme3-plugins` is now a dependency), but none of these models is placed in the scene yet.

## Approved assets

| Asset | Category | Project path | Source | License (evidence) | Original format | Downloaded | Original file | Size on disk |
|---|---|---|---|---|---|---|---|---|
| KayKit Forest Nature Pack, bare trees (Tree_Bare_1_A/B/C, Tree_Bare_2_A/B/C) | Trees | `assets/models/environment/trees/kaykit_forest/` | [kaylousberg.itch.io/kaykit-forest](https://kaylousberg.itch.io/kaykit-forest) | CC0 1.0. `License.txt` in the pack and the itch.io "Asset license" field. Credit is requested, not required. | glTF 2.0 (`.gltf` + `.bin`, texture `forest_texture.png`) | 2026-11-17 | `KayKit_Forest_Nature_Pack_1.0_FREE.zip` (6,435,630 bytes) | 6 models, about 0.2 MB, plus `forest_texture.png` (49 KB) |
| Stylized Nature MegaKit (Standard), dead trees (DeadTree_1–5) | Trees | `assets/models/environment/trees/quaternius_megakit/` | [quaternius.itch.io/stylized-nature-megakit](https://quaternius.itch.io/stylized-nature-megakit) | CC0 1.0. `License_Standard.txt` in the pack. | glTF 2.0 (`.gltf` + `.bin`, textures `Bark_DeadTree.png` and `Bark_DeadTree_Normal.png`) | 2026-11-17 | `Stylized Nature MegaKit[Standard].zip` (104,088,529 bytes) | 5 models, about 2 MB, plus two bark textures (about 10 MB) |
| KayKit Adventurers (free 2.0), characters Barbarian, Knight, Mage, Ranger, Rogue, Rogue_Hooded | Characters (player) | `assets/models/characters/` | [kaylousberg.itch.io/kaykit-adventurers](https://kaylousberg.itch.io/kaykit-adventurers) | CC0 1.0. `KayKit_Adventurers_License.txt` in this folder (copied from the pack). | GLB (also FBX in the pack) | 2026-11-17 | `KayKit_Adventurers_2.0_FREE.zip` (13,024,345 bytes) | 6 models, about 2.5 MB |
| Retro House Pack, houses house1 to house5 (GLB) | Buildings | `assets/models/structures/` | [elegantcrow.itch.io/retro-house-pack](https://elegantcrow.itch.io/retro-house-pack) | Models CC0 (page). The zip has no license file. Textures: AmbientCG (CC0) plus Pixabay, Pexels and Unsplash (own licenses). **Texture sources not yet confirmed per file.** | GLB (also FBX and OBJ in the pack) | 2026-11-17 | `retro_house_pack.zip` (35,444,428 bytes) | 5 models, about 0.3 MB total. The GLB files have materials only, no texture images. The pack's 45 PNG textures were not copied and are not used by these files. |
| House with Thatched Roof (venturon), thatched hut | Buildings | `assets/models/structures/` | [venturon.itch.io/house-with-thatched-roof](https://venturon.itch.io/house-with-thatched-roof) | CC0 (page). Free. | GLB (also FBX in the pack) | 2026-11-17 | `HouseBeams12.glb` (105,196 bytes) | 1 model, 105 KB. Size about 6.5 × 4 × 6.6 units. The file has one embedded texture, although the page says material colours only. |
| Footsteps in snow (kirrifant), slow, medium and fast, WAV and OGG | Audio: player footsteps on snow | `assets/audio/footsteps/snow/` | [kirrifant.itch.io/footsteps-in-snow](https://kirrifant.itch.io/footsteps-in-snow) | CC0 (page). The zip has no licence file. Recreated sounds, not field recordings. | WAV and OGG | 2026-11-17 | `Footsteps_in_snow.zip` (6,667,922 bytes) | 6 files, about 8.3 MB. Seamless loops per the page. The original files are 11 to 19 s of continuous walking, so the game does not play them directly. The game uses the single-step clips below. The OGG files are not played (the engine's OGG loader is not on the classpath). |
| Single-step clips cut from the Footsteps in snow WAV files (derived, by the project) | Audio: footsteps | `assets/audio/footsteps/snow/` (`snow_step_fast.wav`, `snow_step_medium.wav`, `snow_step_slow.wav`) | Derived from the kirrifant pack above | Same as the source pack (CC0 per the page) | WAV | 2026-11-17 | Cut from the three source WAV files by the project's own tool | 0.38 s, 0.87 s and 0.49 s clips. The slow clip is not used yet. |
| KayKit Adventurers rigs, Rig_Medium_General and Rig_Medium_MovementBasic | Animation rigs | `assets/models/characters/` | same as above | CC0 1.0 (same pack) | GLB | 2026-11-17 | same zip | 2 files, about 1.5 MB |
| Red SUV, PSX style, with interior (`suvinterior.glb` from the zip, saved as `parked_car.glb`) | Vehicle (parked car) | `assets/models/vehicles/parked_car.glb` | [som-natalino.itch.io/red-suv](https://som-natalino.itch.io/red-suv) | CC0 1.0 Universal (page; the author confirmed the update to CC0 in the comments). Credit not required. Free download; the payment page was declined, nothing paid. | GLB (also FBX, OBJ in the zip) | 2026-11-18 | `Red Suv - PSX Car` zip (387,106 bytes), file `suvinterior.glb` (210,756 bytes) | 1 model. Nodes include Interior, SteeringWheel, Shifter, doors, trunk and wheels; textures embedded. |

## Not used

| Asset | Reason |
|---|---|
| Quaternius Textured Lowpoly Trees | Only FBX, OBJ, Blend. No GLB or glTF. Conversion needs Blender, which is not installed. |
| Kenney Nature Kit | Formats and snow trees not confirmed on the source page. Not downloaded. |
| Kenney Holiday Kit | Holiday theme, likely off-brief. Formats not stated. Not downloaded. |

## Decisions made

- **`jme3-plugins` 3.9.0-stable** added to `build.gradle` (owner approved). It provides the glTF, GLB and FBX loaders. The game can now read these files, but nothing loads them into the scene yet.

## Notes

- Files are kept under their original names. Each pack has its own folder, so names do not clash.
- The MegaKit Standard zip is also in the Downloads folder and is not in the project.
