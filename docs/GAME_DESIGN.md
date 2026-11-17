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
- Warm cabin interiors against cold exteriors. **Planned.**
- VHS-inspired post-processing, used selectively. **Planned.**
- Externally sourced glTF/GLB models will be supplied by the owner. **Planned.**

## Audio direction

Snow footsteps varying by surface and footwear, wind through trees and structures, fireplace crackle, creaking wood, wildlife ambience, distant disturbances, positional audio for threats and interactions. **Planned.**

## Current implementation (milestone 1: project foundation)

**Implemented:**

- Main menu titled "COLD FRIEND, COME IN." with New Game, Continue, Settings, Quit.
- New Game is a placeholder: it shows "Gameplay is not implemented yet."
- Continue is shown dimmed and cannot be selected, because saving does not exist yet.
- Settings screen with master, music and sound-effect volume, resolution, fullscreen and vsync.
- Procedural menu backdrop: snow ground, crooked conifers, a lit cabin, falling snow, fog, slow camera sway.

**Settings behaviour (implemented):**

- Volumes are saved to `config/settings.properties` but **not yet applied**, because there is no audio system in this milestone. The settings screen says so.
- Resolution, fullscreen and vsync are applied. Changing them restarts the display when Apply is chosen.
- Back discards unapplied changes.

## Planned, not decided

- Survival resources (warmth, food, health): **Planned, values and rules not decided.**
- Seven-day structure, shaman arrival and the creature's behaviour: **Planned, details not decided.**
- Save system (enables Continue): **Planned, format not decided.**
