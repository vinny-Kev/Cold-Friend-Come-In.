package com.coldfriend.interaction;

/**
 * Something a player can use, and where. Its identity is {@link #ref()}, which does not depend on how it is
 * drawn.
 *
 * @param ref stable identity
 * @param x world x of the spot to use it from, in metres
 * @param z world z of the spot to use it from, in metres
 * @param reach how close a player must be, in metres
 */
public record Interactable(InteractableRef ref, float x, float z, float reach) {

    /** @return what sort of thing it is */
    public InteractableKind kind() {
        return ref.kind();
    }
}
