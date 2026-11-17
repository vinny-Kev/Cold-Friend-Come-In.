package com.coldfriend.interaction;

/**
 * Something a player can use. It has a stable id that does not depend on how it is drawn.
 *
 * @param id stable identifier
 * @param kind what sort of thing it is
 * @param x world x of the spot to use it from, in metres
 * @param z world z of the spot to use it from, in metres
 * @param reach how close a player must be, in metres
 */
public record Interactable(String id, InteractableKind kind, float x, float z, float reach) {
}
