package com.coldfriend.world.items;

/**
 * The answer to "can this item go here?". When it can, {@code y} is the height it will rest at: the ground, the
 * cabin floor, or a table top.
 *
 * @param valid whether the spot is allowed
 * @param problem why not, or null if it is allowed
 * @param y resting height of the item's base in metres
 * @param onSurface true if it would rest on a raised surface such as a table rather than the ground or floor
 */
public record PlacementCheck(boolean valid, PlacementProblem problem, float y, boolean onSurface) {

    /** @return an allowed spot resting at a height */
    public static PlacementCheck allowed(float y, boolean onSurface) {
        return new PlacementCheck(true, null, y, onSurface);
    }

    /** @return a refused spot with the reason */
    public static PlacementCheck refused(PlacementProblem problem) {
        return new PlacementCheck(false, problem, 0f, false);
    }
}
