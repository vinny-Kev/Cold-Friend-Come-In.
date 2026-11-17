package com.coldfriend.world.items;

/** Why an item cannot go where the player wants it. The message is shown to the player. */
public enum PlacementProblem {
    /** The item cannot be placed or dropped at all. */
    NOT_ALLOWED("This item cannot be put down."),
    /** The spot is outside the world. */
    OUTSIDE_WORLD("That is outside the world."),
    /** The spot is farther than the player can reach. */
    TOO_FAR("Too far away."),
    /** A wall, tree, rock or other obstacle is in the way. */
    BLOCKED("Something solid is in the way."),
    /** The ground is too steep. */
    TOO_STEEP("The ground is too steep."),
    /** The spot is in water. */
    IN_WATER("It would fall in the water."),
    /** The player is standing there. */
    ON_PLAYER("You are standing there."),
    /** Another object is already there. */
    OVERLAPS("Another object is already there."),
    /** The player cannot reach the spot because something is in between. */
    UNREACHABLE("You cannot reach there.");

    private final String message;

    PlacementProblem(String message) {
        this.message = message;
    }

    /** @return the text to show the player */
    public String message() {
        return message;
    }
}
