# AI and Survival Testing Strategy

Status: **Proposed.** No new tests are written by this document. The existing test suite (20 classes under `src/test/java`) is listed in the roadmap's baseline; it was **not run** for this plan.

The principle: **AI behaviour is validated by deterministic simulation tests and recorded traces, not by watching screenshots.** Screenshots and manual play confirm feel and presentation. They do not prove that the creature respects the information barrier, that it never walks through a wall, or that every death was fair.

## 1. Test layers

| Layer | What it proves | Speed | Needs jME? | Examples |
|---|---|---|---|---|
| **Unit** | One pure function or class is correct | ms | No | `MemoryDecayTest`, `FieldOfViewTest`, `HearthTest` |
| **Fixture** | Perception and navigation on hand-built worlds | ms | No | Flat plane with one tree, a ridge, a house |
| **Scenario** | A scripted player and creature run for simulated minutes, asserting mode and survival events | 10–500 ms each | No | "player stands in cone, creature must not approach" |
| **Property** | An invariant holds across many seeds | seconds | No | "no path crosses a solid", over 25 seeds |
| **Golden trace** | A seeded run produces the same event sequence (regression) | seconds | No | Mode sequence hash for seed 42, 30 game minutes |
| **Performance** | Per-tick cost and allocation stay within budget | seconds | No | AI tick under 0.5 ms, zero allocation in steady state |
| **Architecture** | Packages obey the dependency rules | ms | No | No `com.jme3` import in `creature`, `survival`, `player`, `world` (except `world.render`) |
| **Manual** | Feel, readability, audio mix, camera | minutes | Yes | Playtest notes in `docs/development/PLAYTEST_LOG.md` |

The first five layers need no engine. The simulation is written so that they run under plain JUnit, as the existing `PlayerMovementTest` and `WorldGeneratorTest` already do.

## 2. Deterministic simulation harness

A `SimHarness` test helper (test code only, no production class) that:

- Builds a `WorldData` from a seed using the existing generator.
- Builds a `WorldSimulation` with a fixed `ItemCatalog` (test items, `TestItems` already exists).
- Advances in **fixed steps only** (20 Hz). The harness never passes a real frame time, so runs are repeatable.
- Accepts a scripted player (a list of `PlayerInput` per step, or a function of the step index and the player's state), a fixed clock start, and a fixed random seed for the creature.
- Records a **trace**: per step or per AI tick, the creature's mode, position, the player's position and the sense events. Traces are compared or summarised.

Helpers:

| Helper | Purpose |
|---|---|
| `ScriptedPlayer` | Walks a route, turns to face a point, stands still, opens the door |
| `TraceRecorder` | Records state per AI tick into a preallocated array, writes a short text summary |
| `TraceHash` | Stable hash of a trace (for golden tests) |
| `FixedWorlds` | Named seeds used across tests, for example 42, 1337, 20261117 (as used in `GoldenValues`) |
| `FlatGround` | Hand-built `HeightField` with optional trees and walls, for fixture tests |

## 3. Perception tests

### 3.1 Field of view

Fixture: flat ground, creature at known positions, player at origin with known yaw and pitch.

| Test | Expectation |
|---|---|
| Creature exactly at the player's yaw, 10 m | `watchedByPlayer` true, creature visible |
| Creature at the cone edge (35°) | Included; at 36° excluded (boundary test) |
| Creature at 60° horizontal, 5 m | Not watched by player; creature sees player only inside 8 m close range |
| Creature directly above the cone (pitch edge, 25° vertical) | Boundary test as horizontal |
| Player yaw wraps at ±π | Works for yaw 3.1 and −3.1 pointing at the same creature |
| Range by phase | Visible at 59 m by day, not at 61 m; 34 m at dusk; 14 m at night |
| Player turned around | Creature not watched even at 5 m |

Tolerance: 1e-4 radians. Use exact boundary values with a small margin, not random points.

### 3.2 Line of sight

| Test | Expectation |
|---|---|
| Open ground | Clear |
| One tree between (circle radius 0.5 at midpoint) | Blocked; tree radius 0.5 m with creature radius 0.4 m: a gap of 0.1 m is blocked |
| Building footprint between | Blocked |
| Tree beside the line, 1 m off | Clear |
| Ridge between (heightfield bump 3 m, 10 m from creature) | Blocked by terrain occlusion; same ridge at 0.1 m height: clear |
| Hollow between (creature in a hollow, player on a ridge) | Clear (boundary: the rule is about the ray above the ground, not about being below it) |
| Symmetry | `lineClear(a, b) == lineClear(b, a)` on 200 random pairs in a seeded world |

### 3.3 Hearing

| Test | Expectation |
|---|---|
| Sound inside loudness radius | Observation of kind SOUND with confidence as specified |
| Sound outside radius | No observation |
| Sound through a wall | Loudness × 0.35: a footstep (6 m) through a wall is heard only within 2.1 m |
| Confidence cap | Never above 0.9 |
| Position jitter | Within 1.5 m, seeded, identical across runs |
| Creature's own KNOCK | Not heard by itself |
| Event overflow | Eight events processed max; the rest are counted as dropped |

### 3.4 Environmental observation

- A TRACK is created for each breadcrumb every 6 m of player walking, up to 40 breadcrumbs; the 41st replaces the oldest.
- A CHANGED observation appears after an item drop and after a felled tree.
- A LIGHT observation at night exists while the hearth is BURNING, with confidence 0.8; it is 0.2 by day.

## 4. Memory tests

| Test | Expectation |
|---|---|
| Decay is linear | At half the lifetime, confidence is half the original (exact) |
| Zero at lifetime | Removed at the next brain tick |
| Merge within 8 m | Same kind replaces when confidence is higher or equal; time refreshes |
| Merge outside 8 m | Two entries kept |
| Capacity 32 | Adding 33rd evicts the lowest confidence, tie evicts oldest; deterministic order |
| Suspicion | +0.2 per KNOCK, clamped at 1.0; decays 0.02 per game minute without cabin evidence |
| Lost confidence | A SIGHT at 5 game minutes old cannot drive INVESTIGATE (confidence 0) |
| Memory survives form change (Phase E) | `Memory` identical before and after a form switch |

Memory tests use a fake clock value passed in explicitly, never `System.currentTimeMillis()`.

## 5. Behaviour and transition tests

### 5.1 Brain unit tests

With fixed considerations (a test `Considerations` stub returning chosen values), assert the chosen mode:

| Case | Expected mode |
|---|---|
| Player watching, distance 10 m | WITHDRAW |
| No evidence, day | ROAM |
| Fresh SOUND from 15 m, night | INVESTIGATE |
| Player inside cabin, fire low, night, no restraint | PROBE |
| Same, but fire protection 1 | HOLD |
| Same, restraint 0 | HOLD (restraint roll is seeded; test with a fixed roll that selects HOLD) |
| Current mode OBSERVE, STALK score 1.1× OBSERVE | Stays OBSERVE (below 15% hysteresis) |
| Current mode OBSERVE, STALK score 1.2× OBSERVE, commit passed | STALK |
| Current mode STALK, commit not passed, WITHDRAW watched 1 s | WITHDRAW (override) |

### 5.2 Scenario tests (per behaviour)

Each scenario is a fixed seed, a scripted player, and assertions on the trace:

| Scenario | Script | Assertions |
|---|---|---|
| Observe from cover | Player stands in the open 40 m from the cabin, facing away | Creature reaches OBSERVE within 90 game seconds; never inside the player's cone for more than 2% of ticks |
| Stalk avoids cone | Player walks slowly, turns to look every 20 s | Creature never enters the cone at ≤ 25 m; withdraws within 2 s of being watched |
| Investigate a sound | Player makes a footstep sprint at 20 m | Creature moves to within 4 m of the sound position within 30 game seconds, and searches |
| Lost trail | Sound, then player vanishes | After 3 game minutes the creature stops INVESTIGATE and returns to HOLD or ROAM |
| Circle the cabin | Player inside, fire BURNING, door closed | Creature stays 12–18 m from the cabin centre; never enters the cabin footprint |
| Probe with fire low | Fire level 0.1, night, player inside | PROBE happens at least once within 60 game minutes |
| Restraint | Player stands 3 m from the door, 200 seeds | STRIKE happens in fewer than 60% of seeds over the night (bound, not a fixed value) |
| Withdraw and return | Player faces it at 15 m, then looks away | WITHDRAW, then cooldown, then no STRIKE for at least the configured cooldown |

### 5.3 Transition coverage

A test enumerates every edge in the mode diagram in `CREATURE_AI_DESIGN.md` and checks that at least one scenario or unit case covers it. The list lives in the test file as data, so an edge added to the design without a test fails the build. This is the one place where a test mirrors a document, and it is deliberate.

## 6. Navigation tests

| Test | Expectation |
|---|---|
| A* path on flat ground | Found; every cell walkable; start and goal cells correct |
| A* around a tree wall | Path exists and avoids the wall; cost greater than straight-line distance |
| No path through a solid | Property over 25 seeds: each path cell is walkable by `TerrainRules` and not inside a footprint |
| Node budget | A search with an unreachable goal stops at 4000 nodes and returns partial or none; never loops |
| Deterministic | Same inputs give identical paths (compare lists) |
| Replan on block | Creature blocked by a placed item re-routes within 2 s |
| Movement collision | Creature steered into a tree slides along it; never inside the tree's circle (property over steps) |
| Reachability after felling (Phase A) | Felling a tree that opened a route keeps `WorldValidator` passing for 10 seeds |

## 7. Survival tests

| Test | Expectation |
|---|---|
| Clock phases | Boundaries at 05:30, 07:00, 17:00, 19:30 exact; wraps at midnight; `dayNumber` increments at 00:00 |
| Clock with fixed step | After N steps, time equals N × step × rate (exact in double arithmetic within 1e-9) |
| Fixed step independence | Same inputs at simulated 30, 60 and 144 fps yield identical `WorldSimulation` state after 10 s |
| Warmth target | Each situation in the table gives the stated target |
| Warmth rate | Rises 4 per game minute, falls 2 per game minute (exact over 10 game minutes) |
| Health falls only when critical | Health unchanged while all values ≥ 20 |
| Health death path | Health reaches 0 and a death event is emitted once |
| Clamp | Warmth, nourishment, hydration, health stay in [0, 100] under random inputs (property, 1000 steps × 25 seeds) |
| Fuel burn | Level 0.3 for 10 game hours burns 3.0 fuel-hours (±0.01) |
| Fuel never negative | Under random feeding and time, fuel ≥ 0 |
| Feed capped | Level never above 0.9 |
| Embers and out | Level below 0.15 becomes EMBERS; out after 30 game minutes without fuel |
| Chimney | Blocked for 3 game minutes lowers level; clearing removes the block |
| Firewood yields | Tree yields 2 or 3, same yield for same seed and tree id |
| Felled tree | Removed from collision; not removed from `WorldData` (immutability) |
| Eat and drink | Restore amounts as specified; cannot exceed 100 |
| Fill container | Works only at `WATER_ACCESS` within reach; refused elsewhere; item converted |
| Consistency with dev.time | `dev.time=night` sets the clock to 22:00 on day 1 |

Survival numbers are tested by **expected totals over a simulated day**, not by per-frame values. Example: "a player who does nothing from 07:00 to 07:00 ends with nourishment within 70 ± 1 of 30 below the start."

## 8. Fairness tests

These are the most important tests for the horror, because they enforce "danger must be understandable in retrospect".

| Test | Expectation |
|---|---|
| Cue before damage | Over 200 seeded nights with a passive player, every creature damage event has at least two cues in the prior 30 game minutes |
| Survival deaths have warnings | A passive player who dies of cold or hunger has a CRITICAL cue at least 20 game minutes before death |
| Fire protection holds | With the fire at level ≥ 0.3 all night, no STRIKE within 25 m in 200 seeds |
| No instant death | Health never falls by more than 40 in one game minute from any single cause |
| Cue log content | Cue kinds recorded match the events (for example a probe at the door produces a KNOCK cue) |

A fairness failure is a bug in the rules, not a tuning problem. If a test fails, either add the missing cue or change the rule that caused the damage.

## 9. Determinism and seeds

- All randomness comes from `RandomStreams` (existing) with named streams: `"creature.roam"`, `"creature.cover"`, `"creature.restraint"`, `"survival.yield"`. A new stream never changes the world generation streams.
- Tests use a small fixed set of seeds (for example 42, 1337, 20261117 and 7) and one "stress" set of 25 seeds for property tests. Seeds are named constants.
- Golden traces store **hashes and short summaries**, not full traces. When a golden value changes, the change must be justified in the decision log, just as `GenerationVersion` works today.
- Creature parameter changes are allowed without a version bump, but **change the golden hashes**, and the commit must say so.

## 10. Multiple world seeds

| Test | Seeds |
|---|---|
| World generation still valid | Existing `WorldGeneratorTest` and `GoldenValues` |
| Creature starts outside the village and at least 150 m from the cabin | 25 seeds |
| Creature never spawns inside a footprint or solid | 25 seeds |
| Creature can reach the cabin's perimeter ring | 25 seeds (ring cells walkable) |
| Scenario outcomes are not identical across seeds | At least 3 different mode sequences among 10 seeds (guards against a constant behaviour) |

## 11. Performance

Budgets, measured on the development machine and recorded in the decision log when first measured:

| Measure | Budget |
|---|---|
| Creature AI tick (perception + brain), one creature | Average under 0.5 ms, p99 under 2 ms |
| Movement and collision per fixed step | Under 0.1 ms |
| Terrain line-of-sight, one check | Under 0.05 ms (50 samples) |
| A* search, 4000 node budget | Under 2 ms |
| Steady-state allocation in the AI tick | Zero bytes per tick after warm-up |
| Simulation step at 20 Hz for one player and one creature | Under 1 ms total |

Test approach:

- Timing: run 10 000 AI ticks in a loop after 1000 warm-up ticks; compare to budget with a generous factor (×3) so the test is not flaky on slow machines. Log the actual figure.
- Allocation: `com.sun.management.ThreadMXBean.getThreadAllocatedBytes` before and after 1000 steady-state ticks; assert the difference is below a small constant (for example 4 KB, to allow for logging). If the JVM does not support it, skip with a message, not a failure.
- Repeated updates: 20 game hours of simulated time (1200 ticks per game hour at 20 Hz) finishes in under 60 s wall time with one creature. Catches leaks and drift.

Performance tests are tagged so they can be excluded from quick runs (for example a JUnit tag `performance`), but they run in CI before merging a phase.

## 12. Manual runtime verification

Manual checks cover what tests cannot judge. Each check names its method and writes a note in `docs/development/PLAYTEST_LOG.md`.

| Phase | Check | How |
|---|---|---|
| A | The lights follow the clock; the HUD values fall and recover | `dev.time=dusk`, play 3 game minutes; screenshot with `dev.screenshot` at two times |
| A | Feeding the stove changes the window glow | Feed, then screenshot from outside (`dev.spawn=village` or a scripted spawn) |
| B | Overlay shows the cone, rays and last known position | `dev.creature=true`; walk behind a tree and watch the sighting go stale |
| C | The creature stays out of view at night | Walk the perimeter at night for 10 minutes with the overlay; note how often it is visible |
| C | Stalk feels like stalking | Playtest with a second person who does not know the rules; ask what they felt |
| D | Cues are readable | Let the fire go out; list what you noticed before the creature reached the door |
| D | Fire protection is visible | Feed the stove, watch the creature keep 25 m |
| F | Full day loop works | Three in-game days by hand |
| F | Death is understandable | Two deaths; for each, write what the cue log showed |

Screenshots are for presentation. They are not used as evidence that AI behaviour is correct. Use the trace and the overlay for that.

## 13. Test code layout (proposed)

```
src/test/java/com/coldfriend/
  creature/        PerceptionVisionTest, PerceptionHearingTest, MemoryDecayTest,
                   CreatureBrainTest, BehaviourScenarioTest, RestraintTest,
                   InformationBarrierTest, CreatureImportRuleTest
  creature/support FlatGround, ScriptedPlayer, TraceRecorder, TraceHash
  survival/        WorldClockTest, HearthTest, SurvivalTest, CueLogFairnessTest
  world/query/     GridPathFinderTest, FelledTreeCollisionTest
  simulation/      FixedStepTest, FullDayScenarioTest, PerformanceTest (tag: performance)
```

## 14. The information barrier test (required)

This test protects the most important design rule. It must exist before any behaviour is written in Phase C.

1. Build a world and a creature.
2. Run the brain for 60 game seconds with the player at position P, out of sight.
3. Repeat with the player at position Q, also out of sight (same time, same events), where Q is far from P.
4. Assert that the creature's modes, targets and `Memory` are **identical** in both runs.

If the creature could read the player's position, the two runs would differ. The test runs for 25 seeds. It is also the answer to "does the creature have magical knowledge?", with evidence.

A second version adds a visible player: positions differ, but the player is in view in both runs at the same time and place. The creature's decisions must then be identical in the first ticks, because the observation is the same.

## 15. Definition of done per phase

A phase is not done until:

1. Every new class has a unit test, or is covered by a scenario test.
2. Every scenario and property test in the phase passes on the fixed seed set.
3. Golden hashes are updated with a note in the decision log, if they changed.
4. The performance budget is measured and recorded (for Phases B onward).
5. The architecture test passes (no engine import in the simulation packages).
6. The manual check for the phase has an entry in the playtest log.
