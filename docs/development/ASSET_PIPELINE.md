# Asset Pipeline

This guide covers how models, textures and other files get into the game, and how the code loads them. It reflects the current project.

## Where assets live

```
assets/
  models/
    catalog.properties        which model files draw each kind of scenery
    characters/               player and creature models, and rig files
    environment/
      trees/                  tree models, by pack
      rocks/                  rock models, by pack
      plants/                 bushes and ferns, by pack
    structures/               buildings
  audio/                      planned (empty)
  materials/                  planned (empty)
  environments/               planned (empty)
src/main/resources/           engine-facing resources (logging config, shaders)
ASSET_MANIFEST.md             every third-party asset, with source and licence
```

Each pack has its own folder, with its licence file beside it (for example `assets\models\environment\LICENSE_kaykit_forest.txt`).

## Preferred runtime format

- **GLB** (one binary file per model) is preferred. It is a single file and its textures are often embedded.
- **glTF** (`.gltf` with a `.bin` and separate textures) also loads, because `jme3-plugins` provides the loader. Keep the `.bin` and texture files in the same folder as the `.gltf`, since the file refers to them by relative path.
- **FBX** loads with `jme3-plugins`, but FBX is less predictable than glTF. Prefer glTF or GLB.
- **OBJ** with an `.mtl` is also loadable by the engine, but it has no animation and its materials are basic.

Avoid DAE and Blend at runtime. Convert them first (see below).

## Importing a GLB or glTF

1. Check the licence (see below) and record the asset in `ASSET_MANIFEST.md`.
2. Copy the file into the right folder under `assets\models\`. Keep the original file name where practical.
3. Add it to `catalog.properties` under the right kind (see below), or load it from code with `assetManager.loadModel("models/.../name.glb")`.
4. Run the asset showcase (`gradlew.bat showcase`) and check its size and look.

## Converting other formats

Blender is not installed on the development machine. To convert FBX, OBJ, DAE or Blend files:

1. Install Blender (free). Check the licence of the asset before you convert it, and keep the licence file with the converted file.
2. **File → Import** the source file (FBX, OBJ, DAE or the `.blend`).
3. Check scale: Blender's units are metres. Apply the scale (**Ctrl+A → Scale**) so the object's scale is 1.
4. Check orientation: the game's up axis is +Y. Blender's is +Z, which the glTF exporter converts. Confirm the model stands upright in the preview.
5. **File → Export → glTF 2.0**. Choose **glTF Binary (.glb)** for a single file, or **glTF Separate** if you want to keep the textures apart.
6. Open the result in the showcase to check that textures and materials came through.

Not every conversion works perfectly. Textures sometimes come out missing or with the wrong colour. Flag these in the manifest rather than hiding them.

## Textures and materials

- Models with an embedded texture show their texture in the game. Models with only material colours show flat colour.
- `ModelCatalog` replaces each model's PBR material with a simple lit material that keeps its texture. Check the result in the showcase, because a model can change colour when this happens.
- Use `KIND.tint` in `catalog.properties` to shift a kind's colour (for example, to make autumn colours wintry). The tint is applied to the whole kind.
- Texture size: 1024 pixels is the most used in the packs. Keep textures no larger than needed; large textures use memory quickly.

## Scale and orientation

- The world is in metres. A person is about 1.8 m tall. The characters in this project are about 2.2 to 2.7 units tall, so they are slightly oversized for a person.
- Each kind has a scale in `catalog.properties` (the number after `:`). Use it to bring different packs to one size. Measure before you change it.
- Check with the showcase rather than guessing. The showcase places models at real size next to each other.

## Licensing and attribution

- Use **CC0** where possible. It needs no attribution.
- **CC-BY** needs attribution. Keep it in a credits file and note it in the manifest.
- Reject NC, personal-use-only, and any licence you cannot confirm.
- Check the **specific asset's** licence on its own page. A website is not one licence.
- Record in `ASSET_MANIFEST.md`: asset name, source URL, licence, original format, download date, original file name, and size.

## Naming conventions

- Lowercase words with underscores: `pine_tall_01.glb`. Keep the original name when importing a pack, so the source is traceable.
- Put the pack name in the folder, not in every file name, when a pack has many files.
- Give each kind a clear name in `catalog.properties` (`CONIFER`, `ROCK`, `BUSH`). Kinds are uppercase, as in `PropKind`.

## Where assets belong

| Content | Folder |
|---|---|
| Trees | `assets\models\environment\trees\<pack>\` |
| Rocks | `assets\models\environment\rocks\<pack>\` |
| Bushes and ferns | `assets\models\environment\plants\<pack>\` |
| Buildings | `assets\models\structures\` |
| Player and creature models, rigs | `assets\models\characters\` |
| Textures shared across models | with the model that uses them, unless shared by several packs |

## How the code loads assets

- The `assets` folder is registered once, in `GameApp.simpleInitApp`, with a `FileLocator`. Paths in code are relative to that folder, with forward slashes.
- `ModelCatalog` reads `catalog.properties` and loads each listed model. It catches a failed load, logs it, and keeps going.
- Gameplay and rendering ask `PropCatalog` for a `PropModel` by `PropKind`. They never name a file.

Example, adapted from `ModelCatalog`:

```java
Spatial model = assets.loadModel("models/environment/trees/quaternius_megakit/Pine_1.gltf");
PropModel prop = new PropModel(model, scale);
```

## Gameplay must not depend on file names

Gameplay refers to kinds (`PropKind.CONIFER`, `PropKind.ROCK`), never to a file. Changing a model should mean editing `catalog.properties`, not code. If a gameplay rule depends on a particular model (for example, that a tree can be cut down), add a kind for it rather than checking a file name.

## Troubleshooting missing textures and materials

| Symptom | Likely cause | Fix |
|---|---|---|
| Pink or missing texture | The texture file is not beside the `.gltf`, or its name in the `.gltf` is wrong | Copy the texture into the same folder; check the `uri` in the `.gltf` |
| Model is black | No light reaches it, or it has no normals | Check the lights in the scene; check the model in the showcase |
| Model is invisible | Scale is wrong (very small or huge), or it is outside the camera range | Check the scale in the showcase; check the camera's far distance |
| Model is missing from the world | Its path in `catalog.properties` is wrong, or the file is missing | Check the log for `Could not load` |
| Model looks the wrong colour | Its material or tint is wrong | Check the `KIND.tint` line in `catalog.properties` |
| `Cannot find loader` for a format | The format is not used by the game; the jME loader for it is missing | Ignore Ogre and OGG lines. For glTF or FBX, check `jme3-plugins` is in `build.gradle` |

## Checklist for a new model

- [ ] Licence checked on the asset's own page; file kept with its licence
- [ ] Recorded in `ASSET_MANIFEST.md`
- [ ] In the right folder, original name kept
- [ ] Textures beside the file, or embedded
- [ ] Added to `catalog.properties` under the right kind
- [ ] Checked in the showcase for scale, orientation and look
- [ ] `gradlew.bat build` still passes
