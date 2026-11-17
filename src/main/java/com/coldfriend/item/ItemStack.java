package com.coldfriend.item;

/**
 * A quantity of one kind of item: three firewood, one water container. This is the unit that inventories hold and
 * that lies in the world. It refers to its kind by {@link ItemId}, so it can be stored, sent and compared without
 * the definition.
 *
 * @param item the kind of item
 * @param quantity how many; at least 1
 */
public record ItemStack(ItemId item, int quantity) {

    /** Rejects missing items and quantities below one. */
    public ItemStack {
        if (item == null) {
            throw new IllegalArgumentException("A stack needs an item");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("A stack needs a quantity of at least 1 but was " + quantity);
        }
    }

    /** @return a stack of the same item with a different quantity */
    public ItemStack withQuantity(int newQuantity) {
        return new ItemStack(item, newQuantity);
    }
}
