# Creature Phase B: Plan

Status: **Phase B implemented** (steps 1 to 7 below), with the deviations listed in section 9. Phase C has not been started. Written after Phase A was merged. Read it with [CREATURE_AI_DESIGN.md](CREATURE_AI_DESIGN.md), which holds the full design. This document says what Phase B builds, in what order, and what must be decided first.

## 1. Goal of Phase B

One creature exists in the world. It can see, hear and notice traces, remembers what it noticed for a while, and moves around the forest on its own. It does **not** attack, circle the cabin or react to the player yet. Behaviour beyond wandering is Phase C.

What the player can see in Phase B: nothing, apart from a debug overlay (`dev.creature=true`) that draws the creature's cone, its last known point of the player and its mode. Phase B is verified by tests, not by play.

## 1b. Check against the design intent (Creature Behavior brief)

The brief asks for a patient predator that studies the player, avoids being seen, and never cheats. Phase B lays the foundation for that. Everything else in the brief is mapped to a later phase below, so nothing is lost and nothing is built early.

| Brief item | Where it lands |
|---|---|
| Separate: player sees creature / creature sees player / obstruction / creature thinks it is watched / recently observed | Phase B, step 2: one perception result with these as separate facts |
| Hearing gives approximate position, not an exact marker | Phase B, step 2 (position jitter); already in the plan |
| Evidence: tracks, disturbed ground, resources, things left outside | Phase B, step 1 and 3 (TRACK, CHANGED); food left outside is a later sense event (Phase E, bear) |
| Memory of what was actually perceived, with confidence and age | Phase B, step 3 |
| Never reads the player's position directly; a test enforces it | Phase B, step 6 (already required) |
| Can lose the player and investigate the wrong place | Phase C (investigation); Phase B memory is built so this is possible: stale or wrong entries are kept until they decay |
| Seek cover when watched, reposition, follow out of view | Phase C (stalking, withdraw); Phase B supplies the "watched" fact they need |
| Nine modes with scored selection and hysteresis | Phase C; Phase B has one mode (wander) so the selection code is not needed yet |
| Cabin observation, chimney clogging, door tests | Phase D |
| Wolf, bear, human disguises, human only at night, hidden ending | Phase E. Phase B and C keep the form as data (see section 5) so these can be added without rewriting the brain |
| Terrain, cover and elevation shape the route | Phase C. Phase B's path search is cached and planned only when the target changes, so this can be added later |
| Sound drives memory, not a marker | Phase B step 2 |
| Fairness: limited perception, imperfect memory, no cheating | Phase B step 2 and 6; the fairness checks come in Phase D |

## 2. What already exists (verified in source)

| Need | Already there | Where |
|---|---|---|
| Game time and phase (dawn, day, dusk, night) | `WorldClock`, `DayPhase` | `time/` |
| Fixed 20 Hz steps | `FixedStep`, `WorldSimulation.step` | `world/` |
| Player position and look direction (yaw, pitch) | `PlayerState` | `player/` |
| Line of sight across scenery and buildings | `WorldCollision.lineClear(...)` (2D, circles and footprints) | `world/query/` |
| Ground height for 3D occlusion | `HeightField.heightAt(x, z)` | `world/model/` |
| Walkable ground | `TerrainRules.walkable(...)`, `WaterMap` | `world/query/`, `world/model/` |
| Moving a body and sliding along obstacles | `WorldCollision.move(...)` | `world/query/` |
| Reachability flood fill | `GridNavigation` (no path search at run time) | `world/query/` |
| A* path search | `PathFinder`, **package-private**, used only for path generation | `world/gen/` |
| Stable ids | `EntityId`, allocated in order by `WorldItems` | `entity/`, `world/items/` |
| Seeded randomness | `RandomStreams` (per stage) | `world/gen/` |
| Footsteps as events | `StepCounter` (stride distance) in `audio/`, not yet a simulation event | `audio/` |

Not there yet, and needed: a creature package, sense events from the simulation, a run-time path search, and creature state in `WorldSimulation`.

## 3. Decisions to confirm before coding

These change the code. Each has a proposed default.

1. **Sight cone and range.** Proposed: creature sight 120° wide, 60° tall; range 60 m by day, 35 m at dusk and dawn, 15 m at night (from `DayPhase`). The player counts as *watched by the player* when their look direction is within 35° horizontally and 25° vertically of the creature and line of sight is clear.
2. **Where the creature starts.** Proposed: a seeded spot 150 m or more from the cabin, outside the village, on walkable ground.
3. **Whether the creature moves in daylight.** Proposed: yes, slowly and far away (40 m or more from the player). Its design doc says "never seen in daylight within 40 m".
4. **Path search.** Proposed: promote `PathFinder` to a public class in `world/query` with a node budget of 4000 and a 4 m cell size. Alternative: copy it. Promote is less code.
5. **Creature speed.** Proposed: wander 1.4 m/s; never faster than the player's walk (3.4 m/s) in Phase B.

## 4. Build order

Each step has its tests before the next starts.

### Step 1: Sense events from the simulation (small)

- New `creature/SenseEvent` record: kind, x, z, loudness, time (game minutes).
- `WorldSimulation` collects events for a footstep (player moved one stride), a door open or close, an item drop, pick-up or place, and a firewood stack appearing. The list is cleared at the end of each step.
- Move `StepCounter` into `player/` (or read stride distance inside the simulation). Keep `Footsteps` as the audio consumer.
- **Tests:** a door toggle produces one DOOR event; a drop produces one DROP event; events are cleared after the step; no events when the player stands still.

### Step 2: Perception (the core of the barrier)

- `creature/Perception.sense(creature, player, world, phase)` returns observations. It does not store them.
- Vision: cone test, range by phase, 2D line of sight from `WorldCollision.lineClear`, and 3D terrain occlusion by sampling `HeightField` every 2 m (cap 50 samples).
- Hearing: an event is heard if its distance is within its loudness. Each solid wall crossed multiplies the loudness by 0.35. Confidence is capped at 0.9, and the position is jittered by up to 1.5 m with a seeded stream.
- Perception runs every fourth fixed step (5 Hz).
- The result is one value, `Sighting`, with these **separate** facts. They are kept apart on purpose, because the brief needs the creature to tell them apart:
  - `playerCanSeeCreature`: the player's look cone covers the creature, within range, with clear line of sight.
  - `creatureCanSeePlayer`: the creature's own cone covers the player, within range, with clear line of sight.
  - `blocked`: line of sight is cut by scenery, a building, or terrain. This is the same test for both directions, so the two stay consistent.
  - `creatureBelievesWatched`: the creature sees the player looking its way, within 25 m, with clear line of sight. This is what the creature acts on.
  - `recentlyObserved`: time since the player last could see the creature (kept in memory, step 3).
- **Tests:** cone edges at 35° and 36°; range boundaries at 59 m, 61 m and night 14 m; a tree blocks and a clearing does not; a ridge blocks; hearing through a wall; the 0.9 cap; jitter is repeatable for the same seed; a close player who is facing away has `creatureCanSeePlayer` true and `playerCanSeeCreature` false.

### Step 3: Memory

- `creature/Memory` holds at most 32 observations. Each has a kind (SIGHT, SOUND, TRACK, CHANGED), a position, a game time and a confidence.
- Merge rule: same kind within 8 m replaces the entry if confidence is at least as high; otherwise only the time is refreshed.
- Decay is linear per kind: `confidence = c0 * max(0, 1 - age / lifetime)`. Lifetimes: SIGHT 4, SOUND 3, TRACK 90, CHANGED 240 (game minutes). Entries at zero are removed.
- Last known player position is kept per `PlayerId`.
- Two timestamps are kept per player: when the creature last saw the player, and when the player last saw the creature (`recentlyObserved`). The second one is what Phase C uses to decide to hide.
- Stale and wrong information is kept until it decays, not deleted when it turns out wrong. That is what lets the creature investigate the wrong place (Phase C).
- **Tests:** exact linear decay values; merge and no-merge cases; eviction order at 33 entries (lowest confidence, then oldest); a sighting older than its lifetime has zero confidence; a wrong sound location is kept until it decays.

### Step 4: Path search for the creature

- Make `PathFinder` public in `world/query` (or copy it), on a 4 m grid, budget 4000 nodes.
- Input: start and goal in world metres. Output: a list of waypoints, or none.
- **Tests:** flat ground path is walkable; a path never crosses a footprint; a tree wall is walked around; an unreachable goal stops at the budget and returns none; the same input gives the same path.

### Step 5: Creature state and wander

- `creature/Creature`: `EntityId` (allocated from `WorldItems`-style counter, or a shared allocator), position, heading, speed, mode (`WANDER` only in Phase B), current path and waypoint index.
- `creature/CreatureSimulation` owned by `WorldSimulation`, called once per fixed step. Brain runs once per second of game time (every 20th step). Movement runs every step through `WorldCollision.move`.
- WANDER: pick a seeded waypoint 30 to 80 m away on walkable ground, path to it, pick the next one when reached or after 60 s.
- Path policy (from the brief's "avoid full-world pathfinding every frame"): a path is searched only when the target changes, never per frame. A search is at most two per brain tick, and a path is reused until its target changes or it is blocked.
- The creature never gets within 2 m of the cabin footprint while wandering (kept out by a simple distance check).
- **Tests:** creature spawns at least 150 m away for 25 seeds; it never stands inside a footprint or a tree (property over 500 steps); it reaches a waypoint within the timeout on flat ground; same seed, same route.

### Step 6: Information barrier test (required)

- Run the brain twice with the player in two different spots, both out of sight at all times. The creature's memory and modes must be identical.
- A second run with the player visible in both runs at the same place must match on the first observation.
- This test must pass before Phase C starts.

### Step 7: Debug overlay

- `dev.creature=true` in `config/game.properties`, read through `GameConfig`.
- Draws: the cone, the last known player point, the current mode and the number of observations. Lives in `world/render/CreatureDebugView`. Reads snapshots only.
- No test. Checked by a screenshot.

## 5. Scope limits (not in Phase B)

- No attacks, no damage, no cabin entry, no PROBE or STRIKE.
- No circling, stalking, investigating or hiding (Phase C).
- No fire protection, chimney, or cue log (Phase D).
- No disguises, no speech (Phase E).
- No creature model. Debug view only.

**Seams kept open for later (no code in Phase B):**
- **Form as data.** `CreatureForm` is an enum of numbers (speed, senses, abilities, model key), so the wolf, bear and human are data, not three AI classes. Phase B only defines WOLF.
- **Sense kinds reserved.** `SenseEvent` kinds for food left outside (bear), knocks and speech (human), and gunshots are named now but not produced. Adding one is a new enum value and one producer.
- **Night rule for the human form.** Perception reads `DayPhase`, so a day-only or night-only rule can be checked from the clock. No separate code needed now.
- **Hidden ending.** It is a narrative event, not AI. It needs a place to hook in later (`narrative` package). Nothing in Phase B should block it: the creature never has a special path to the cabin door, so a scripted visit can use the same movement.

## 6. Risks

| Risk | Mitigation |
|---|---|
| Line-of-sight cost (terrain sampling every 2 m, per check) | Two checks per perception step at most; measure; cache per 0.2 s if slow |
| Path search too slow on a 1536 m world | Budget of 4000 nodes; plan around it; use 4 m grid |
| Creature spawns inside a building or a tree | Spawn search uses `WorldCollision.canStandAt` and retries with the seed |
| Player-facing behaviour leaks in before Phase C | Brain has only WANDER; enforced by a test that no other mode exists |
| Map changes when felling trees arrive later | Not in Phase B; sense events are the seam for it |

## 7. Acceptance for Phase B

1. All Phase A tests still pass.
2. The information barrier test passes over 25 seeds.
3. Perception, memory and path tests pass, including the boundary cases above.
4. Over 10 000 creature steps, no allocation in steady state (measured with `ThreadMXBean` or by reading the code path) and the AI tick averages under 0.5 ms.
5. The debug overlay shows the creature moving and the cone changing with the camera, checked by screenshot.
6. Phase C has not been started.

## 8. Decisions I need from you

Please confirm or change the five items in section 3. The defaults above are what I will build if you say "go" without changes.

## 9. As built: deviations from the plan

- **Trees block the creature's sight.** The plan said line of sight uses `WorldCollision.lineClear`. That check ignores trees, rocks and placed objects (placement needs it that way). Sight therefore uses a new `WorldCollision.sightClear`, which uses the full obstruction test. Hearing still counts building walls only, as the plan said.
- **Player's sight range:** 100 m by day, 60 m at dawn and dusk, 30 m at night. Confirmed by the owner.
- **Creature identity is a fixed reserved id** (`CreatureSimulation.ID`, 1 000 000 000). The plan said to share the item counter. Sharing it changed the ids of placed items and broke existing tests, so items keep their own counter.
- **Sense kinds for knocks, speech, gunshots and food left outside are not in the enum yet.** The plan said to name them. They are left out so no code is written for things with no producer. They are added in Phase E.
- **`CreatureParameters` is a class of constants, not a `*Parameters.DEFAULT` record.** The set is large and is read in one place.
- **Path search: `PathFinder` moved to `world/query` and made public.** It takes an optional expansion budget; the creature uses 4 000, and world generation keeps the unlimited search, so generated worlds are unchanged (the golden-value tests still pass). The sort-key helper moved to a new `world/query/SearchKey`, so `query` does not depend on `gen`.
- **`StepCounter` moved from `audio` to `player`.** The simulation counts footsteps, and it must not depend on the audio package.
- **Debug overlay:** a red marker and one HUD line (mode, position, memories, route left). The vision cone is not drawn yet.
- **Creature tests:** the spawn check runs on 10 seeds, not 25, to keep the suite fast.
- **Information barrier:** enforced three ways. Only `Perception` may read the player's state (a test scans the creature package). Two worlds with the player far away in different places give identical creature states step for step. A positive control shows that a player in view is remembered. The plan's second variant (player in view in both runs) is covered by the positive control rather than a separate test.
- **Wandering does not use memory yet.** The brain picks spots at random. Memory affects decisions from Phase C.
- **Known limitation: the path grid is 4 m.** A route can pass through the corner of a thin building between two grid points. Collision then stops the creature, the route is marked blocked, and the brain searches again. Phase C should check route segments, not only points.

## 10. Measured

- Creature cost: average 0.008 ms per fixed step over 4 000 steps (budget 0.5 ms). The first step takes about 26 ms, because it builds the search grid. That happens at world start, not in the middle of play.
- Full suite on the branch: 34 test classes, 263 tests, all passing, `-Werror` build passing.
- In-game: seed 404 at 12:00 with `dev.creature=true`. The HUD line shows the creature wandering, with 0 memories, and no errors in the log.
