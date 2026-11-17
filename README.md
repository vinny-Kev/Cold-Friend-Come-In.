# Cold Friend, Come In

A first-person, atmospheric winter survival horror game, built with Java 21 and jMonkeyEngine 3.9.0.

This repository contains the project foundation (main menu, settings, backdrop) and the first playable world: a seeded, procedurally generated winter wilderness with rivers, forest, a cabin and a village site, which you can walk around in first person. There is no survival gameplay yet. See `docs/GAME_DESIGN.md` for what is implemented and what is planned.

## Requirements

- **Windows 10 or 11** (the only platform verified so far).
- **JDK 21.** Gradle is set up with a Java 21 toolchain and will look for a JDK 21 install. If Gradle cannot find one, install a JDK 21 distribution (for example Eclipse Temurin 21) and set `JAVA_HOME` to it before running the commands below.
- Internet access on the first build, so Gradle can download dependencies.

Gradle itself does not need to be installed; the project includes a wrapper.

## Build

```bash
./gradlew.bat build
```

This compiles the code with `-Xlint:all -Werror` (warnings fail the build) and runs the unit tests.

## Run

```bash
./gradlew.bat run
```

The game must be started from the project folder, because it reads `config/game.properties` relative to the working directory. Gradle's `run` task does this automatically.

## Controls (in the world)

| Action | Keys |
|---|---|
| Move | W / A / S / D or the arrow keys |
| Look | Mouse |
| Run | Shift |
| Interact | E (open or close the cabin door; pick up the item you are looking at) |
| Inventory | I or Tab; number keys 1 to 9 select a slot |
| Drop | Q drops one of the selected item, Shift+Q the whole stack |
| Place | P places the selected item: aim, R or the wheel rotates, click or Enter places, right click or Esc cancels |
| Pause | Escape (Resume, Settings, Quit to Menu) |

## Worlds and seeds

New Game generates a world from a seed. Leave `world.seed` blank in `config/game.properties` for a different random world each time, or set it to a number or any text to get the same world again. The seed, generation version and parameter fingerprint are written to the log. The same seed and generation version always give the same world.

You start inside the cabin with the door shut. For development, `dev.spawn` (cabin, cabindoor, cabinview, village or river) picks where you start, `dev.showFps` shows the frame rate, `dev.autostart` starts a world straight from the menu, `dev.time` (day, dusk or night) sets the light, `dev.scenario` (inventory, drop or placement) runs a short script for screenshots, and `dev.screenshot=<path>` makes the game save one picture of its own frame there after a few seconds and quit. The screenshot option never captures anything outside the game window.

## Controls (menus)

| Action | Keys | Mouse |
|---|---|---|
| Move selection | Up / Down, W / S | Hover |
| Confirm | Enter, Space | Left click |
| Change value | Left / Right, A / D | Left click (adds one step) |
| Back | Escape | |

## Configuration

- `config/game.properties` holds tuning values: default settings and the menu backdrop (tree count, snow particle count, camera sway). Edit it and restart the game.
- `config/settings.properties` is written by the game when you choose **Apply** on the settings screen. It is ignored by git. Delete it to return to defaults.

## Settings

| Setting | Behaviour |
|---|---|
| Master, Music, Sound Effects volume | Saved. **Not applied yet**, because audio is not implemented. |
| Resolution | Applied on **Apply**; restarts the display. |
| Fullscreen | Applied on **Apply**; restarts the display. |
| VSync | Applied on **Apply**; restarts the display. |
| Fog, Snow (Off / Light / Normal / Heavy), Camera Sway (On / Off) | Applied immediately on **Apply**; no restart. Defaults come from `settings.default.fog`, `.snow` and `.cameraSway` in `config/game.properties`. |

**Back** discards unapplied changes.

## Project layout

```
assets/          externally sourced assets (models, textures, audio, ...)
config/          game.properties (tuning); settings.properties is created at run time
docs/            design, architecture, conventions, decisions
src/main/java/   game code, one subsystem per package under com.coldfriend
src/main/resources/   logging config, engine-facing resources
src/test/java/   JUnit 5 tests
```

## Verification status

| Check | Result |
|---|---|
| `gradlew build` compiles with `-Werror` | Passes |
| Unit tests (17 in 4 classes) | Pass |
| Distance fog (`jme3-effects`) | Build passes and the game starts without errors. Visual result not yet checked in a screenshot. |
| Game starts on JDK 21, main menu shown | Verified (screenshot and log) |
| New Game generates a world, you can walk in it, Escape returns to the menu with the backdrop restored | Verified in the real game window (screenshots and log), seeds 101, 202, 404 and random |
| Rivers, water, fallen logs, village blocks and the cabin block draw in the world | Verified in screenshots, seeds 101 and 404 |
| Frame rate in the world | About 600 FPS on the development machine (one measurement) |
| Cabin interior, door, lights and the pause menu draw and work | Verified by in-game screenshots (seed 404 and 101); the door logic is also covered by unit tests |
| Mouse look | **Not verified at runtime.** Synthetic mouse movement could not be sent to a captured cursor. |
| Mouse click on "Settings" opens the settings screen | Verified (screenshot) |
| Keyboard navigation and value changes | **Not verified at runtime.** Automated key presses did not reach the game window in the test session. The keyboard path uses the same dispatcher as the mouse path, which is verified. |
| Apply restarts the display with a new resolution | **Not verified at runtime.** Code is in place (`GameApp.saveAndApplySettings`). |
| Exit via the Quit item | **Not verified at runtime.** Quit calls `stop()`; the item was not clicked in the test session. |

## Known limitations

- Volumes are stored but not applied (no audio yet).
- The menu backdrop is placeholder geometry.
- Most item models are placeholders (a magenta cube); only the stone has a model. There are no inventory icons.
- The village buildings are hollow shells with transparent windows but no usable doors. The cabin has an interior (bed, stove, table), a door, glowing windows, and warm lights; its furniture cannot be used yet.
- No snowfall in the world yet, no seed entry screen (use `world.seed`).
- The bush texture is autumn red; it is tinted darker but not green.
- Only Windows has been tested.

## Developer documentation

Start with the [Developer Handbook](docs/development/DEVELOPER_HANDBOOK.md). It says which document to read for each task.

- [Getting started](docs/development/GETTING_STARTED.md): setup, build, run, test, IntelliJ.
- [Architecture guide](docs/development/ARCHITECTURE_GUIDE.md): packages, lifecycle, screens, input, world, player.
- [World generation](docs/development/WORLD_GENERATION_GUIDE.md): seeds, versions, stages, adding features.
- [jMonkeyEngine guide](docs/development/JMONKEYENGINE_GUIDE.md): scene, materials, lights, input, AppStates.
- [Coding conventions](docs/development/CODING_CONVENTIONS.md) and [testing guide](docs/development/TESTING_GUIDE.md).
- [Asset pipeline](docs/development/ASSET_PIPELINE.md) and [asset manifest](ASSET_MANIFEST.md).
- [Roadmap](docs/development/DEVELOPMENT_ROADMAP.md) and [decision log](docs/development/DECISIONS.md).
- [Audio catalogue](docs/audio/AUDIO_ASSET_CATALOG.md) and [audio design](docs/audio/AUDIO_DESIGN.md) (research only, nothing downloaded).

## Troubleshooting

- **Toolchain error mentioning Java 21:** no JDK 21 was found. Install JDK 21 and set `JAVA_HOME`.
- **`Cannot load configuration`** when starting: `config/game.properties` is missing or a key is malformed. The log names the key.
- **Gradle wrapper download times out:** download Gradle 8.14.3 manually, unzip it, and run its `bin\gradle.bat build` in place of `gradlew.bat`.
