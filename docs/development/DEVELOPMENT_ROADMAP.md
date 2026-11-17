# Development Roadmap

Based on the repository as it stands. Labels: **Completed** (built and tested or checked), **Partial** (some of it works), **Not implemented**, and **Needs decision** (the owner must decide before work starts).

## Completed

| Area | Evidence |
|---|---|
| Gradle project on Java 21 with pinned jMonkeyEngine 3.9.0 | `build.gradle`, `gradle.properties`, `gradlew build` passes |
| Main menu, settings screen, pause menu | Checked by running the game (menus and settings screenshots, earlier milestone) |
| Settings persistence (saved, validated, written safely) | `SettingsStoreTest`, `GameSettingsTest` |
| Config loading with clear errors | `GameConfigTest` |
| Winter menu backdrop with fog | Runs at launch; fog enabled with `jme3-effects` (owner approved) |
| Seeded world generation: terrain, rivers, regions, vegetation, cabin, village, trail, validation | `WorldGeneratorTest`, `PlacementTest`, `CabinTest`; runs at launch and enters play |
| First-person world screen with camera, fog and pause | Runs at launch |
| First-person movement rules | `PlayerMovementTest` |
| Prop catalogue with model loading and fallbacks | Runs; models shown in the showcase |
| Models loaded in the world: trees, rocks, bushes | Runs at launch with the catalogue |
| Items, inventory, dropping, picking up, placing | `InventoryTest`, `WorldItemTest`, `ObjectPlacementTest` |
| Fixed-step simulation (20 Hz), game clock, day and night lighting | `FixedStepTest`, `WorldClockTest`; night lighting seen in a screenshot (Milestone 04 Phase A) |
| Needs (warmth, food, water, health) and the stove (fuel, embers, out, lighting) | `SurvivalTest`, `HearthTest`, `SurvivalSimulationTest`; HUD and stove glow seen in screenshots |

## Partial

| Area | What works | What is missing |
|---|---|---|
| Cabin | Layout, doorway, door opens and is drawn | Not yet playtested end to end. Check in-game that the prompt appears at the door and E opens it. |
| Interaction | E uses the door, world items and the stove; F uses the selected item (eat, drink, fill). Identity is `InteractableRef` | Not playtested by hand end to end |
| Survival loop | Clock, needs, stove, eating, drinking, filling water, death message | Rates not playtested; no restart flow, no firewood gathering, no chimney, no sounds for the fire |
| World quality | Worlds generate and validate | The owner has not approved how the worlds look |
| Models | Trees, rocks, bushes, cabin walls in use | No fallen logs; no approved hut; characters are placeholders |
| Asset manifest and licences | Recorded for everything in use | Some textures' licences are unconfirmed (the Retro houses) |

## Not implemented

- **Audio beyond footsteps:** wind, fire, creature cues, music, positional audio and a full mixer. Footsteps on snow are wired (WAV only). See `docs/audio/AUDIO_DESIGN.md`.
- **Survival beyond Phase A:** gathering firewood (felling trees), the chimney, death and restart, the seven-day structure. See `SURVIVAL_GAMEPLAY_ROADMAP.md`.
- **Narrative:** notes and storytelling; package is a placeholder.
- **Creature:** behaviour, perception and cues; package is a placeholder.
- **Save and load:** worlds are regenerated from their seed each time.
- **Multiplayer:** a future architectural extension. `WorldSimulation` is written so it could run on a server, but nothing networked exists.

## Needs decision (owner)

1. **Day and night:** how long a day lasts, and what changes at night.
2. **The fire:** whether it must be tended, and what happens when it goes out.
3. **Music:** how much, and in which moments.
4. **The creature:** its first appearance and the rules it follows. Not designed.
5. **The world's look:** approve or change the look of generated worlds.
6. **Characters:** the current characters are placeholders. A grounded, less cartoonish set is wanted, and free assets are required.
7. **Config format:** keep `.properties` (see `docs/DECISIONS.md` D-004) or approve a JSON or YAML library.
8. **Development keys:** `dev.autostart`, `dev.spawn`, `dev.screenshot` are on in `config\game.properties`. Decide which stay for development and which are removed before any release.

## Prototype path

The intended order (from the design) is:

1. **Stable foundation:** done.
2. **Seeded procedural wilderness:** generation runs; approve the look.
3. **Generated village and cabin placement:** placed; check with the owner's approval of the layout.
4. **First-person player controller:** movement and camera done; add the interaction loop from the player's side.
5. **Interaction system:** started with the cabin door; generalise it to other objects.
6. **Survival systems:** not started. Needs the owner's decisions on warmth and the fire first.
7. **Environmental audio:** not started. Design written; catalogue started (see `docs/audio/`).
8. **Creature behaviour:** not started. Needs design. Keep it simple at first.
9. **Narrative progression:** not started.
10. **Polish and optimisation:** later. Measure before optimising.
11. **Multiplayer:** later, as an extension. Do not start it before the single-player prototype is playable.

## Next recommended step

Playtest Phase A by hand (feed and relight the stove, eat, drink, fill at the river, pause and resume, die of cold, watch a full day) and tune `SurvivalParameters` and `HearthParameters`. Then start Phase B (creature perception and memory) as planned in `SURVIVAL_GAMEPLAY_ROADMAP.md`. Owner decisions on day length and the fire (items 1 and 2 above) are now implemented with the proposed defaults and should be confirmed.
