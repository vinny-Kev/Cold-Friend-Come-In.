# Simulation Time, Survival and the Hearth

This describes the code as it is after Milestone 04, Phase A. It is the reference for the fixed-step simulation, the game clock, a player's needs, the cabin stove, and interactable identity. The design reasoning is in [SURVIVAL_SYSTEM_DESIGN.md](SURVIVAL_SYSTEM_DESIGN.md); where the code differs from that design, the differences are listed at the end.

Status: **Implemented** with unit and simulation tests. Interactive play with keyboard and mouse has **not** been checked by hand; see [Verification](#verification).

## Fixed-step simulation

`world/FixedStep` turns frame times into whole simulation steps.

| Value | Constant | Meaning |
|---|---|---|
| 20 steps per second | `FixedStep.STEPS_PER_SECOND` | Every gameplay rule advances in steps of 0.05 s. |
| 5 steps per update | `FixedStep.MAX_STEPS_PER_UPDATE` | A long frame runs at most five steps (0.25 s of game time). |

- Time is counted in **whole nanoseconds**, so the same total elapsed time gives the same number of steps whatever the frame rate.
- **Excess time is dropped.** If a frame owes more than five steps (a window drag, a stall), the extra steps are discarded and counted in `droppedSteps()`. The game runs slower than real time for that moment instead of catching up in a burst.
- Zero, negative and NaN frame times count as no time.
- **Pause.** While a screen is paused (`WorldPlayState.setPaused(true)`, the pause menu, the inventory screen), the play screen stops calling `WorldSimulation.update`, so nothing advances: movement, clock, fire, needs. On resume it calls `resetFrameTiming()`, which forgets owed time, so there is no burst. The same happens in `onEnable`.
- **Time scale.** There is none. The frame time passed in is jME's `tpf`; the game never changes the engine's speed. Adding a time scale would mean multiplying the frame time before `FixedStep.advance`, in one place.

### Update order

`WorldSimulation.update(frameSeconds, inputs)`:

1. **Mouse look**, once per frame, for every living player (`PlayerMovement.look`). Look is a change of angle, not a rate, so it does not depend on frame rate, and the camera answers the mouse at once.
2. **Fixed steps**, as many as are due. Each step, in this order:
   1. Player movement (`PlayerMovement.move`, one step of 0.05 s). Dead players do not move.
   2. Clock (`WorldClock.advanceStep`).
   3. Hearth (`Hearth.update` with the step's game minutes).
   4. Each player's needs (`Survival.update`), using the clock's phase and the hearth's heat after this step.

Each subsystem is its own class; `WorldSimulation` owns them and calls them in this order. There is no event bus. A step allocates nothing.

### Drawing between steps

Players move 20 times a second; the screen may draw 60 or 144 times. `PlayerState` keeps the position at the start of the last step (`previousX/Y/Z`), and `WorldPlayState.followPlayer` blends from there to the current position using `WorldSimulation.interpolation()` (0 to just under 1). This is presentation only.

## Game clock

`time/WorldClock`, `time/DayPhase`, `time/ClockParameters`. The `time` package has no engine types and no dependency on `world`, so `survival` and later the creature can use it.

| Parameter (`ClockParameters.DEFAULT`) | Value |
|---|---|
| Real seconds per in-game day | 1200 (20 minutes); one game minute is 0.833 real seconds; one step is 0.06 game minutes |
| Dawn | 05:30 |
| Day | 07:00 |
| Dusk | 17:00 |
| Night | 19:30 |
| New game start | 07:00 on day 1 |

- Time is stored as a whole number of steps, so day changes happen at an exact step (`advanceStep()` returns true on the step that starts a new day). There is no rounding drift.
- The clock never reads the computer's clock. It only advances in simulation steps, so it stops while paused.
- Read it through `WorldSimulation.day()`, `minuteOfDay()`, `phase()` and `clockParameters()`. Only the simulation advances it.
- `dev.time` in `config/game.properties` now sets the **start time** (`day` 12:00, `dusk` 18:00, `night` 22:00, or `HH:MM`; blank is 07:00). It no longer picks a fixed lighting mode. Parsing is in `PlayOptions.parseStartTime`.
- **Lighting follows the clock** (`state/SkyLighting`, presentation only): it blends the `LightingProfile` colours: night until 05:30, toward day by 07:00, day until 17:00, dusk at 18:15, night from 19:30. The sky, fog, ambient light and sun are set every frame from reused objects.

## Needs

`survival/Survival` (one per player, owned by the simulation), `survival/SurvivalParameters`, `survival/SurvivalStatus` (read-only snapshot), `survival/Need`.

All four values are between 0 and 100 and are clamped after every change.

| Need | Starts | Changes |
|---|---|---|
| Warmth | 80 | Moves toward a target: rises 4 per game minute, falls 2 per game minute, stops at the target. |
| Nourishment | 100 | Falls 70 per in-game day. A ration restores 45. |
| Hydration | 100 | Falls 90 per in-game day. A drink restores 30. |
| Health | 100 | Falls 1 per game minute while one need is critical, 4 while two or more are. Recovers 1 per game minute while warmth, nourishment and hydration are all 40 or more. At 0 the player is dead. |

Warmth targets:

| Where | Target |
|---|---|
| Inside the cabin, strong fire | 85 |
| Inside, small fire or embers | 55 |
| Inside, no fire | 25 |
| Outside by day | 40 |
| Outside at dawn or dusk | 25 |
| Outside at night | 12 |
| Outside and wading (water deeper than 0.25 m) | 25 less than outside |

"Inside" is the cabin's footprint. An open door does not change the target (not designed yet).

Thresholds:

| Need | Warning ("low") below | Critical (harms health) below |
|---|---|---|
| Warmth | 35 | 20 |
| Nourishment | 30 | 10 |
| Hydration | 30 | 10 |
| Health | 50 | (never critical itself) |

- **Warning before death.** Every need passes its warning level before it is critical, and health only falls once a need is critical, so the HUD warns first. From full health, death takes at least 25 game minutes (about 21 real seconds) with two critical needs, and 100 game minutes with one.
- **Death.** At health 0, `SurvivalStatus.causeOfDeath()` names the critical need that was furthest below its threshold. A dead player cannot move, look, use items or interact. The HUD says how they died and points to Esc. There is no restart flow yet (Phase F).
- All rates are in `SurvivalParameters.DEFAULT`. They are the design's proposals and need playtesting. Note: outside at night, warmth goes from 80 to critical in 30 game minutes (25 real seconds), and to death about 100 game minutes later. That is harsh; tune it in playtest.

## The hearth

`survival/Hearth`, `survival/HearthParameters`, `survival/HearthStatus`, `survival/HearthState`, `survival/Heat`. The cabin has one hearth, owned by `WorldSimulation`; it is the stove in `CabinLayout`. Drawing (the stove's glowing fire door and the room light in `CabinView`) reads its level and never changes it.

| Parameter (`HearthParameters.DEFAULT`) | Value |
|---|---|
| Fuel per firewood | 1 fuel-hour |
| Most fuel the stove holds | 6 fuel-hours |
| Level per fuel-hour | 0.15 (so 2 fuel-hours is level 0.3, 6 is the cap of 0.9) |
| Strong heat at | level 0.3 or more, burning |
| Embers below | level 0.15 (fuel below 1) |
| Least burn while lit | 0.1 fuel-hour per hour |
| Embers last after the fuel is gone | 30 game minutes |
| A new world starts with | the stove lit, 3 fuel-hours (level 0.45) |

States: `UNLIT` (never lit), `BURNING`, `EMBERS`, `OUT`.

- A lit fire burns `max(level, 0.1)` fuel-hours per game hour. Because level is proportional to fuel, a fuller stove burns hotter and faster. Two pieces of wood (level 0.3) fall below strong heat at once; three pieces stay strong about 2.7 game hours; six pieces about 7.3.
- Below level 0.15 the fire is embers. Embers still warm the room a little (low heat). Feeding embers brings back flames without a match.
- When the fuel is gone, embers last 30 game minutes, then the hearth is `OUT`.
- Wood put in a cold stove does not burn until it is lit.
- **Lighting** needs a box of matches and one branch (kindling). The branch is used up; the matches are kept (see the differences below).
- Heat: `STRONG` when burning at level 0.3 or more, `LOW` when burning lower or embers, `NONE` when unlit or out.

Expected use: keeping a strong fire through a ten-hour night costs about 4 to 5 pieces of firewood (refuelling between 2 and 6 fuel-hours). The player starts with 4 in the kit and 3 by the stove.

## Actions

All actions are requests to `WorldSimulation`, which checks them and changes nothing if they fail. No item is lost or duplicated (tested).

| Key | Action | Code |
|---|---|---|
| E at the stove | If the stove is cold and has wood: light it (needs matches, uses one branch). Otherwise: add one firewood. Refused if there is no firewood or the stove is full. | `WorldSimulation.interact` with `InteractableRef.CABIN_HEARTH` → `SurvivalActions.tendHearth` |
| F | Use the selected item: eat a ration; drink from a full water container (it becomes empty); fill an empty container while standing in water or with open water 1.2 m ahead. | `WorldSimulation.useItem` → `SurvivalActions.use` |

- Eating is refused at full nourishment, drinking at full hydration, so items are not wasted.
- The water container has two item definitions in `config/items.properties`: `water_container` (full) and `water_container_empty`. Drinking or filling swaps one for the other, all or nothing.
- The stove is within reach within 1.6 m of its centre. When the door and the stove are both in reach, the nearer one is used. An item the player looks at always comes first.

## Interactable identity

`interaction/InteractableRef(kind, number)` is the stable identity of something a player can use.

- **Equality** is by kind and number (a record), so a ref made anywhere for the same thing is equal and hashes the same.
- **Fixed parts** have constants: `InteractableRef.CABIN_DOOR`, `InteractableRef.CABIN_HEARTH`.
- **Items** use their `EntityId`: `InteractableRef.item(id)`, and `ref.entity()` gives the id back. There is one identity system for things in the world (`EntityId`), not a second one.
- Refs never come from scene nodes or model files, so redrawing the world cannot change them. `WorldSimulation.interact` accepts a request only if its ref equals what the player can use right now.
- `Interactable` is now `(ref, x, z, reach)`; `kind()` reads the ref's kind. `InteractionRequest` is `(player, target)`. `InteractableKind` gained `HEARTH`.
- Removed: `WorldSimulation.CABIN_DOOR_ID` and `ITEM_PREFIX` (the old `"item:<id>"` strings).

## Presentation

- `ui/SurvivalHud`: top-left lines for day, time and phase; warmth, food, water and health; and the stove's state with about how many hours of wood are in it. A centred warning names the most urgent low need; red when critical or when health is falling. The text is rebuilt four times a second, not every frame.
- `WorldPlayState`: the prompt at the stove (`[E] Light the stove` or `[E] Add firewood (n left)`), `[F] Eat/Drink/Fill` in the selected-item line, the fire level passed to `WorldScene.setCabinFireLevel`, and the sky from `SkyLighting`.
- `CabinView`: the room light's strength follows the fire level (dark when out), and a glowing fire door on the stove shows while it burns.
- New development spawn: `dev.spawn=stove` (in front of the stove, facing it).

## Tests

| Test class | Covers |
|---|---|
| `world/FixedStepTest` | Same steps for the same time at different frame rates, irregular frames, step boundary, the cap and dropped time, bad frame times, reset, whole-simulation frame-rate independence, no burst after a pause |
| `time/WorldClockTest` | Start, day length, phase boundaries, midnight wrap and day count, stepping, determinism, bad parameters |
| `survival/SurvivalTest` | Start values, warmth targets, rise and fall rates, daily drain, health rules, warnings before critical, recovery, death, eat and drink, no time means no change, bounds under random input, determinism |
| `survival/HearthTest` | Start state, adding wood and capacity, burn rate, strong to low to embers to out, embers relit by feeding, lighting, relighting, no time means no burn, bounds under random input, determinism |
| `world/SurvivalSimulationTest` | The real simulation with the shipped items: start time and stove, feeding moves exactly one firewood, a full stove keeps the wood, no wood, relighting with matches and a branch, fire versus cold cabin, eating, drinking and filling, pause, frame-rate independence of clock, needs and fire, death after a warning, stable refs, out-of-reach requests |
| `interaction/InteractableRefTest` | Equality, hashing, kinds, entity ids |
| `state/PlayOptionsTest` | `dev.time` start times |

## Verification

- `gradlew build` with `-Werror`: passes. 24 test classes, 197 tests, 0 failures, 0 skipped (JDK 21; Gradle 8.14.3 cannot run on the machine's default JDK 25, set `JAVA_HOME` to a JDK 21).
- The game was started three times with `dev.autostart` and `dev.screenshot` (seed 404): at 07:00 inside the cabin, at 22:00 in front of the stove, and at 22:00 from in front of the cabin. The frames show the HUD (day, time, needs, stove), the night sky, the stove's glowing fire door and the warm room light. The logs had no errors apart from known asset warnings (glTF models and footstep WAVs are not in the repository, so placeholders were used).
- Keyboard and mouse were not driven by the developer. In two of the runs the frames showed input the developer did not send (the camera had turned, one firewood had gone into the stove and the door had been closed), most likely from someone using the machine while the window had focus. So E at the stove and at the door did work in the real game, but this was not a controlled check. **A hand playtest is still needed**: feed and relight the stove, eat, drink, fill at the river, pause and resume, die of cold, and watch a full day pass.

## Differences from the design

| Design | Code | Why |
|---|---|---|
| `WorldClock` in `survival` | In a new `time` package | Avoids a package cycle (`world` → `survival` → `world`); the creature will need it too |
| One match used per light | The box of matches is kept; one branch is used | `matches` is one item, "a small box", with a stack of 1. Counting matches would need item durability, which the item system does not have |
| Fill only at `WATER_ACCESS` locations | Fill wherever there is open water at the player's feet or 1.2 m ahead | Simpler to play; any river is water. Revisit if water points should matter |
| `water_full` item | `water_container` is the full flask, `water_container_empty` the empty one | Keeps the existing item id and its `Drink` verb |
| Start fuel not set in the design | 3 fuel-hours | With 2 the fire started exactly at the strong threshold and fell to low heat at once (seen in the first run) |
| River drink, chimney, felling trees, axe | Not built | Not in Phase A's scope |
| Wind, open door, wet clothes affecting warmth | Not built | Postponed in the design |
