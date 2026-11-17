package com.coldfriend.world.render;

import com.coldfriend.assets.PropCatalog;
import com.coldfriend.assets.PropModel;
import com.coldfriend.entity.EntityId;
import com.coldfriend.item.ItemCatalog;
import com.coldfriend.world.items.WorldItem;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws the items lying or placed in the world. It owns no game state: every frame it is shown the simulation's
 * list of world items and adds, moves or removes models to match.
 */
public final class WorldItemsView {

    private final Node root = new Node("world-items");
    private final Map<EntityId, Spatial> shown = new HashMap<>();
    private final Map<EntityId, WorldItem> drawn = new HashMap<>();
    private final PropCatalog catalog;
    private final ItemCatalog items;

    /**
     * @param catalog gives a model for each item model key
     * @param items the item definitions
     */
    public WorldItemsView(PropCatalog catalog, ItemCatalog items) {
        this.catalog = catalog;
        this.items = items;
    }

    /** @return the node to attach to the scene */
    public Node root() {
        return root;
    }

    /**
     * Makes what is drawn match the simulation.
     *
     * @param current every world item right now
     */
    public void sync(List<WorldItem> current) {
        Set<EntityId> present = new HashSet<>();
        for (WorldItem item : current) {
            present.add(item.id());
            WorldItem before = drawn.get(item.id());
            if (before == null || before.x() != item.x() || before.y() != item.y() || before.z() != item.z()
                    || before.yaw() != item.yaw()) {
                Spatial old = shown.remove(item.id());
                if (old != null) {
                    old.removeFromParent();
                }
                add(item);
            }
            drawn.put(item.id(), item);
        }
        Iterator<Map.Entry<EntityId, Spatial>> it = shown.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<EntityId, Spatial> entry = it.next();
            if (!present.contains(entry.getKey())) {
                entry.getValue().removeFromParent();
                drawn.remove(entry.getKey());
                it.remove();
            }
        }
    }

    private void add(WorldItem item) {
        PropModel model = catalog.itemModel(items.get(item.stack().item()).modelKey());
        Spatial spatial = model.template().clone(false);
        spatial.setLocalScale(model.baseScale());
        spatial.setLocalRotation(new Quaternion().fromAngleAxis(item.yaw(), Vector3f.UNIT_Y));
        spatial.setLocalTranslation(item.x(), item.y(), item.z());
        root.attachChild(spatial);
        shown.put(item.id(), spatial);
    }
}
