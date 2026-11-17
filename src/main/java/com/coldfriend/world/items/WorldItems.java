package com.coldfriend.world.items;

import com.coldfriend.entity.EntityId;
import com.coldfriend.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every item lying or placed in the world, by id. Ids are handed out in order, so the same sequence of actions
 * always gives the same ids. Owned by the simulation; screens only ever see snapshots.
 */
public final class WorldItems {

    private final Map<EntityId, WorldItem> items = new LinkedHashMap<>();
    private long nextId = 1L;

    /** Creates a world item with a new id. */
    public WorldItem spawn(ItemStack stack, float x, float y, float z, float yaw, boolean placed) {
        WorldItem item = new WorldItem(new EntityId(nextId++), stack, x, y, z, yaw, placed);
        items.put(item.id(), item);
        return item;
    }

    /** @return the item with this id, if it still exists */
    public Optional<WorldItem> get(EntityId id) {
        return Optional.ofNullable(items.get(id));
    }

    /** Replaces an existing world item with a changed copy, keeping its id. */
    public void replace(WorldItem item) {
        if (!items.containsKey(item.id())) {
            throw new IllegalArgumentException("No world item " + item.id());
        }
        items.put(item.id(), item);
    }

    /** @return true if the item existed and has been removed */
    public boolean remove(EntityId id) {
        return items.remove(id) != null;
    }

    /** @return a snapshot of all world items, oldest first */
    public List<WorldItem> all() {
        return new ArrayList<>(items.values());
    }

    /** @return how many world items exist */
    public int count() {
        return items.size();
    }
}
