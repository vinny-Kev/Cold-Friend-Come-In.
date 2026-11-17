package com.coldfriend.item;

/**
 * What a kind of item is. Definitions are static data shared by the player inventory, items lying in the world,
 * placed objects, and later villager and merchant inventories, trading and crafting. A definition says nothing
 * about how many of the item exist or where they are; that is an {@link ItemStack} or a world item.
 *
 * @param id stable identifier
 * @param name name shown to the player
 * @param description short text shown with the item
 * @param category the broad kind
 * @param maxStack most of this item that fit in one inventory slot; 1 means it does not stack
 * @param weight weight of one item in kilograms; not enforced yet
 * @param modelKey key the asset catalogue turns into a model; never a file name
 * @param iconKey key for the inventory icon; a plain placeholder is drawn when there is none
 * @param canPickUp whether an item of this kind lying in the world can be picked up
 * @param canDrop whether the player may drop it freely
 * @param canPlace whether the player may deliberately place it
 * @param placeRadius radius in metres that a placed item blocks movement and other placements; 0 means it can be
 *        walked through but still keeps other placed items from overlapping a small spot
 * @param useVerb what using the item will be called, such as "Eat"; empty if it has no use yet
 */
public record ItemDefinition(
        ItemId id,
        String name,
        String description,
        ItemCategory category,
        int maxStack,
        float weight,
        String modelKey,
        String iconKey,
        boolean canPickUp,
        boolean canDrop,
        boolean canPlace,
        float placeRadius,
        String useVerb) {

    /** Checks that the values make sense. */
    public ItemDefinition {
        if (id == null || category == null) {
            throw new IllegalArgumentException("An item needs an id and a category");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item " + id + " needs a name");
        }
        if (maxStack < 1) {
            throw new IllegalArgumentException("Item " + id + " must have a stack size of at least 1");
        }
        if (weight < 0f || placeRadius < 0f) {
            throw new IllegalArgumentException("Item " + id + " cannot have a negative weight or radius");
        }
        description = description == null ? "" : description;
        modelKey = modelKey == null ? "" : modelKey;
        iconKey = iconKey == null ? "" : iconKey;
        useVerb = useVerb == null ? "" : useVerb;
    }

    /** @return true if more than one fits in a slot */
    public boolean stackable() {
        return maxStack > 1;
    }
}
