# Milestone 04: Documentation Updates to Apply

Status: **Superseded.** This was written during planning, when the handbook, guides and decision logs looked uncommitted. After the branch was fast-forwarded to `origin/main` they are tracked, and Phase A updated them directly. The decision entries were recorded in `docs/DECISIONS.md` as D-027 to D-034 (not D-017 to D-027 as proposed below, because `docs/development/DECISIONS.md` already used D-012 to D-026). Keep this file only as a record of the planning.

## Why this is a separate file

The existing project documents that the brief asks to update are **not tracked in this branch**. In the main checkout they exist only as uncommitted files:

| Requested update | Existing file (main checkout, uncommitted) | Also present |
|---|---|---|
| Developer handbook | `docs/development/DEVELOPER_HANDBOOK.md` | none |
| Architecture guide | `docs/development/ARCHITECTURE_GUIDE.md` and `docs/ARCHITECTURE.md` (committed, stale) | two architecture files |
| Development roadmap | `docs/development/DEVELOPMENT_ROADMAP.md` | none |
| Decision log | `docs/DECISIONS.md` (committed, D-001 to D-015, working tree adds D-016) and `docs/development/DECISIONS.md` (uncommitted) | **two decision logs** |

Editing those files here would copy the user's uncommitted work into a branch, and a later merge would conflict with the untracked originals. So the text is set out below. Apply it in the main checkout, or in the implementation branch after the world code is committed.

**Decision needed (housekeeping):** keep one decision log. Proposed: `docs/DECISIONS.md` is the log (it is the one `ARCHITECTURE.md` points to, and it holds D-016). Merge the entries from `docs/development/DECISIONS.md` into it, and delete the other.

---

## A. Developer handbook: add a section "Milestone 04 planning"

Add near the top, after the setup section:

> **Planning documents for survival and the creature.** The plan is in `docs/development/SURVIVAL_GAMEPLAY_ROADMAP.md`. Read it with `CREATURE_AI_DESIGN.md`, `SURVIVAL_SYSTEM_DESIGN.md` and `AI_TESTING_STRATEGY.md`. Everything in these documents is **proposed** until the owner approves the open decisions in the roadmap. Facts about the current code are labelled with their source file.
>
> **Rules for the new systems:**
> - Simulation code (`survival`, `creature`, `player`, `world` except `world.render`) has no `com.jme3` imports. A test checks this.
> - Gameplay values live in `*Parameters.DEFAULT` records, not in `config/game.properties`. The `dev.*` keys stay in the config.
> - Simulation time advances only by the fixed step. Never use frame time to change a gameplay rule.
> - The creature decides from its memory only. Do not give it `PlayerState` directly.
> - Tests use fixed seeds and the scripted harness described in `AI_TESTING_STRATEGY.md`. Do not validate AI with screenshots alone.

Also add to the "How to run tests" section: performance tests carry the JUnit tag `performance`; run quick tests with `gradlew test -PexcludeTags=performance` once the tag is wired (Phase A task; not yet wired).

## B. Architecture guide: replace the subsystem table rows and add

Replace the status column for these rows (main checkout's current state, verified in source):

| Package | Responsibility | Status |
|---|---|---|
| `world` | `WorldSimulation`: players, inventories, world items, cabin door, interactions, placement. Moves players at frame time (fixed step planned, Phase A). | Implemented |
| `world.query` | Walkability, collision (grid of circles, footprints, door, placed items), grid navigation (reachability only) | Implemented; runtime path finding planned (Phase B) |
| `world.items` | Item actions and placement validation | Implemented |
| `player` | Player identity, input, movement (no engine types) | Implemented |
| `interaction` | `Interactable`, `InteractionRequest`, `ActionResult`; door and item pickup | Implemented |
| `item`, `inventory` | Item definitions from `config/items.properties`, stacks, inventory slots | Implemented |
| `audio` | Footsteps (WAV), wind loop | Partial |
| `survival` | Clock, warmth, nourishment, hydration, health, hearth, cue log | Planned (Phase A, see `SURVIVAL_SYSTEM_DESIGN.md`) |
| `creature` | Creature state, perception, memory, brain, behaviours | Planned (Phases B and C, see `CREATURE_AI_DESIGN.md`) |
| `narrative` | Notes and storytelling | Planned, not designed |

Add a new section:

> **Simulation layers (proposed, Milestone 04).** `WorldSimulation` owns the authoritative state and calls, in order, the clock, survival, hearth and creature subsystems once per fixed step (20 Hz). Each subsystem is its own class; `WorldSimulation` does not implement their rules. Perception runs at 5 Hz, the creature brain at 1 Hz, and movement every step. The only shared channel between subsystems is a per-tick list of `SenseEvent` values, cleared at the end of each tick. There is no event bus.

Add to "Known gaps":

> - The simulation has no game clock; `dev.time` only changes lighting (Phase A).
> - `WorldSimulation.update` uses frame time (Phase A, fixed step).
> - `audio.StepCounter` holds simulation logic and should move to `player` (Phase A).
> - `InteractionRequest` identifies targets by string; planned to become `InteractableRef` (Phase A).
> - `WorldData` is immutable, so felled trees need a simulation overlay (Phase A).
> - `WorldPlayState` is about 550 lines. New gameplay HUD must not add rules to it.

## C. Development roadmap: replace "Not implemented" survival and creature lines and "Prototype path" steps 6 and 8

Replace step 6 and step 8 of the prototype path with:

> 6. **Survival systems:** planned in `docs/development/SURVIVAL_SYSTEM_DESIGN.md` (Milestone 04, Phase A). Blocked on the owner's decisions 1 and 2 in `SURVIVAL_GAMEPLAY_ROADMAP.md`; defaults are proposed.
> 8. **Creature behaviour:** planned in `docs/development/CREATURE_AI_DESIGN.md` (Milestone 04, Phases B to E). Perception and memory come before any behaviour. Owner decision 5 needed before Phase C.

Add to "Not implemented", replacing the Survival and Creature bullets:

> - **Survival:** clock, warmth, nourishment, hydration, health, the hearth's fuel and state, firewood by chopping. Planned, Milestone 04 Phase A.
> - **Creature:** perception, memory, behaviour, disguises. Planned, Milestone 04 Phases B to E.

Add a row to "Needs decision (owner)" if not already listed: the seven-day structure and the death flow (roadmap open decisions 3 and 4 in the Milestone 04 plan).

## D. Decision log: new entries

Each entry is **Proposed** until the owner approves. Numbering continues from D-016 in `docs/DECISIONS.md`.

## D-017: The simulation owns a fixed-step clock; rendering reads it

**Decision:** `WorldSimulation.update` accumulates frame time and advances the simulation in fixed steps of 1/20 s, with a cap on steps per frame. Gameplay rules never use frame time. The game clock (`WorldClock`) is part of the simulation.
**Why:** creature decisions, survival rates and fuel burn must not change with frame rate, and multiplayer later needs one tick rate.
**Alternatives:** keep frame time and scale rates (rejected: makes tuning depend on the machine); a separate server thread now (rejected: no networking is planned).
**Status:** Proposed. Phase A.

## D-018: Survival and creature are separate classes that the simulation calls in order; no event bus

**Decision:** `survival` (`WorldClock`, `Hearth`, `Survival`, `CueLog`) and `creature` (`CreatureSimulation` and its parts) are plain classes. `WorldSimulation` calls them once per fixed step in a fixed order. The only shared channel is a per-tick `SenseEvent` list, produced by the simulation and read by the creature, then cleared.
**Why:** keeps each rule testable on its own, avoids a giant manager (the brief forbids them), and avoids an event bus that would hide the order of updates.
**Alternatives:** an event bus with subscribers (rejected: hidden order, harder to test deterministically); all rules inside `WorldSimulation` (rejected: it would become the giant manager).
**Status:** Proposed. Phase A and B.

## D-019: Creature decisions use memory only; the information barrier is a test

**Decision:** the creature's brain and behaviours read `Memory`, perception output and world data. They never read `PlayerState`. Perception is the only code that reads the player's position, and it writes observations. A test runs the brain with two different hidden player positions and requires identical decisions.
**Why:** the brief requires imperfect information. A rule in a document is not enough; the test makes it enforceable.
**Alternatives:** trust the implementer (rejected); give the creature noisy copies of the player position (rejected: still magical knowledge).
**Status:** Proposed. Phase B.

## D-020: Creature behaviour is a nine-mode finite state machine with scored mode choice, hysteresis and commitment times

**Decision:** modes are an enum (ROAM, INVESTIGATE, OBSERVE, STALK, CIRCLE, PROBE, HOLD, STRIKE, WITHDRAW). At a 1 Hz brain tick, each mode is scored from considerations; the best mode wins if it beats the current mode by 15% and the current mode's commit time has passed. Each mode is a small class with `enter`, `update` and `exit`.
**Why:** named modes are easy to log, debug and test. Scores give variety without flicker. A behaviour tree or GOAP would add a framework the project does not need for this many modes.
**Alternatives:** pure FSM (transition table explodes); behaviour tree (framework without a clear need); GOAP (hard to tune and explain); utility only (no stable intent, restraint is hard to express).
**Status:** Proposed. Phase C.

## D-021: Perception uses one line-of-sight function for both directions; the player's view uses camera yaw and pitch and world geometry together

**Decision:** the player's "watching the creature" test is the camera cone (yaw and pitch) and the same line-of-sight test the creature uses (2D circles and footprints via `WorldCollision.lineClear`, and 3D terrain occlusion via `HeightField`). Visibility of the creature on screen follows the same rule.
**Why:** the brief says camera direction is not line of sight. Sharing one function keeps the two consistent and testable.
**Status:** Proposed. Phase B.

## D-022: Memory confidence decays linearly per observation kind, and observations merge within 8 m

**Decision:** observations (SIGHT, SOUND, TRACK, CHANGED, LIGHT, SMOKE, KNOCK) are kept in a bounded list of 32. Confidence decays linearly with age over a lifetime per kind. Same-kind observations within 8 m merge.
**Why:** linear decay is exact and testable; bounded lists avoid per-tick allocation.
**Status:** Proposed. Phase B.

## D-023: Survival is a minimal set: warmth, nourishment, hydration, health, and a hearth with fuel

**Decision:** the first playable version tracks warmth, nourishment, hydration and health, plus the hearth's state and fuel. Stamina, sickness, wet clothing, weight, weather and sleep are postponed. Health falls only while a survival value is critical, so every death follows a visible warning.
**Why:** the brief asks for meaningful decisions without chores. Each kept value supports a decision (the fire, the river, the cold); each postponed one would add upkeep without a new decision.
**Alternatives:** the full list of meters (rejected: tedious; not justified by the design).
**Status:** Proposed; values are proposals, pending owner decisions 1 and 2.

## D-024: Gameplay tuning lives in `*Parameters.DEFAULT` records, not in `config/game.properties`

**Decision:** creature and survival values are in `CreatureParameters.DEFAULT` and `SurvivalParameters.DEFAULT`. `config/game.properties` keeps development keys (`dev.*`) and the existing display and menu values.
**Why:** creature and survival values are part of the game's balance. Editing them in a properties file would change behaviour silently, the same reason D-012 keeps generation parameters out of the config. The values are still easy to find and change in one place, and tests refer to them by name.
**Alternatives:** properties file (rejected for the reason above); JSON (needs a library, see D-004).
**Status:** Proposed. Revisit if the owner wants to tune without rebuilding.

## D-025: Disguises are data on a form enum; the creature's identity and memory do not change with form

**Decision:** `CreatureForm` (WOLF, BEAR, HUMAN) is an enum holding speed, sensing multipliers, a set of abilities, and a model key. `CreatureState` keeps the same `EntityId` and `Memory` across forms. No class hierarchy per disguise.
**Why:** the brief asks for composition and says identity must not depend on appearance. Enum data is the smallest structure that does this.
**Status:** Proposed. Phase E (WOLF only before that).

## D-026: Felled trees and placed items use an overlay in the simulation; generated world data stays immutable

**Decision:** felling a tree records its placement index in a simulation set, and `WorldCollision` removes that tree from its grid. `WorldData` is not changed.
**Why:** `WorldData` is the same for the same seed (D-012, D-013). Player changes belong to the simulation, which can be saved or sent later.
**Status:** Proposed. Phase A.

## D-027: Stable identifiers: `EntityId` for things in the world, `PlayerId` for beliefs about players

**Decision:** the creature gets an `EntityId` allocated the same way as world items (`WorldItems` counter pattern, or one shared allocator). Memory is keyed by `PlayerId` when a player is involved. Interactable identity moves from strings to an `InteractableRef` (door, or item `EntityId`).
**Why:** the brief asks for stable ids and future multiplayer compatibility. Strings for ids are fragile (the item branch currently parses one back into a number).
**Status:** Proposed. Phase A (`InteractableRef`), Phase B (creature id).

---

## E. Owner decisions still open (copy of the roadmap's list)

1. Day length and the phases of the day (proposed 20 real minutes per day).
2. The fire: must be tended; burns down and goes out (proposed).
3. Seven-day structure, and when the shaman's arrival would be.
4. Death flow: same seed from 07:00 (proposed) or a new seed.
5. Creature first appearance and its rules (proposed: none before the first night; never seen in daylight within 40 m).
6. Start with the stove lit or unlit.
7. Whether a river drink (no container, cold) is an acceptable choice.
8. Whether chopping should be quieter, to avoid punishing the same action twice.
