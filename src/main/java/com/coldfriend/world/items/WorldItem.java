package com.coldfriend.world.items;

import com.coldfriend.entity.EntityId;
import com.coldfriend.item.ItemStack;

/**
 * Items that exist in the world rather than in an inventory: something dropped on the ground, or an object the
 * player deliberately placed. It has a stable id, says what and how many it is, and where it rests.
 *
 * @param id stable identity, kept for as long as it exists
 * @param stack what it is and how many; a placed object is always one
 * @param x world x in metres
 * @param y height of its base in metres
 * @param z world z in metres
 * @param yaw rotation about the vertical axis in radians
 * @param placed true if the player placed it deliberately; false if it was dropped or lies there from the start
 */
public record WorldItem(EntityId id, ItemStack stack, float x, float y, float z, float yaw, boolean placed) {

    /** @return the same world item holding a different stack */
    public WorldItem withStack(ItemStack newStack) {
        return new WorldItem(id, newStack, x, y, z, yaw, placed);
    }
}
