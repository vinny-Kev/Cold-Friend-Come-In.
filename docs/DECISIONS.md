# Decisions

Each entry: the decision, why, and what was considered. Entries marked **Open** need owner approval.

## D-001: Modular monolith, packages as subsystems

**Decision:** one Gradle module; subsystems are packages under `com.coldfriend`.
**Why:** a single desktop game with one developer. Separate modules add build overhead without a benefit yet.
**Alternatives:** multi-module Gradle build. Revisit if a subsystem needs independent release or tooling.

## D-002: jMonkeyEngine 3.9.0-stable with its own state and input model

**Decision:** screens are `BaseAppState` subclasses managed by the engine's `AppStateManager`. Input goes through jME's `InputManager`, wrapped by `InputController`.
**Why:** it is the engine's own lifecycle mechanism, so it needs no extra framework.
**Alternatives:** a custom screen stack (rejected: duplicates engine code); Lemur or Nifty GUI for menus (rejected: a new dependency, and the menu is small).

## D-003: Menus drawn with engine text, no GUI library

**Decision:** `MenuView` draws `BitmapText` rows and hit-tests them itself. Navigation logic lives in `MenuModel`, which has no engine dependency.
**Why:** the menus are a list of text items. A GUI library is not justified by this milestone, and the model is unit testable.
**Alternatives:** Lemur, Nifty GUI (both need owner approval; neither is needed yet). Revisit when there are real panels, inventory grids or text input.

## D-004: Settings stored as Java `.properties`, not JSON or YAML

**Decision:** `config/game.properties` (tuning, versioned) and `config/settings.properties` (player settings, git-ignored), both `java.util.Properties`.
**Why:** the brief asks for JSON or YAML. Java has no built-in JSON or YAML parser, and both need a library. `.properties` needs none and covers flat key/value configuration.
**Alternatives:** Gson (JSON), SnakeYAML (YAML), Jackson. Each needs owner approval.
**Open:** if you want JSON or YAML, approve one library and the files will move to it.

## D-005: Logging with SLF4J API and slf4j-simple backend

**Decision:** code logs through `org.slf4j`. Backend is `slf4j-simple`, configured in `src/main/resources/simplelogger.properties`.
**Why:** SLF4J is in the approved stack. The API alone prints nothing, so a backend is needed. slf4j-simple is the smallest one and has no configuration files beyond one properties file.
**Alternatives:** Logback (more features, more configuration; not needed yet).

## D-006: Distance fog via `jme3-effects` 3.9.0-stable

**Decision:** the menu scene uses jME's `FogFilter` (in `BackdropState`), fading distant geometry into the sky colour. Dependency `org.jmonkeyengine:jme3-effects:${jmeVersion}` added to `build.gradle`.
**Why:** the owner approved the dependency. It is the same engine version as the other jME modules.
**Status:** approved and added. Build and unit tests pass; the game starts with no errors. The visual effect has not been checked in a screenshot.
**Tuning:** fixed constants `FOG_DENSITY = 1.0` and `FOG_DISTANCE = 120` in `BackdropState` (shortcut: not yet in config).

## D-007: Volume settings stored, not applied

**Decision:** master, music and sound-effect volumes are read, changed and saved, but no audio is played.
**Why:** audio is not part of this milestone. Applying volumes now would need a mixing design that the owner has not approved.
**Alternatives:** wire volumes to jME audio nodes now (rejected: no audio exists to apply them to).

## D-008: Display changes apply by restarting the display

**Decision:** changing resolution, fullscreen or vsync stores the new settings, calls `setSettings` with fresh `AppSettings`, then `restart()`.
**Why:** jME's supported way to change window configuration at run time.
**Status:** the code path is written and reviewed. It has **not** been exercised at runtime, because the synthetic keyboard input used for testing did not reach the game (see the verification notes in the README).

## D-009: Procedural placeholder scene; no external assets yet

**Decision:** the menu backdrop is generated in code from boxes and faceted cones. No model files are loaded.
**Why:** the brief asks for placeholders and no external assets in this milestone.
**Note:** the only texture used is jME's built-in `Common/Textures/dot.png` for snowflakes.

## D-010: Assets folder registered with a file locator

**Decision:** `GameApp` registers `assets/` as a file locator. The folder tree is in the repo with `.gitkeep` files.
**Why:** the brief defines this structure, and external assets will live there.
