# Coding Conventions

- Java 21. Gradle toolchain enforces the language level.
- Four-space indentation, UTF-8 sources.
- Standard Java naming: `PascalCase` types, `camelCase` members, `UPPER_SNAKE_CASE` constants.
- One top-level type per source file.
- Explicit imports only. No wildcard imports.
- Small, focused methods. Split any class that becomes a god class.
- Prefer composition over inheritance. The only inheritance used is the engine's `SimpleApplication` and `BaseAppState`.
- Interfaces only at genuine subsystem boundaries (currently `ScreenNavigator`).
- Use records for immutable value types (`Resolution`, `GameSettings`, `BackdropSettings`).
- Handle exceptions explicitly. Log and fall back where the game can keep running; fail fast with a clear message where the configuration is wrong.
- Logging through SLF4J only. No `System.out` or `System.err`.
- No magic numbers. Put named constants at the top of the class. Tunable gameplay values belong in `config/game.properties`.
- Public types and methods that other subsystems rely on carry a short Javadoc comment.
- Dependencies are pinned in `gradle.properties`. Do not change versions without owner approval.
- Build flags: `-Xlint:all,-this-escape -Werror`. Warnings fail the build.
- Tests: JUnit 5, in `src/test/java` with the same package as the code under test. Test pure logic (models, parsing, persistence). Rendering is verified by running the game.
- Gameplay code must not depend on model file names or formats. Use asset interfaces when they exist.
