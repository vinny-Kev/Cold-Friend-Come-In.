# Item System

How items are defined, where their data lives, and how to add one. For what happens to items once they exist, see [INVENTORY_SYSTEM.md](INVENTORY_SYSTEM.md) and [WORLD_PLACEMENT.md](WORLD_PLACEMENT.md).

## The idea

An item has two separate parts:

| Part | Class | What it is |
|---|---|---|
| **Definition** | `ItemDefinition` | What a kind of item *is*: name, description, category, stack limit, weight, which model draws it, what the player may do with it. One per kind, shared by everyone. |
| **Instance** | `ItemStack` (and `WorldItem` in the world) | A real quantity: "3 firewood". It refers to its definition by `ItemId`, never by copying it. |

Because instances only carry an `ItemId`, the same definition serves the player inventory, items lying on the ground, placed objects, and later villager and merchant inventories, trading and crafting. Nothing in the item code depends on a model file name.

All of this is in the `com.coldfriend.item` package. It has no engine types.

## Classes

| Class | Purpose |
|---|---|
| `ItemId` | Stable identifier such as `water_container`. Lower snake case, checked when created. |
| `ItemCategory` | `MATERIAL`, `FOOD`, `CONTAINER`, `SUPPLY`, `TOOL`. For grouping and display only. |
| `ItemDefinition` | A record of the properties below. Rejects nonsense (no name, stack size below 1, negative weight). |
| `ItemStack` | `ItemId` plus a quantity of at least 1. |
| `ItemCatalog` | All definitions by id, plus the starting kit. `load(path)` reads the data file; `find` returns an `Optional`; `get` throws `IllegalArgumentException` naming a missing item. |

## Definition properties

| Property | Meaning |
|---|---|
| `id` | Stable identifier. Never rename one that has been used. |
| `name`, `description` | Shown to the player. |
| `category` | See above. |
| `maxStack` | Most that fit in one inventory slot. `1` means the item does not stack. |
| `weight` | Kilograms per item. The inventory sums it and shows it; carrying is not limited by weight yet. |
| `model` | A **model key**, not a file name. The asset catalogue turns it into a model (see below). |
| `icon` | An icon key. No icons exist yet, so the inventory draws a coloured square with the item's first letter. |
| `pickUp` | Whether an item of this kind lying in the world can be picked up. |
| `drop` | Whether the player may drop it. |
| `place` | Whether the player may place it deliberately. |
| `placeRadius` | Metres a placed item blocks movement and other placements. `0` means it can be walked through. |
| `useVerb` | What using it is called ("Eat", "Drink", "Fill"), shown as `[F] Eat` for the selected item. What using does is decided by item id in `world/SurvivalActions` (ration, water_container, water_container_empty); other items with a verb cannot be used yet. |

## Where the data lives

`config/items.properties`. One block per item. The `items` list decides which items exist and the order the catalogue keeps them in. `startingKit` lists what a new player starts with.

```
items=firewood, stone
startingKit=firewood:4, stone:5

item.firewood.name=Firewood
item.firewood.description=Split logs for the stove.
item.firewood.category=MATERIAL
item.firewood.maxStack=10
item.firewood.weight=1.5
item.firewood.model=firewood
item.firewood.icon=firewood
item.firewood.place=true
item.firewood.placeRadius=0.3
```

Defaults when a key is absent: `pickUp=true`, `drop=true`, `place=false`, `weight=0`, `placeRadius=0`. `name`, `category` and `maxStack` are required. A bad value fails at start-up with the item and key named, for example `Invalid item 'rock': missing item.rock.name`.

## Current catalogue

| Id | Category | Stack | Placeable | Model |
|---|---|---|---|---|
| `firewood` | Material | 10 | yes | placeholder |
| `stone` | Material | 20 | yes | Pebble (Quaternius Stylized Nature MegaKit) |
| `branch` | Material | 20 | yes | placeholder |
| `ration` | Food | 5 | yes | placeholder |
| `water_container` | Container | 1 | yes | placeholder |
| `bandage` | Supply | 10 | no | placeholder |
| `matches` | Supply | 1 | yes | placeholder |

## Missing assets

Only `stone` has a model. The rest are drawn as a bright **magenta cube**, on purpose, so a missing model cannot be mistaken for a finished one. Models needed: firewood, branch, ration, water container, bandage, matches. No models were invented or downloaded for this milestone. No inventory icons exist either.

## How to add a new item

1. Add its id to `items=` in `config/items.properties`.
2. Add an `item.<id>.*` block (copy one from the file).
3. Optionally add it to `startingKit`.
4. Run the tests. `ItemCatalogTest.theShippedItemFileLoadsAndEveryItemIsUsable` checks every item has a name and a model key.

No code changes are needed.

## How to add or change an item model

1. Put the glTF/GLB (and its textures) under `assets/models/` and record it in `ASSET_MANIFEST.md` with its licence.
2. In `assets/models/catalog.properties` add a line `ITEM.<model key> = path/to/model.gltf:scale`. The key is the item's `model` value. The optional scale sizes it (an item should be roughly its real size in metres).
3. Tints (`KIND.tint`) apply to scenery kinds only; item models are drawn untinted.

Item models are loaded without instancing, because items are few and individually placed. The loader replaces each model's material with a plain lit one that keeps its texture, as for scenery. Check the model in game with `dev.scenario=drop`.

## How to change a stack limit

Change `item.<id>.maxStack`. Existing stacks in a running game are not split; the limit applies to adding and merging.

## Rules the code relies on

- Gameplay code uses `ItemId` and `ItemCatalog`, never model names.
- An `ItemStack` quantity is never below 1; code that would make it 0 removes the stack instead.
- `ItemCatalog` is read-only once built, so it can be shared freely.

## Tests

`ItemCatalogTest` covers id validity, stack limits, lookup (found and missing), duplicate ids, parsing with defaults, clear errors for invalid data, and the shipped data file. `TestItems` builds small catalogues so other tests do not depend on the game data.
