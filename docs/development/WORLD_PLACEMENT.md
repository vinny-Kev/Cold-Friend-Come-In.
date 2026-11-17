# World Placement

Putting items in the world on purpose: placement mode, the rules that decide where an item may go, what a placed object is, and how it is picked up again. Related: [ITEM_SYSTEM.md](ITEM_SYSTEM.md), [INVENTORY_SYSTEM.md](INVENTORY_SYSTEM.md).

## Three ways an item can be in the world

| | How it got there | Can be picked up | Blocks movement |
|---|---|---|---|
| **Dropped** | Dropped from the inventory, or lying there from the start | if the definition allows (`pickUp`) | no |
| **Placed** | Deliberately placed (`WorldItem.placed = true`) | yes, again | yes if `placeRadius > 0` and it is on the ground or floor |
| **Not allowed** | The definition has `drop=false` or `place=false` | n/a | n/a |

Dropped and placed objects are both `WorldItem` records (stable `EntityId`, an `ItemStack`, position, rotation, a `placed` flag) held by `WorldItems` inside the simulation. Items on a table do not register collision; they rest on the table top.

## Who decides what

| Part | Responsibility | Authoritative? |
|---|---|---|
| `PlacementValidator` | Decides whether a spot is allowed and what height it rests at | yes |
| `ItemActions.place` | Validates, takes one item from the slot, creates the world object, registers collision | yes |
| `WorldSimulation.checkPlacement` | Read-only question used for the preview | advice only |
| `WorldPlayState` | Placement mode: aims, previews, rotates, sends the confirmed spot | no |
| `PlacementPreview` | Draws the ghost model and ring | no |

The preview is presentation. When the player confirms, the simulation checks the spot **again** and places only if it passes, so the preview can never put something where the rules forbid.

## Placement mode (what the player does)

1. In the inventory press **P**, or while playing press **P** with a placeable item selected.
2. The item's model appears where the line of sight meets the ground, floor or table top (within about 6.5 m), on a ring: **green** if the simulation would allow it, **red** if not. The reason is shown ("Too far away.", "Something solid is in the way.").
3. Turn the object with **R** (15 degrees) or the mouse wheel. Walking and looking work as normal.
4. **Left click** or **Enter** places it. While the stack has items left, placement mode stays on so several can be placed. **Right click** or **Esc** cancels; cancelling consumes nothing.

## Validation rules

`PlacementValidator` holds two ordered lists of `PlacementRule`s. The first rule that fails gives the reason shown to the player.

**Placing** (`placeRules`):

| Rule | Refuses with | When |
|---|---|---|
| `insideWorld` | `OUTSIDE_WORLD` | within 1 m of or beyond the world edge |
| `withinReach` | `TOO_FAR` | more than `REACH` (4.5 m) from the player |
| `reachable` | `UNREACHABLE` | a wall, closed door or other solid lies between the player and the spot (so you cannot place through a wall) |
| `terrainUsable` | `IN_WATER`, `TOO_STEEP` | on the ground: water deeper than 5 cm, or slope above the walking limit. A table top is always level. |
| `notOverlappingObjects` | `OVERLAPS` | another world item is already there |
| `notBlocked` | `BLOCKED` | on the ground: a wall, building, furniture, tree, rock or placed object overlaps. On a table: the item would hang over the edge. |
| `notOnPlayer` | `ON_PLAYER` | the player is standing there |

An item with `place=false` is refused with `NOT_ALLOWED` before any rule runs.

**Dropping** (`dropRules`) uses a subset: inside the world, reachable, terrain usable, not on a placed object, not blocked. It does not need the player to aim.

**Resting height.** `restHeight(x, z)` gives the table top if the spot is over the table, the floor top if inside the cabin, otherwise the terrain. Items are never placed below the terrain.

## What a placement does

`ItemActions.place` (all in the simulation):

1. Slot must hold an item; `checkPlace` must pass, else it returns the reason and **changes nothing**.
2. `inventory.take(slot, 1)` removes exactly one.
3. `WorldItems.spawn(...)` creates a `WorldItem` with `placed = true`, the checked resting height, and the next stable id.
4. If `placeRadius > 0` and it is on the ground or floor, `WorldCollision.addObstacle` makes it solid.

## Picking a placed object up again

The same interaction as any item: look at it, press **E**. `ItemActions.pickUp` returns it to the inventory (subject to stack limits and space), removes the `WorldItem` and calls `WorldCollision.removeObstacle`, so it stops blocking. Because the stack goes back by item id, nothing is duplicated.

## Cabin and village

- **Cabin:** walls, furniture and the shut door are solids, so nothing can be placed in or through them. The table (`CabinLayout.supportSurfaces`) is the one raised support surface. The cabin floor is its own rest height.
- **Village:** buildings are solid footprints, so nothing can be placed inside them. Their windows use the same transparent glass as the cabin (see below).
- Starting items (firewood by the stove, a water container on the table) show placed-looking items correctly positioned in the cabin.

## How to add a new placement rule

1. Add a method to `PlacementValidator` that takes a `PlacementRule.Spot` and returns `Optional<PlacementProblem>`.
2. If it needs a new reason, add a value to `PlacementProblem` with a player-facing message.
3. Add `this::yourRule` to `placeRules` (and `dropRules` if drops should obey it) **in the order you want reasons reported**. Cheap, general rules go first; the most specific reason should win.
4. Add a test to `ObjectPlacementTest` that refuses the case and checks the inventory is unchanged.

## How to add a new placeable object type

1. Define it as an item in `config/items.properties` with `place=true` and a `placeRadius` (metres it blocks; 0 to be walk-through).
2. Give it a model key and model (see ITEM_SYSTEM.md). Until then it is a magenta cube.
3. That is all: placement, preview, collision, pick-up and the inventory work from the definition. Behaviour (a stove that burns wood) is a later system and is not built.
4. To let objects rest on a new surface, add it to `CabinLayout.supportSurfaces` (or a new source of `Surface`s) and make `PlacementValidator.restHeight` and `supportAt` use it.

## Transparent windows (rendered, not simulated)

Windows are real openings. `RenderParts.wallWithOpenings` builds a wall as solid pieces around each opening, an opaque frame, and two glass sheets facing opposite ways, so glass shows from both sides. The glass material (`RenderParts.glass`) is lit, alpha blended, in the transparent bucket, with **depth writing off** so it can never hide what is behind it, whatever the draw order. The opaque walls and frames render in the normal bucket. See the root cause in `docs/DECISIONS.md` (D-016).

## Limits

- Objects are placed on the ground, the cabin floor or the table only; no stacking on other objects, shelves or walls.
- Rotation is about the vertical axis only.
- Placed objects have no behaviour and no saved state beyond item id, position and rotation.
- Dropped items do not fall or roll; they appear where there is room.

## Tests

`ObjectPlacementTest` (16 tests): a valid placement consumes one and creates a stable object; placed objects block movement; refusals for walls, the world edge, too far, on the player, on another object, in water, through a closed wall, non-placeable items; preview never consumes; resting on the table and refusing overhang; pick-up of a placed object restores the item and removes the collision; no loss or duplication; the shipped data gives a starting kit and in-cabin starting items.
