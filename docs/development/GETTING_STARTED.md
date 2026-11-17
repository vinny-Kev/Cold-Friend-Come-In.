# Getting Started (Windows)

This guide sets up the project on Windows, builds it, runs it, and tests it. Every command was run on the development machine. Paths in angle brackets are for you to fill in.

## What you need

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 | Gradle uses a Java 21 toolchain. Install Temurin 21 or any JDK 21. |
| Gradle | 8.14.3 | Use the wrapper (`gradlew.bat`). It downloads Gradle the first time. |
| IntelliJ IDEA | 2025.x (Community or Ultimate) | Used for editing and running. Optional for command-line work. |
| Git | any recent | The project is a git repository. |

The project declares its Java version in `build.gradle` (`JavaLanguageVersion.of(21)`), so Gradle looks for a JDK 21 itself. If it cannot find one, set `JAVA_HOME` to a JDK 21 folder.

## The Gradle wrapper

`gradlew.bat` runs the Gradle version in `gradle/wrapper/gradle-wrapper.properties`. The first run downloads Gradle into `%USERPROFILE%\.gradle\wrapper\dists`. Dependencies go into the Gradle cache.

On the development machine the wrapper's download timed out once, so the build was run with a local Gradle 8.14.3 instead. If the wrapper fails the same way, unzip Gradle 8.14.3 somewhere and run its `bin\gradle.bat` in place of `gradlew.bat`, from the project folder.

## Open the project in IntelliJ

1. **File → Open**, choose the project folder (the one containing `build.gradle`).
2. Choose **Open as Project** if asked. IntelliJ imports the Gradle build.
3. Wait for indexing and the Gradle sync to finish (bottom status bar).
4. Check **File → Project Structure → Project SDK** is a JDK 21. Set it if not.
5. Check **Settings → Build, Execution, Deployment → Build Tools → Gradle** uses the Gradle JVM set to JDK 21.

Source folders are already set: `src/main/java` and `src/test/java`. Resources are in `src/main/resources`.

## Build

From the project folder in PowerShell or Command Prompt:

```bat
gradlew.bat build
```

This compiles with `-Xlint:all,-this-escape -Werror` (warnings fail the build), runs all tests, and creates `build\libs\cold-friend-come-in-0.1.0-SNAPSHOT.jar`.

Compile without running tests:

```bat
gradlew.bat compileJava
```

## Run the game

```bat
gradlew.bat run
```

Run it from the project folder. The game reads `config\game.properties` relative to the working directory, and Gradle's `run` task sets this up.

In IntelliJ, open `src/main/java/com/coldfriend/Main.java` and press the green run arrow. Check that the run configuration's working directory is the project folder.

## Run tests

```bat
gradlew.bat test
```

Test results are in `build\reports\tests\test\index.html` (open in a browser) and `build\test-results\test\*.xml`. Only failures are printed in the console. See `TESTING_GUIDE.md` for details.

To run one test class:

```bat
gradlew.bat test --tests com.coldfriend.ui.MenuModelTest
```

## Clean build artifacts

```bat
gradlew.bat clean
```

This deletes `build\`. `.gradle\` (project-local Gradle cache) is also safe to delete if builds misbehave, but it is git-ignored and will be rebuilt.

## Inspect a build failure

1. Read the first `error:` or `FAILED` line in the console. The task name is shown above it.
2. For a compile error, the console gives the file and line. Open it from the console link in IntelliJ.
3. For a test failure, read the full stack trace printed under `FAILED`. Test logs are in `build\test-results\test\`.
4. For a Gradle or dependency problem, run with `--stacktrace` or `--info`:

```bat
gradlew.bat build --stacktrace
```

5. If a warning fails the build (`-Werror`), the warning text is printed just before `BUILD FAILED`. Fix the warning rather than removing `-Werror`.

## Verify the game is running correctly

Expect this in the console:

- `Main - Starting COLD FRIEND, COME IN. with settings ...`
- `GameApp - Main menu ready`

If a world is started at launch (for example `dev.autostart=true` in `config\game.properties`), you will also see:

- `WorldGenerator - Generating world: seed=...`
- `WorldGenerator - World ready: ...`
- `WorldPlayState - World ready to play: ...`

Errors to look for: `SEVERE`, `ERROR`, or any `Exception` in the log. Ignore the jME `Cannot find loader` lines for formats the game does not use (the Ogre and OGG loaders). The OGG line is expected until the audio milestone.

## Development settings in `config\game.properties`

The file contains developer keys. Check them before you commit or ship:

| Key | Meaning |
|---|---|
| `world.seed` | Seed for a new world. Empty means a random seed. |
| `dev.autostart` | `true` starts a world at launch, skipping the menu. |
| `dev.spawn` | Where the player starts: `cabin`, `cabinview`, `cabindoor`, `village` or `river`. |
| `dev.showFps` | Shows the frame rate. |
| `dev.screenshot` | Takes one screenshot to this path, then exits. **Set this to a path on your machine, or leave it empty.** |

Player settings are saved to `config\settings.properties`. That file is git-ignored; delete it to restore defaults.

## Controls in the game

| Action | Keys |
|---|---|
| Move | W A S D or arrow keys |
| Sprint | Shift |
| Look | Mouse |
| Interact (door) | E |
| Pause | Esc |

Menus use the arrow keys or W/S, Enter or Space to confirm, and Esc to go back. The mouse also works.

## Troubleshooting

- **`Cannot load configuration`** at launch: `config\game.properties` is missing or a key is malformed. The log names the key.
- **Toolchain error mentioning Java 21:** install JDK 21 and set `JAVA_HOME`.
- **Missing textures or models** (pink or grey objects): check `assets\models\catalog.properties` and the file paths. See `ASSET_PIPELINE.md`.
- **Window does not appear or the game closes at once:** read the console for `SEVERE` lines. Check the graphics driver is current.
