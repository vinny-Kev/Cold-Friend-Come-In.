# Inventory System

The player's inventory: its rules, the screen that shows it, and how drops and pick-ups move items between it and the world. Items themselves are described in [ITEM_SYSTEM.md](ITEM_SYSTEM.md).

## Where things live

| Part | Class | Package |
|---|---|---|
| The inventory and its rules | `Inventory` | `inventory` (no engine or screen types) |
| Who owns which inventory | `WorldSimulation` (a map from `PlayerId`) | `world` |
| Drop and pick-up logic | `ItemActions` | `world.items` |
| The screen | `InventoryState` (input, lifecycle) and `InventoryView` (drawing) | `state`, `ui` |

Quantities live only in `Inventory`, inside the simulation. The screen holds a cursor and a picked-up slot and nothing else.

## Inventory rules

`Inventory` has a fixed number of slots (`PLAYER_SLOTS = 24`). Each slot is empty or holds one `ItemStack`.

| Operation | Behaviour |
|---|---|
| `add(stack)` | Fills partly full stacks of the same item first, then empty slots. Returns how much **did not fit** (0 if it all went in). |
| `canFit(stack)` | Asks whether it would all fit, without changing anything. |
| `remove(item, quantity)` | All or nothing. Takes from the last slots first. Returns false and changes nothing if there is not enough. |
| `take(slot, quantity)` | Takes some or all of one slot. Empty result if the slot is empty or holds fewer. |
| `count(item)`, `has(item, quantity)` | How many are held; whether at least that many are. |
| `move(from, to)` | Empty target: moves. Same item: merges up to the stack limit. Different item: swaps. Returns `MOVED`, `MERGED`, `SWAPPED` or `UNCHANGED`. |
| `select(slot)`, `selectedStack()` | The selected slot, used by the hotbar keys and placement. |
| `totalWeight()`, `emptySlots()` | For display. Weight does not limit carrying yet. |

Invalid input is rejected, not clamped: a quantity below 1 throws `IllegalArgumentException`, a bad slot throws `IndexOutOfBoundsException`, an item missing from the catalogue throws `IllegalArgumentException`.

**Capacity** means slots only. A full inventory refuses further pick-ups; partial stacks still take what fits.

## The inventory screen

Open with **I** or **Tab** while playing. While it is open the game stands still (`WorldPlayState.setPaused(true)`), gameplay input is released and the cursor is free, so navigating cannot move or turn the player.

| Input | Action |
|---|---|
| Arrow keys, W/A/S/D, or the mouse | Move the cursor and select a slot |
| Enter, Space, or left click | Pick up the stack in the slot; do it again on another slot to put it down (move, merge or swap) |
| Q | Drop one of the selected stack |
| X | Drop the whole stack |
| P | Place the selected item in the world (closes the inventory and enters placement mode) |
| I, Tab, or Esc | Close |

The screen shows 24 slots, an icon placeholder (a square coloured by category with the item's first letter), the name, the quantity, an empty-slot state, a details panel for the selected slot (name, category, quantity, weight, description, whether it can be placed), total weight, free slots, and a line of feedback.

It uses the existing menu architecture: `InputController` (`bind`/`release`) delivers `GameAction`s (`MENU_*` plus `INVENTORY_TOGGLE`, `INVENTORY_DROP`, `INVENTORY_DROP_ALL`, `INVENTORY_PLACE`). `GameApp.openInventory()` attaches the state and pauses the world; `closeInventory()` detaches it and resumes.

## Hotbar-style shortcuts while playing

Number keys **1 to 9** select slots 1 to 9. The selected item is shown bottom left. **Q** drops one (hold Shift to drop the stack), **P** starts placing it.

## Dropping

`WorldSimulation.dropItem(player, slot, quantity)` calls `ItemActions.drop`:

1. The slot must hold at least `quantity`, and the item's `drop` must be true.
2. It tries spots in front of the player (0.9, 1.3 and 0.55 m ahead, at several angles) and takes the first `PlacementValidator.checkDrop` accepts, so an item is never dropped inside a wall, a tree, deep water or the world edge.
3. Only then does it remove the quantity from the slot and spawn a `WorldItem` with the next stable id.
4. If no spot works it fails and nothing changes.

Partial stacks are supported: dropping 2 of 4 leaves 2.

## Picking up

The player looks at an item and presses **E**. `WorldSimulation.interactableFor` returns the world item whose position is nearest the line of sight (within 0.45 m of it and 3.2 m away); otherwise the cabin door if the player is next to it. E sends an `InteractionRequest`; `interact` dispatches by kind, so there is one interaction system, not two.

`ItemActions.pickUp` checks that the item exists, `pickUp` is true, it is within 3.2 m, and nothing solid is between (you cannot take things through a wall). It then adds to the inventory:

- everything fits: the world item is removed;
- some fits: the world item stays with the same id and the remaining quantity ("Picked up 2 Firewood. Your inventory is full; 3 left.");
- nothing fits: it stays untouched ("Your inventory is full.").

The world item leaves the world only when the transfer succeeds, so there is no duplication or loss. `WorldItemTest.dropAndPickUpNeverCreateOrLoseItems` runs 200 random drops and pick-ups and checks the totals never change.

## How to add a new inventory interaction

1. Put the **rule** in `Inventory` or `ItemActions` and have `WorldSimulation` expose it as a method taking a `PlayerId`. Return an `ActionResult` with a short message.
2. Test it in `InventoryTest` or `WorldItemTest` with no engine involved.
3. For a screen action, add a `GameAction` (and its key in `InputController.triggersFor`), handle it in `InventoryState.onAction`, and call the simulation method. Do not change quantities in the screen.
4. For a shortcut during play, add a mapping and a `consume...` method to `GameplayInput` and handle it in `WorldPlayState.handleGameplayKeys`.

## Starting state

A new player's inventory is filled from `startingKit` in `config/items.properties`. `StartingItems` also puts firewood by the stove, a water container on the table and two branches by the door as world items, so there is something to pick up. These are simulation state, not generated world data, so world generation and seeds are unchanged.

## Tests

`InventoryTest` (16 tests): adding, stack fill order, maximum stacks, full inventory with remainder, `canFit`, removing across slots, `has`, invalid quantities, unknown items, partial take, move/merge/swap, selection, weight. `WorldItemTest` (11 tests): drop quantities, ids, position near the player and on the ground, whole-stack drop, bad drops, never inside a wall, pick-up, out of reach, full inventory, partial pick-up with the same id, not through walls, no duplication or loss.
