package com.coldfriend.assets;

/**
 * Kinds of scenery the world places. Gameplay and generation refer to these kinds only; which model file draws
 * a kind is decided by the asset catalogue, so models can be swapped without touching gameplay code.
 */
public enum PropKind {
    /** Evergreen conifer tree. */
    CONIFER(1.6f, 0.35f),
    /** Leafless living tree. */
    BARE_TREE(1.4f, 0.3f),
    /** Dead tree. */
    DEAD_TREE(1.2f, 0.3f),
    /** Fallen log lying on the ground. */
    FALLEN_LOG(1.2f, 0f),
    /** Boulder or rock outcrop. */
    ROCK(1.0f, 0.8f),
    /** Low bush. */
    BUSH(0.7f, 0f),
    /** Small ground clutter such as stones and branches. */
    CLUTTER(0.3f, 0f);

    private final float spacingRadius;
    private final float colliderRadius;

    PropKind(float spacingRadius, float colliderRadius) {
        this.spacingRadius = spacingRadius;
        this.colliderRadius = colliderRadius;
    }

    /** @return how much room the prop keeps free around itself at scale 1, in metres */
    public float spacingRadius() {
        return spacingRadius;
    }

    /** @return radius of the solid part at scale 1, in metres; 0 if the player can walk through it */
    public float colliderRadius() {
        return colliderRadius;
    }

    /** @return true if the player cannot walk through this kind of prop */
    public boolean blocksMovement() {
        return colliderRadius > 0f;
    }
}
