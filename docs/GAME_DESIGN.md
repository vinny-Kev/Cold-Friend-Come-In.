# Game Design

Status legend: **Implemented** (in code and verified), **Planned** (owner's direction, not built), **Proposed** (suggested by the engineering side, not approved).

## Concept

First-person, atmospheric winter survival horror. The player is cursed and exiled by their village into an isolated cabin in the woods. They must survive seven days until a shaman arrives to purify their soul. The creature haunting the forest kills the shaman before that, leaving the player stranded.

Source: project owner's brief. **Planned.**

## Pillars

- Horror from anticipation, uncertainty, environmental storytelling and an intelligent predator. No reliance on conventional jumpscares. **Planned.**
- Winter wilderness survival with resource management. **Planned.**
- Exploration and investigation. **Planned.**
- Intelligent shapeshifting creature behaviour. **Planned.**
- Possible cooperative multiplayer in a later phase. **Planned, undecided.**

## Visual direction

- Low-poly environments, pixelated low-resolution textures, crooked winter trees, dense snowy forests, heavy fog. **Planned.**
- Setting: a remote winter settlement. Asian or oriental influence is optional, not required. The tree mix is kept small, not a rainforest-style variety. **Decided by the owner; assets not yet sourced.**
- Cabin: a hut with a thatched roof, not a log cabin. **Decided by the owner.**
- Assets: free packs only. No paid packs. **Decided by the owner.**
- Characters: grounded and less cartoonish, suited to a horror game. The current stylised character pack (KayKit Adventurers) is a placeholder only. **Decided by the owner.**
- Houses: the Retro house pack is accepted as is. **Decided by the owner.**
- Warm cabin interiors against cold exteriors. **Planned.**
- VHS-inspired post-processing, used selectively. **Planned.**
- Externally sourced glTF/GLB models will be supplied by the owner. **Planned.**

## Audio direction

Snow footsteps varying by surface and footwear, wind through trees and structures, fireplace crackle, creaking wood, wildlife ambience, distant disturbances, positional audio for threats and interactions. **Planned.**

## Current implementation (milestone 2: procedural world and gameplay foundation)

**Implemented:**

- Main menu: New Game, Continue (dimmed until saving exists), Settings, Quit.
- Settings: volumes (stored, not applied), resolution, fullscreen, vsync, fog, snow, camera sway.
- New Game generates a seeded world (1536 m square) and starts a first-person game: mouse look, walking, running, wading shallow water, collision with terrain, trees, rocks and buildings.
- Terrain with hills, valleys, mountains and hollows; rivers that run downhill and widen downstream; five regions (conifer forest, open woodland, mountain forest, river valley, clearing); about 40,000 pieces of scenery per world (conifers, bare and dead trees, rocks, bushes, fallen logs, pebbles) placed by region, slope and distance to water.
- A cabin the player starts inside: door (E), bed, stove, table, glowing windows, warm interior light and a lantern. Its distance to water and forest density vary with the seed.
- A village with a general store, several homes, a square with a well and lamp post, paths to every door, and a trail to the cabin. Buildings are empty shells.
- Places to collect water, fords where paths cross rivers, and a check that the village, every door and every water point can be walked to from the cabin.
- Pause menu (Escape): Resume, Settings, Quit to Menu.
- Seeds: random by default or set with `world.seed`; seed, generation version and parameter fingerprint are logged.

**Milestone 04, Phase A (implemented, values proposed):** a game clock (one day is 20 real minutes; new games start at 07:00 on day 1) with lighting that follows it; warmth, food, water and health with warnings before anything harms health; a stove that burns firewood, dies to embers and goes out, and must be relit with matches and a branch; eating rations, drinking from and refilling a water container at the river. See `docs/development/SIMULATION_AND_SURVIVAL.md`.

**Not implemented:** NPCs, dialogue, trading, the shapeshifter, gathering firewood, hunting, the chimney, restart after death, most audio, saving, multiplayer. The world data (`world.model`) already has stable ids and named locations they will use.

## Planned, not decided

- Survival resources (warmth, food, health): **Planned, values and rules not decided.**
- Seven-day structure, shaman arrival and the creature's behaviour: **Planned, details not decided.**
- Save system (enables Continue): **Planned, format not decided.**
