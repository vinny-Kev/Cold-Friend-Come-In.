package com.coldfriend.player;

/**
 * What a player asked for during one frame. The input device is turned into this by the presentation layer, so
 * the simulation never sees keys or the mouse.
 *
 * @param forward -1 (back) to 1 (forward)
 * @param strafe -1 (left) to 1 (right)
 * @param turn how far to turn this frame, in radians; positive turns toward the player's right
 * @param look how far to look up this frame, in radians; negative looks down
 * @param sprint whether the player wants to run
 */
public record PlayerInput(float forward, float strafe, float turn, float look, boolean sprint) {

    /** No movement and no look change. */
    public static final PlayerInput NONE = new PlayerInput(0f, 0f, 0f, 0f, false);
}
