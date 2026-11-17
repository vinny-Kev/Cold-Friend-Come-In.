# Cold Friend, Come In

A first-person, atmospheric winter survival horror game, built with Java 21 and jMonkeyEngine 3.9.0.

This repository currently contains the **project foundation** only: a working main menu, a settings screen, and a procedural winter backdrop. There is no gameplay yet. See `docs/GAME_DESIGN.md` for what is implemented and what is planned.

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
| Mouse click on "Settings" opens the settings screen | Verified (screenshot) |
| Keyboard navigation and value changes | **Not verified at runtime.** Automated key presses did not reach the game window in the test session. The keyboard path uses the same dispatcher as the mouse path, which is verified. |
| Apply restarts the display with a new resolution | **Not verified at runtime.** Code is in place (`GameApp.saveAndApplySettings`). |
| Exit via the Quit item | **Not verified at runtime.** Quit calls `stop()`; the item was not clicked in the test session. |

## Known limitations

- Volumes are stored but not applied (no audio yet).
- The menu backdrop is placeholder geometry.
- Only Windows has been tested.

## Troubleshooting

- **Toolchain error mentioning Java 21:** no JDK 21 was found. Install JDK 21 and set `JAVA_HOME`.
- **`Cannot load configuration`** when starting: `config/game.properties` is missing or a key is malformed. The log names the key.
- **Gradle wrapper download times out:** download Gradle 8.14.3 manually, unzip it, and run its `bin\gradle.bat build` in place of `gradlew.bat`.
