package com.coldfriend.interaction;

import com.coldfriend.entity.EntityId;
import java.util.Locale;

/**
 * The stable identity of something a player can use. It never comes from a scene node or a model file, so it
 * stays the same when the world is drawn again, and a future server can check it. Equality is by kind and
 * number (a record), so two refs to the same thing are equal wherever they were made.
 *
 * <p>Fixed parts of the world, such as the cabin door, have constant refs. Items use their {@link EntityId}, so
 * there is one identity system for things in the world, not two.
 *
 * @param kind what sort of thing it is
 * @param number which one of that kind: the entity id for items, a fixed number for fixed parts
 */
public record InteractableRef(InteractableKind kind, long number) {

    /** The cabin's front door. */
    public static final InteractableRef CABIN_DOOR = new InteractableRef(InteractableKind.DOOR, 1L);
    /** The car beside the cabin. */
    public static final InteractableRef VEHICLE = new InteractableRef(InteractableKind.VEHICLE, 1L);
    /** The cabin's stove. */
    public static final InteractableRef CABIN_HEARTH = new InteractableRef(InteractableKind.HEARTH, 1L);

    /** Checks that the kind is given. */
    public InteractableRef {
        if (kind == null) {
            throw new IllegalArgumentException("An interactable ref needs a kind");
        }
    }

    /**
     * @param id a world item's entity id
     * @return the ref of that item
     */
    public static InteractableRef item(EntityId id) {
        return new InteractableRef(InteractableKind.ITEM, id.value());
    }

    /**
     * @return the entity id of the item this ref names
     * @throws IllegalStateException if this ref is not an item
     */
    public EntityId entity() {
        if (kind != InteractableKind.ITEM) {
            throw new IllegalStateException(this + " is not an item");
        }
        return new EntityId(number);
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase(Locale.ROOT) + ":" + number;
    }
}
