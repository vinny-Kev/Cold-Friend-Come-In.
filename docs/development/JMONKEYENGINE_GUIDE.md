# jMonkeyEngine Guide

This is a working guide for this project only. It uses the classes that exist in the repository. For the engine itself, see the jMonkeyEngine documentation at https://wiki.jmonkeyengine.org.

Engine version: **3.9.0-stable**, pinned in `gradle.properties` (`jmeVersion`). Modules in use: `jme3-core`, `jme3-desktop`, `jme3-lwjgl3`, `jme3-effects` (fog), `jme3-plugins` (glTF, GLB and FBX loaders). Do not change the version without owner approval.

## Scene graph: Nodes and Spatials

- A `Spatial` is anything in the scene. A `Node` holds other spatials. A `Geometry` is a spatial with a mesh and a material.
- `rootNode` (the 3D world) and `guiNode` (2D overlay) come from `SimpleApplication`.
- Attach with `node.attachChild(spatial)`, detach with `spatial.removeFromParent()`.
- Positions are local to the parent. The project uses **metres** for world units.

Example, from `WinterBackdrop` (adapted):

```java
Geometry ground = new Geometry("ground", new Box(GROUND_SIZE / 2f, GROUND_THICKNESS / 2f, GROUND_SIZE / 2f));
ground.setMaterial(litMaterial(GROUND_COLOR));
ground.setLocalTranslation(0f, -GROUND_THICKNESS / 2f, GROUND_CENTER_Z);
scene.attachChild(ground);
```

## Geometry and Mesh

- `Geometry` holds a `Mesh` and a `Material`.
- Procedural meshes come from `environment.LowPolyMeshes` (faceted triangles with flat normals) and jME shapes such as `Box`.
- Build a mesh once and reuse it. `TerrainMeshes` builds terrain chunks once and never changes them.

## Materials and textures

- Materials are created from a material definition file, for example `Common/MatDefs/Light/Lighting.j3md` (lit) or `Common/MatDefs/Misc/Unshaded.j3md` (no lighting).
- The project uses `Lighting.j3md` for most lit objects, and `Unshaded.j3md` for flat overlays such as the menu dimmer.
- `ModelCatalog` replaces the PBR material of a loaded model with a simple lit material that keeps its texture. Do the same for new models, so they match the scene's lights.
- Transparent overlays need a blend mode: `material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha)`, as used by `BackdropState`.

## Lighting

`WorldPlayState` sets up the world's lights:

```java
rootNode.addLight(new AmbientLight(AMBIENT));
rootNode.addLight(new DirectionalLight(MOON_DIRECTION, MOONLIGHT));
```

- Lights apply to the node they are added to. Add them to `rootNode` for the whole world.
- Lit materials need lights, or the geometry is black.
- Warm lights in the cabin come from `CabinView` (point lights). Keep their count low; each light costs shader work.

## Cameras

- `cam` is the camera. Set its frustum with `cam.setFrustumPerspective(fov, aspect, near, far)`, as `WinterBackdrop.attach` does.
- Move it with `cam.setLocation(...)` and aim it with `cam.lookAt(target, Vector3f.UNIT_Y)`.
- The menu camera sways in `WinterBackdrop.update`. The world camera follows `PlayerState` in `WorldPlayState`.
- `flyCam` (the free-fly camera) is **turned off** in `GameApp`, because the menus use the mouse. Do not re-enable it without checking the menu cursor.

## Input mappings

Input goes through `InputManager`. Two wrappers in this project:

- `InputController` (menus): registers `GameAction` mappings once, and passes presses to whichever screen owns input. A screen calls `bind(this, handler)` in `onEnable` and `release(this)` in `onDisable`.
- `GameplayInput` (world): owns the movement, sprint, interact and pause mappings. It removes all of them in `detach()`.

Add a new mapping in the wrapper that owns that context, not in a screen:

```java
inputManager.addMapping(INTERACT, new KeyTrigger(KeyInput.KEY_E));
inputManager.addListener(listener, INTERACT);
```

Remove it in `detach()`, or it will fire after the screen has closed.

## AppStates

`BaseAppState` subclasses are the screens. The engine calls:

| Method | When | Use it for |
|---|---|---|
| `initialize(app)` | Once, when first attached | Loading fonts and creating scene objects |
| `onEnable()` / `onDisable()` | Each time the state is enabled or disabled | Attaching and detaching scene nodes, binding input |
| `update(tpf)` | Every frame while enabled | Per-frame logic. Check `isEnabled()` first. |
| `cleanup(app)` | Once, when the app stops | Releasing references |

Rules this project follows:

- Attach to the scene in `onEnable` and detach in `onDisable`. Never leave a node attached to a disabled screen.
- Keep long work out of `update`. `WorldGenerator.generate` runs on the main thread, so `GameApp.startNewGame` uses `enqueue` to let one frame draw first.
- Menu screens extend `MenuScreenState`, which already handles hover, click and resize.

## AssetManager

- `assetManager` loads files. `GameApp` registers the `assets` folder with `FileLocator`, so `loadModel("models/characters/Knight.glb")` reads `assets\models\characters\Knight.glb`.
- Load fonts with `app.getAssetManager().loadFont("Interface/Fonts/Default.fnt")`. That path is in the engine's own resources.
- Load textures with `loadTexture`, and models with `loadModel`. A failed load throws `AssetNotFoundException`, a `RuntimeException`. `ModelCatalog` catches it per model and logs it.
- Use the catalogue (`assets\models\catalog.properties`) for scenery. Do not hard-code file paths in gameplay or rendering code. See `ASSET_PIPELINE.md`.

## Audio

Audio is not implemented. The engine's `AudioNode` and `AudioRenderer` are present in `jme3-core`, but no audio code exists yet. See `docs/audio/AUDIO_DESIGN.md`.

## Physics

No physics engine is configured. The project uses its own movement and collision rules (`PlayerMovement`, `WorldCollision`), which are tested without the engine. Do not add `jme3-bullet` without owner approval.

## Frame updates and lifecycle

- `simpleInitApp` runs once, after the engine is ready. Create the scene here or in an `AppState.initialize`.
- `simpleUpdate(tpf)` runs every frame. `tpf` is seconds since the last frame.
- Put gameplay logic in `WorldSimulation`, which is called from `WorldPlayState.update`. Keep rendering in `WorldScene` and `CabinView`.
- `GameApp.destroy` runs when the app stops. Detach and release anything that holds native resources.

## Resource cleanup

- Detach nodes that are no longer shown (`removeFromParent`). A detached node still holds its memory until it is garbage-collected.
- `WorldPlayState` detaches the world and removes its fog filter when the player leaves. Check this when you add new scene objects.
- Removing a state with `stateManager.detach(state)` calls its `cleanup`. Null out references there.
- Input mappings must be deleted, not just ignored (see Input above).

## Practical example: adding a prop to the scene

1. Add the model file under `assets\models\environment\...` (see `ASSET_PIPELINE.md`).
2. Add it to the right line in `assets\models\catalog.properties`, for example under `ROCK`.
3. Run the game or the showcase to check its size and look.

No code change is needed when the kind already exists in `PropKind`.

## Common mistakes

- **Black objects:** a lit material has no light in its node, or the model has no normals.
- **Pink or missing textures:** the texture path inside the glTF is wrong, or the texture file is not in the same folder as the model.
- **Objects not appearing:** they are attached to a disabled state, or they are outside the camera's far distance (`FAR` in `WorldPlayState`).
- **Input still firing after a screen closes:** a mapping was not removed in `detach` or `release`.

## Transparent glass

Window glass in this project is an alpha-blended, lit material (`RenderParts.glass`). What matters, and why:

- **Blend mode `Alpha`** makes the glass mix with what is behind it.
- **Depth write off** (`setDepthWrite(false)`) is the important one. Glass that writes depth hides everything drawn after it behind that depth, which is how "see-through" windows turn opaque. With depth writing off, glass is still depth *tested* (walls in front of it hide it) but never hides anything behind it.
- **Transparent bucket** draws glass after all opaque geometry, sorted back to front, so the world behind it is already in the picture.
- **Face culling stays on, with two sheets facing opposite ways.** One double-sided sheet is lit wrongly from the back (its normal points away), so each side gets its own correctly facing sheet and cannot vanish from either direction.
- **Frames and walls stay in the opaque bucket.** A wall with a window is built from separate boxes around the opening, never one box with a see-through patch, so only the glass is transparent.
- If the scene uses `FilterPostProcessor` (fog), transparent geometry that does not write depth is fogged by the depth of whatever is behind it, which looks right for glass.
- Other transparent things (water, the placement preview ring) follow the same rule: depth write off, transparent bucket.

To check glass, use `dev.spawn=windowoutside|windowinside|villagehome` with `dev.time=day|dusk|night` and `dev.screenshot`.
