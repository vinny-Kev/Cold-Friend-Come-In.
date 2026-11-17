# Developer Decision Log

This file records decisions that affect how developers work. The full decision log, with rationale and alternatives for the early decisions, is `docs/DECISIONS.md` (D-001 to D-011). This file adds decisions made since, and marks the ones that still need owner confirmation.

**Confirmed** means the owner decided it. **Recorded from code** means the decision is visible in the code and comments, but the owner has not stated it. Confirm those before relying on them.

| ID | Decision | Status | Rationale | Alternatives considered | Reference |
|---|---|---|---|---|---|
| D-001 | Modular monolith: packages are subsystems, one Gradle module | Confirmed | One developer, one desktop game | Multi-module build | `docs/DECISIONS.md` |
| D-002 | jMonkeyEngine 3.9.0 with `BaseAppState` screens and its input system | Confirmed | The engine's own lifecycle | Custom screen stack, Lemur, Nifty | `docs/DECISIONS.md` |
| D-003 | Menus drawn with engine text; menu logic in plain classes | Confirmed | Menus are text lists; logic is testable | Lemur, Nifty | `docs/DECISIONS.md` |
| D-004 | Configuration in Java `.properties`, not JSON or YAML | Confirmed; open | No library needed | Gson, SnakeYAML, Jackson | `docs/DECISIONS.md` |
| D-005 | Logging through SLF4J with `slf4j-simple` | Confirmed | Approved stack | Logback | `docs/DECISIONS.md` |
| D-006 | Distance fog with `jme3-effects` | Confirmed (owner approved) | Real fog | Sky colour and overlay | `docs/DECISIONS.md` |
| D-007 | Volumes stored, not applied | Confirmed (until audio exists) | No audio yet | Wire volumes to nodes now | `docs/DECISIONS.md` |
| D-008 | Display changes restart the display | Confirmed; runtime check outstanding | jME's supported way | None | `docs/DECISIONS.md` |
| D-009 | Procedural placeholder scene | Still applies to the menu backdrop. The play world uses real models from the catalogue | Placeholders first | None | `docs/DECISIONS.md` |
| D-010 | `assets/` registered with a file locator | Confirmed | The brief defines the folder | None | `docs/DECISIONS.md` |
| D-011 | glTF and FBX loading with `jme3-plugins` 3.9.0 | Confirmed (owner approved) | Asset packs are glTF | Convert everything to GLB first | `docs/DECISIONS.md` |
| D-012 | Settings file written through a temporary file, then moved over the real one | Recorded from code | A crash cannot leave a half-written file | Write in place | `SettingsStore.save` |
| D-013 | Seed plus generation version plus parameters identify a world; a changed value changes the world | Recorded from code. Owner should confirm | Reproducible worlds for testing and bug reports | Random worlds with a saved copy | `WorldGenerator`, `GenerationVersion` |
| D-014 | One random stream per generation stage, derived from seed, version, attempt and stage name | Recorded from code. Owner should confirm | A change in one stage does not disturb another | One shared random source | `RandomStreams` |
| D-015 | Generation retries up to 8 attempts; the attempt number is part of the world's identity | Recorded from code. Owner should confirm | Keeps the world deterministic when validation fails | Fail on the first failure | `WorldGenerator` |
| D-016 | Gameplay state in `WorldSimulation`, which knows nothing about rendering or devices | Recorded from code. Owner should confirm | Testable now, and could run on a server later | Put state in the screen | `WorldSimulation` javadoc |
| D-017 | Prop kinds, not file names, are what gameplay uses; models set in `catalog.properties` | Recorded from code. Owner should confirm | Visual assets stay replaceable | Hard-code paths | `PropCatalog`, `catalog.properties` |
| D-018 | Use `Lighting.j3md` with simple lit materials for scenery, not the PBR materials from models | Recorded from code | Cheaper and works with the scene's plain lights | Keep PBR materials | `ModelCatalog` javadoc |
| D-019 | Temporary `showcase` Gradle task and `dev` package for checking assets | Temporary. Remove when no longer needed | Check model scale and look | Blender for every check | `build.gradle`, `dev/AssetShowcase.java` |
| D-020 | Development keys (`dev.*`) in `config\game.properties` | **Open.** Decide which stay | Speed up testing | Separate dev config file | `config\game.properties` |
| D-021 | Footsteps use WAV files only; the OGG loader is not on the classpath | Recorded from code | Playing OGG needs the `jme3-jogg` module, which is not approved | Add `jme3-jogg` (needs approval) |`audio/Footsteps.java` |
| D-022 | Footsteps are not positional yet; the sound plays at the listener | Recorded from code. Owner should confirm | Simplest first step; positional audio comes with the creature cues | Positional `AudioNode` now |`audio/Footsteps.java` |
| D-023 | Footstep volume is master × sound-effects volume; this is the first place the stored volumes take effect | Recorded from code. Owner should confirm | Settings promised volumes would apply; footsteps are the first sound | Wait for a full mixer |`WorldPlayState.apply` |
| D-024 | One footstep per 2.5 m walked, using the Fast WAV when sprinting and the Medium WAV when walking | Recorded from code. Owner should confirm the stride and speed rule | Tuning value; easy to change | Time-based steps |`audio/StepCounter.java` |
| D-025 | Footsteps use single-step clips cut from the pack, not the pack's 11 to 19 s files | Recorded from code. The pack files played whole walks on every step | A step must sound like one step | Use the pack files whole, or play them from a timer |`audio/Footsteps.java`, `assets/audio/footsteps/snow/` |
| D-026 | Wind is generated in code (`audio/WindLoop`), not downloaded | Recorded from code. Owner should confirm | No licence or download needed; the sound is synthetic and may need tuning by ear | Download a CC0 or CC-BY wind clip |`audio/WindLoop.java`, `audio/Ambience.java` |

## Open questions for the owner

- Confirm D-004 (properties, or a library), D-013 to D-017 (world and architecture choices), and D-020 (development keys).
- Decide the day and night, fire, music and creature decisions listed in `DEVELOPMENT_ROADMAP.md`.

## How to add a decision

Add a row with an ID one higher than the last. Say whether it was **Confirmed** by the owner or **Recorded from code**. Include the rationale and what you considered. If a decision changes an earlier one, mark the earlier row as superseded rather than deleting it.
