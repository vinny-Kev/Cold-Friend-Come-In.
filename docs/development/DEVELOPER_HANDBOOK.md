# Developer Handbook

The entry point for working on Cold Friend, Come In. Read this first, then go to the document for your task.

## Which document to read

| I want to... | Read |
|---|---|
| Set up the project, build it, run it, or fix a build error | [GETTING_STARTED.md](GETTING_STARTED.md) |
| Understand how the code fits together | [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md) |
| Change how a world is generated, or add a generation feature | [WORLD_GENERATION_GUIDE.md](WORLD_GENERATION_GUIDE.md) |
| Change scene, lighting, cameras, input or screens | [JMONKEYENGINE_GUIDE.md](JMONKEYENGINE_GUIDE.md) |
| Add or change an item, its stack limit or its model | [ITEM_SYSTEM.md](ITEM_SYSTEM.md) |
| Change inventory rules, the inventory screen, dropping or picking up | [INVENTORY_SYSTEM.md](INVENTORY_SYSTEM.md) |
| Change where items can go, or add a placement rule or placeable object | [WORLD_PLACEMENT.md](WORLD_PLACEMENT.md) |
| Write code that matches the project's style | [CODING_CONVENTIONS.md](CODING_CONVENTIONS.md) |
| Write or run tests, or check a world by eye | [TESTING_GUIDE.md](TESTING_GUIDE.md) |
| Add or change a model, texture or licence | [ASSET_PIPELINE.md](ASSET_PIPELINE.md) and [ASSET_MANIFEST.md](../../ASSET_MANIFEST.md) |
| See what is done, what is next, and what needs a decision | [DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md) |
| Know why a decision was made, or record a new one | [DECISIONS.md](DECISIONS.md) and the full log in [docs/DECISIONS.md](../DECISIONS.md) |
| Plan the sound | [docs/audio/AUDIO_DESIGN.md](../audio/AUDIO_DESIGN.md) and [AUDIO_ASSET_CATALOG.md](../audio/AUDIO_ASSET_CATALOG.md) |
| Check the game's design intent | [docs/GAME_DESIGN.md](../GAME_DESIGN.md) |

## The five things to know first

1. **The game runs.** `gradlew.bat run` opens the menu. New Game or `dev.autostart` generates a world and enters play.
2. **Worlds are reproducible.** A seed, a generation version and the parameters give the same world every time. Change one of them and the world changes.
3. **Gameplay lives in `WorldSimulation`.** The screen (`WorldPlayState`) reads input and draws. It does not decide anything.
4. **Gameplay refers to kinds, not files.** Scenery is set up in `assets\models\catalog.properties`.
5. **Warnings fail the build.** Compile with `-Werror`. Fix warnings, do not suppress them.

## Before you change code

1. Run `gradlew.bat build` and check it passes. Note the test count.
2. Read the package's `package-info.java` and the class's Javadoc.
3. Check `DEVELOPMENT_ROADMAP.md`: if the change needs an owner decision, ask first.
4. Check `docs/DECISIONS.md` and `DECISIONS.md` for any decision the change touches.

## Before you commit

1. `gradlew.bat clean build` passes.
2. The game starts and shows no `SEVERE` or `Exception` in the log.
3. `config\game.properties` has no dev keys you did not mean to ship (see GETTING_STARTED.md).
4. New assets are recorded in `ASSET_MANIFEST.md` with licence and source.
5. Docs that describe the code you changed are updated.

## Things that are easy to get wrong

- **World changes unexpectedly:** a generation stage changed, or a shared random stream was used. See WORLD_GENERATION_GUIDE.md.
- **A model looks wrong:** check its scale, tint and texture in the showcase (`gradlew.bat showcase`) before changing code.
- **Input keeps firing after a screen closes:** a mapping was not removed. See JMONKEYENGINE_GUIDE.md.
- **Settings change nothing:** volumes are stored but not applied, because audio does not exist yet.

## Where the tracking lives

- Progress and open questions: [DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md).
- Decisions and open decisions: [DECISIONS.md](DECISIONS.md).
- Assets and licences: [ASSET_MANIFEST.md](../../ASSET_MANIFEST.md).
- Audio research: [docs/audio/](../audio/).
