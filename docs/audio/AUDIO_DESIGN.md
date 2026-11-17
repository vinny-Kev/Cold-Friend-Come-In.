# Audio Design

**Status: partly implemented.** Snow footsteps play in the world: `audio/Footsteps.java` plays the WAV files, and `audio/StepCounter.java` decides when a step is due. Footsteps are the only sound so far. The master and sound-effects volumes now apply to footsteps. Music, wind, fire, creatures and positional audio are still design only. This document describes the intended design. It is not a finished specification, and the owner should review it before further implementation.

## Principles

- The environment carries most of the atmosphere. Music is rare; silence is a tool.
- Sound comes from the world, not from a soundtrack. Wind, snow, fire and wood do most of the work.
- Every cue is placed in the world, so the player can work out where it came from.

## Environmental layers

Audio is built from layers that play at once and blend by context:

1. **Base wind:** a continuous, loopable wind bed. Its strength follows a slow curve, so gusts come and go over minutes, not seconds.
2. **Forest texture:** creaks, cracks and distant tree movement. Sparse, random, positioned in the trees around the player.
3. **Local detail:** snow falling from branches, water under ice, and rattling objects near the player.
4. **Interior bed:** inside the cabin, the outside wind is muffled and a quieter room tone takes its place.

## Surface-dependent footsteps

Footstep sounds depend on the surface under the player's feet, which the world already knows (`TerrainRules`, `TerrainColors`):

| Surface | Source in the world | Sound |
|---|---|---|
| Deep snow | Default ground, high snow cover | Soft, muffled, slow to sink |
| Compacted snow | Paths and the village square | Crisp crunch |
| Ice | Frozen water edges (planned) | Thin, sharp, slippery |
| Frozen dirt or gravel | Riverbanks, bare ground | Hard, gritty |
| Wood | Cabin floor | Hollow and creaky |
| Wading | Shallow water (`PlayerMovement` wade depth) | Splash, slower walk |

Footsteps should loop as seamless short clips with pitch variation, so they don't sound repetitive.

## Spatial audio and distance

- Use positional audio for every sound source that has a location. Distance attenuation fades sounds smoothly; no sound cuts off at a fixed radius.
- Creature sounds and unexplained sounds carry further than ordinary ones, so the player hears them before they can see anything.
- The engine's `AudioNode` supports positional playback. Check its settings against the project's units (metres) when implementing.

## Cabin and exterior acoustics

- Outside: wind, forest and snow sounds at full level.
- Inside: outside sounds reduced and low-pass filtered. The fire, the wood and the room tone are louder.
- The transition at the door should be audible, so the player feels the shelter. A door that opens and closes is part of this.

## Day and night

- Night is the default state for horror tension. Daylight should be quieter and calmer, not safe.
- The wind bed and forest texture should change with time of day, not just with weather.
- These rules are not decided yet. The owner should confirm the day/night plan before implementation (see `docs/development/DEVELOPMENT_ROADMAP.md`).

## Dynamic wind

- Wind strength follows a slow random curve, with occasional sharp gusts.
- Gusts also move the forest texture: more creaks and snow fall during a gust.
- Wind should be used to build tension, then release it, not to fill every moment.

## Fire ambience

- The fire crackles at a rate that depends on how much fuel is burning.
- Adding wood raises the level and the crackle rate. The fire gradually burns down.
- **When the fire goes out:** the crackle stops, the room tone drops, and the wind is heard more clearly through the walls. This is the warning cue that the cabin is no longer warm. Its timing is a design decision for the owner.

## Creature cues

- Creature sounds are placed in the world and never confirmed on screen first.
- Cues are sparse: a branch snapping, a footstep that stops, knocking that does not repeat in a pattern.
- The creature's behaviour is not designed yet, so these cues are placeholders until the creature milestone.

## Strategic silence

- Silence after a loud event is part of the scare. Keep the ambient layers quiet for a few seconds after a major cue.
- A quiet cabin with a dying fire is more tense than a loud soundtrack.
- Avoid constant drones. If there is music, it fades in only at transitions.

## Music

- Music is rare and short. It plays at narrative moments or when the environment changes sharply.
- Transitions cross-fade over several seconds. No music starts with a hard cut.
- Music is never the only source of tension.

## Implementation notes

- Volumes come from `GameSettings` (master, music, sound effects). Apply them to the mixer, not to individual sounds, so one change affects everything.
- Keep audio assets in `assets/audio/` (see `docs/development/ASSET_PIPELINE.md`). Gameplay code refers to sound kinds, not file names, the same way it refers to prop kinds.
- Use OGG for looping and music, WAV for short effects, as the catalog lists them.

## Open questions for the owner

1. How long should the wind bed stay quiet between gusts?
2. Should the fire go out, or should the player always have to tend it?
3. How much music, if any, is wanted in the first playable prototype?
