package com.coldfriend.world.model;

/**
 * A named place with a stable id.
 *
 * @param id stable identifier, the same for the same seed and generation version
 * @param kind what the place is for
 * @param x world x in metres
 * @param z world z in metres
 */
public record WorldLocation(String id, LocationKind kind, float x, float z) {
}
